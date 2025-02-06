package kr.hhplus.be.server.reservation.application;

import kr.hhplus.be.server.concert.domain.exception.ConcertException;
import kr.hhplus.be.server.concert.domain.exception.ConcertErrorCode;
import kr.hhplus.be.server.concert.domain.model.Concert;
import kr.hhplus.be.server.concert.domain.model.Seat;
import kr.hhplus.be.server.concert.domain.model.SeatStatus;
import kr.hhplus.be.server.concert.domain.repository.ConcertRepository;
import kr.hhplus.be.server.concert.domain.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.concert.domain.repository.SeatRepository;
import kr.hhplus.be.server.reservation.application.dto.ReservationCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class ConcurrentReservationIntegrationTest {

    @Autowired
    private ReservationFacade reservationFacade;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ConcertScheduleRepository concertScheduleRepository;

    @Autowired
    private ConcertRepository concertRepository;

    Concert givenConcert;


    @BeforeEach
    void init() {
        Concert concert = Concert.builder()
                .name("아이유 콘서트")
                .build();

        givenConcert = concertRepository.save(concert);

        for (int i = 1; i <= 10; i++) {
            Seat seat = Seat.builder()
                    .concertScheduleId(givenConcert.getId())
                    .seatNum(i)
                    .status(SeatStatus.AVAILABLE)
                    .price(10_000L)
                    .build();

            seatRepository.save(seat);
        }
    }

    @AfterEach
    void cleanUp() {
        concertRepository.deleteAll();
        seatRepository.deleteAll();
    }

    @Test
    void 단일_좌석에_대한_100개의_동시_예약_요청시_1개만_성공한다() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Long seatId = 1L;
        Long scheduleId = 1L;
        Long userId = 1L;

        // 여러 스레드에서 동시에 같은 좌석 예약 시도
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    ReservationCommand command = new ReservationCommand(userId, scheduleId, seatId);
                    reservationFacade.reserve(command);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드가 완료될 때까지 대기
        latch.await();
        executorService.shutdown();

        // 검증
        assertAll(
                () -> assertEquals(1, successCount.get(), "한 번만 예약에 성공해야 함"),
                () -> assertEquals(numberOfThreads - 1, failCount.get(), "나머지는 모두 실패해야 함")
        );
    }

    @Test
    void 서로_다른_10개_좌석에_대해_10명의_동시_예약_요청시_모두_성공한다() throws InterruptedException {
        int numberOfSeats = 10;
        int numberOfUsers = 10;
        Long scheduleId = givenConcert.getId();

        List<Long> userIds = IntStream.rangeClosed(1, numberOfUsers)
                .mapToObj(Long::valueOf)
                .toList();

        List<Seat> seats = seatRepository.findAll();
        List<Long> seatIds = seats.stream()
                .map(Seat::getId)
                .toList();

        int numberOfThreads = numberOfSeats;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            final Long seatId = seatIds.get(i);
            final Long userId = userIds.get(i);

            executorService.submit(() -> {
                try {
                    ReservationCommand command = new ReservationCommand(userId, scheduleId, seatId);
                    reservationFacade.reserve(command);
                    successCount.incrementAndGet();
                } catch (ConcertException e) {
                    if (Objects.equals(e.getMessage(), ConcertErrorCode.SEAT_ALREADY_OCCUPIED.getMsg())) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertAll(
                () -> assertEquals(numberOfSeats, successCount.get(), "모든 좌석 예약에 성공해야 함"),
                () -> assertEquals(0, failCount.get(), "예약 실패가 없어야 함"),
                () -> {
                    for (Long seatId : seatIds) {
                        Seat reserved = seatRepository.findById(seatId).orElseThrow();
                        assertEquals(SeatStatus.RESERVED, reserved.getStatus(),
                                String.format("좌석 %d는 예약된 상태여야 함", seatId));
                    }
                }
        );
    }
}
