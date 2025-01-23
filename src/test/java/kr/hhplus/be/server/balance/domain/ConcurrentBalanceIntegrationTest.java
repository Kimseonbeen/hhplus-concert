package kr.hhplus.be.server.balance.domain;

import kr.hhplus.be.server.balance.domain.model.Balance;
import kr.hhplus.be.server.balance.domain.repository.BalanceRepository;
import kr.hhplus.be.server.balance.domain.service.BalanceService;
import kr.hhplus.be.server.user.domain.model.User;
import kr.hhplus.be.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class ConcurrentBalanceIntegrationTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @BeforeEach
    void init() {
        User user = User.builder()
                .name("TEST")
                .build();
        userRepository.save(user);

        Balance balance = Balance.builder()
                .userId(1L)
                .amount(10_000L)
                .build();
        balanceRepository.save(balance);
    }

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
        balanceRepository.deleteAll();
    }

    @Test
    void 포인트_차감_연속_두번_요청시_두번째는_실패한다() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        AtomicReference<String> failMessage = new AtomicReference<>();

        // when
        for (int i = 0; i < 2; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    balanceService.decrease(userId, amount);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failMessage.set(e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Balance finalBalance = balanceService.getBalance(userId);
        assertEquals(1, successCount.get(), "성공 횟수는 1이어야 합니다");
        assertEquals(1, failCount.get(), "실패 횟수는 1이어야 합니다");
        assertEquals(9000L, finalBalance.getAmount(), "잔액이 1000원만 차감되어야 합니다");
        assertEquals("연속 요청은 처리할 수 없습니다", failMessage.get(), "연속 요청은 처리할 수 없습니다");
    }

    @Test
    void 포인트_증감_연속_두번_요청시_두번째는_실패한다() throws InterruptedException {
        // given
        Long userId = 1L;
        Long amount = 1000L;
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        AtomicReference<String> failMessage = new AtomicReference<>();

        // when
        for (int i = 0; i < 2; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    balanceService.increase(userId, amount);
                    successCount.incrementAndGet();
                } catch (IllegalStateException e) {
                    failMessage.set(e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Balance finalBalance = balanceService.getBalance(userId);
        assertEquals(1, successCount.get(), "성공 횟수는 1이어야 합니다");
        assertEquals(1, failCount.get(), "실패 횟수는 1이어야 합니다");
        assertEquals(11000L, finalBalance.getAmount(), "잔액이 1000원만 증감되어야 합니다");
        assertEquals("연속 요청은 처리할 수 없습니다", failMessage.get(), "연속 요청은 처리할 수 없습니다");
    }
}
