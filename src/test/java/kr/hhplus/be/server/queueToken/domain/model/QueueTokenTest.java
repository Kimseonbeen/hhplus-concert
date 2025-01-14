package kr.hhplus.be.server.queueToken.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class QueueTokenTest {

    @Test
    @DisplayName("대기 상태인 경우 true를 반환한다")
    void isWaiting_WhenStatusIsWaiting_ReturnsTrue() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.WAITING)
                .build();

        // when & then
        assertTrue(token.isWaiting());
    }

    @Test
    @DisplayName("대기 상태가 아닌 경우 false를 반환한다")
    void isWaiting_WhenStatusIsNotWaiting_ReturnsFalse() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.ACTIVE)
                .build();

        // when & then
        assertFalse(token.isWaiting());
    }

    @Test
    @DisplayName("만료 상태인 경우 true를 반환한다")
    void isExpired_WhenStatusIsExpired_ReturnsTrue() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.EXPIRED)
                .build();

        // when & then
        assertTrue(token.isExpired());
    }

    @Test
    @DisplayName("만료 상태가 아닌 경우 false를 반환한다")
    void isExpired_WhenStatusIsNotExpired_ReturnsFalse() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.ACTIVE)
                .build();

        // when & then
        assertFalse(token.isExpired());
    }

    @Test
    @DisplayName("활성화 상태인 경우 true를 반환한다")
    void isActive_WhenStatusIsActive_ReturnsTrue() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.ACTIVE)
                .build();

        // when & then
        assertTrue(token.isActive());
    }

    @Test
    @DisplayName("활성화 상태가 아닌 경우 false를 반환한다")
    void isActive_WhenStatusIsNotActive_ReturnsFalse() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.WAITING)
                .build();

        // when & then
        assertFalse(token.isActive());
    }

    @Test
    @DisplayName("토큰을 만료 상태로 변경한다")
    void expire_ShouldChangeStatusToExpired() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.ACTIVE)
                .build();

        // when
        token.expire();

        // then
        assertEquals(token.getStatus(), QueueTokenStatus.EXPIRED);
    }

    @Test
    @DisplayName("토큰을 활성화하면 상태가 ACTIVE로 변경되고 만료시간이 10분 후로 설정된다")
    void activate_ShouldChangeStatusToActiveAndSetExpirationTime() {
        // given
        QueueToken token = QueueToken.builder()
                .status(QueueTokenStatus.WAITING)
                .build();

        // when
        token.activate();

        // then
        assertEquals(QueueTokenStatus.ACTIVE, token.getStatus());
        assertNotNull(token.getExpiredAt());
        assertTrue(token.getExpiredAt().isAfter(LocalDateTime.now()));
        assertTrue(token.getExpiredAt().isBefore(LocalDateTime.now().plusMinutes(10)));
    }


}