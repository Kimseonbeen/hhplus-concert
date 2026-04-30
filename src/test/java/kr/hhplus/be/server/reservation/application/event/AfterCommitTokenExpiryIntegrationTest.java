package kr.hhplus.be.server.reservation.application.event;

import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.repository.BalanceRepository;
import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.domain.repository.QueueTokenRepository;
import kr.hhplus.be.server.reservation.application.ReservationFacade;
import kr.hhplus.be.server.reservation.application.dto.PaymentCommand;
import kr.hhplus.be.server.reservation.domain.event.PaymentCompletedEvent;
import kr.hhplus.be.server.reservation.domain.model.Reservation;
import kr.hhplus.be.server.reservation.domain.model.ReservationStatus;
import kr.hhplus.be.server.reservation.domain.repository.ReservationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AfterCommitTokenExpiryIntegrationTest {

    @Autowired private ReservationFacade reservationFacade;
    @Autowired private QueueTokenRepository queueTokenRepository;
    @Autowired private BalanceRepository balanceRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;

    private static final String TEST_TOKEN = "test-active-token";
    private static final Long USER_ID = 1L;
    private Long reservationId;

    @BeforeEach
    void setUp() {
        balanceRepository.save(Balance.builder()
                .userId(USER_ID)
                .amount(100_000L)
                .build());

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .userId(USER_ID)
                .seatId(1L)
                .price(50_000L)
                .status(ReservationStatus.PENDING_PAYMENT)
                .expiredAt(LocalDateTime.now().plusMinutes(5))
                .build());
        reservationId = reservation.getId();

        queueTokenRepository.save(QueueToken.builder()
                .token(TEST_TOKEN)
                .status(QueueTokenStatus.ACTIVE)
                .build());
    }

    @AfterEach
    void cleanup() {
        reservationRepository.deleteAll();
        balanceRepository.deleteAll();
        queueTokenRepository.removeToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("결제 성공 시 DB 커밋 이후 AFTER_COMMIT에서 Redis 토큰이 삭제된다")
    void completePayment_AfterDbCommit_TokenIsExpiredInRedis() throws InterruptedException {
        // given
        assertTrue(queueTokenRepository.findByToken(TEST_TOKEN).isPresent(),
                "결제 전 토큰이 Redis에 존재해야 합니다");

        PaymentCommand command = PaymentCommand.builder()
                .reservationId(reservationId)
                .userId(USER_ID)
                .build();

        // when
        reservationFacade.completePayment(command, TEST_TOKEN);

        // then - @Async 리스너 실행 대기 후 확인
        Thread.sleep(1000);
        assertFalse(queueTokenRepository.findByToken(TEST_TOKEN).isPresent(),
                "DB 커밋 완료 후 AFTER_COMMIT에서 토큰이 삭제되어야 합니다");
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 AFTER_COMMIT 리스너가 실행되지 않아 토큰이 보존된다")
    void whenTransactionRollback_AfterCommitDoesNotFire_TokenIsPreserved() throws InterruptedException {
        // given
        assertTrue(queueTokenRepository.findByToken(TEST_TOKEN).isPresent(),
                "롤백 전 토큰이 Redis에 존재해야 합니다");

        // when - 트랜잭션 내에서 이벤트 발행 후 강제 롤백
        // AFTER_COMMIT은 트랜잭션이 커밋되어야 실행되므로, 롤백 시 리스너가 실행되지 않아야 함
        transactionTemplate.execute(status -> {
            eventPublisher.publishEvent(new PaymentCompletedEvent(1L, TEST_TOKEN, null));
            status.setRollbackOnly(); // 이벤트 발행 후 강제 롤백
            return null;
        });

        // then - AFTER_COMMIT 미실행으로 토큰이 보존되어야 함
        Thread.sleep(500);
        assertTrue(queueTokenRepository.findByToken(TEST_TOKEN).isPresent(),
                "트랜잭션 롤백 시 AFTER_COMMIT 리스너가 실행되지 않아 토큰이 보존되어야 합니다");
    }
}
