package kr.hhplus.be.server.common.outbox;

import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.repository.BalanceRepository;
import kr.hhplus.be.server.concert.domain.model.*;
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.reservation.application.ReservationFacade;
import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import kr.hhplus.be.server.reservation.application.dto.ReservationResult;
import kr.hhplus.be.server.reservation.infrastructure.client.DataPlatformClient;
import kr.hhplus.be.server.user.domain.model.User;
import kr.hhplus.be.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class OutboxIntegrationTest {

    @Autowired private ReservationFacade reservationFacade;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxEventScheduler outboxEventScheduler;
    @Autowired private BalanceRepository balanceRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ConcertScheduleRepository concertScheduleRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean
    private DataPlatformClient dataPlatformClient;

    private Long userId;
    private Long seatId;
    private String token = "test-token";

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();

        User user = userRepository.save(User.createUser());
        userId = user.getId();

        balanceRepository.save(Balance.builder()
                .userId(userId)
                .amount(100_000L)
                .build());

        ConcertSchedule schedule = concertScheduleRepository.save(
                ConcertSchedule.builder()
                        .concertId(1L)
                        .concertDate(LocalDateTime.now().plusDays(10))
                        .status(ConcertScheduleStatus.AVAILABLE)
                        .build()
        );

        Seat seat = seatRepository.save(Seat.builder()
                .concertScheduleId(schedule.getId())
                .seatNum(1)
                .price(50_000L)
                .status(SeatStatus.AVAILABLE)
                .version(0L)
                .build());

        seatId = seat.getId();
    }

    @Test
    @DisplayName("결제 성공 시 데이터 플랫폼 전송 성공하면 아웃박스가 PUBLISHED로 변경된다")
    void 결제_성공_전송_성공_PUBLISHED() throws InterruptedException {
        // given
        ReservationResult reservation = reservationFacade.reserve(
                ReservationCommand.builder().userId(userId).seatId(seatId).scheduleId(1L).build()
        );

        PaymentCommand command = PaymentCommand.builder()
                .reservationId(reservation.reservationId())
                .userId(userId)
                .build();

        // when
        reservationFacade.completePayment(command, token);

        // AFTER_COMMIT 비동기 처리 대기
        Thread.sleep(500);

        // then
        List<OutboxEvent> published = outboxEventRepository.findAllByStatus(OutboxStatus.PUBLISHED);
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getEventType()).isEqualTo(OutboxEventType.DATA_PLATFORM_SEND);
    }

    @Test
    @DisplayName("데이터 플랫폼 전송 실패 시 아웃박스가 PENDING 유지되고 폴러가 재처리한다")
    void 전송_실패_PENDING_유지_폴러_재처리() throws InterruptedException {
        // [given] 데이터 플랫폼 서버 장애 상황 시뮬레이션
        // sendReservationData()는 실제로 HTTP 요청을 보내는 메서드인데,
        // 테스트에서는 실제 서버가 없으므로 Mock으로 장애를 표현
        // → doThrow = 서버 장애, 호출 시 timeout 예외 발생
        doThrow(new RuntimeException("timeout")).when(dataPlatformClient).sendReservationData(any());

        // [given] 결제할 예약 생성 (사전 준비)
        ReservationResult reservation = reservationFacade.reserve(
                ReservationCommand.builder().userId(userId).seatId(seatId).scheduleId(1L).build()
        );

        PaymentCommand command = PaymentCommand.builder()
                .reservationId(reservation.reservationId())
                .userId(userId)
                .build();

        // [when] 결제 완료 호출
        // 내부에서: outbox 저장(PENDING) → 트랜잭션 커밋 → AFTER_COMMIT 비동기 리스너 실행
        //           → sendReservationData() 호출 → doThrow로 실패 → catch에서 log만 → PENDING 유지
        reservationFacade.completePayment(command, token);

        // AFTER_COMMIT 비동기 리스너가 완료될 때까지 대기
        Thread.sleep(500);

        // [then] 전송 실패했으니 outbox 상태가 PENDING 그대로여야 함
        List<OutboxEvent> pending = outboxEventRepository.findAllByStatus(OutboxStatus.PENDING);
        assertThat(pending).hasSize(1);

        // [when] 데이터 플랫폼 서버 복구 시뮬레이션
        // → doNothing() = 서버 복구, 이제 호출해도 예외 없이 정상 응답
        doNothing().when(dataPlatformClient).sendReservationData(any());

        // 스케줄러가 10초마다 자동 실행되지만, 테스트에서는 직접 호출해서 즉시 재처리
        // 내부에서: PENDING 이벤트 조회 → sendReservationData() 호출 → 성공 → PUBLISHED 처리
        outboxEventScheduler.processOutboxEvents();

        // [then] 폴러가 재처리 성공했으니 PUBLISHED로 변경되어야 함
        List<OutboxEvent> published = outboxEventRepository.findAllByStatus(OutboxStatus.PUBLISHED);
        assertThat(published).hasSize(1);
    }

}
