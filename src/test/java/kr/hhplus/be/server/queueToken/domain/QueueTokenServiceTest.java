package kr.hhplus.be.server.queueToken.domain;

import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueueTokenServiceTest {

    @InjectMocks
    private QueueTokenService queueTokenService;

    @Mock
    private QueueTokenRepository queueTokenRepository;

    @Test
    @DisplayName("대기자가 없고 활성 사용자가 3명 미만일 때 ACTIVE 상태로 토큰 발급")
    void issueQueueToken_ShouldIssueActiveToken() {
        // given
        long userId = 1L;
        given(queueTokenRepository.countByStatus(QueueTokenStatus.ACTIVE)).willReturn(2L);
        given(queueTokenRepository.countByStatus(QueueTokenStatus.WAITING)).willReturn(0L);

        // when
        QueueToken result = queueTokenService.issueQueueToken(userId);

        // then
        assertThat(result.getStatus()).isEqualTo(QueueTokenStatus.ACTIVE);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getExpiredAt()).isNotNull();
        verify(queueTokenRepository).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("활성 사용자가 3명이고 대기자가 있을 때 WAITING 상태로 토큰 발급")
    void issueQueueToken_ShouldIssueWaitingToken() {
        // given
        long userId = 1L;
        given(queueTokenRepository.countByStatus(QueueTokenStatus.ACTIVE)).willReturn(3L);
        given(queueTokenRepository.countByStatus(QueueTokenStatus.WAITING)).willReturn(5L);

        // when
        QueueToken result = queueTokenService.issueQueueToken(userId);

        // then
        assertThat(result.getStatus()).isEqualTo(QueueTokenStatus.WAITING);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getExpiredAt()).isNull();
        verify(queueTokenRepository).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("활성 사용자가 3명 꽉 찼을 때 WAITING 상태로 토큰 발급")
    void issueQueueToken_WhenActiveFullShouldIssueWaitingToken() {
        // given
        long userId = 1L;
        given(queueTokenRepository.countByStatus(QueueTokenStatus.ACTIVE)).willReturn(3L);
        given(queueTokenRepository.countByStatus(QueueTokenStatus.WAITING)).willReturn(0L);

        // when
        QueueToken result = queueTokenService.issueQueueToken(userId);

        // then
        assertThat(result.getStatus()).isEqualTo(QueueTokenStatus.WAITING);
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getExpiredAt()).isNull();
        verify(queueTokenRepository).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("ACTIVE 상태 토큰 조회시 대기 번호는 0번으로 응답")
    void getQueueToken_WhenStatusIsActive() {
        // given
        String token = "test-token";
        QueueToken queueToken = QueueToken.builder()
                .id(1L)
                .token(token)
                .status(QueueTokenStatus.ACTIVE)
                .expiredAt(LocalDateTime.now().plusMinutes(10))
                .build();

        given(queueTokenRepository.findByToken(token)).willReturn(queueToken);

        // when
        QueueTokenResponse response = queueTokenService.getQueueToken(token);

        // then
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.status()).isEqualTo(QueueTokenStatus.ACTIVE);
        assertThat(response.num()).isEqualTo(0);
        assertThat(response.expiredAt()).isNotNull();
    }

    @Test
    @DisplayName("WAITING 상태 토큰 조회시 현재 대기 순서 계산하여 응답")
    void getQueueToken_WhenStatusIsWaiting() {
        // given
        String token = "test-token";
        QueueToken queueToken = QueueToken.builder()
                .id(5L)
                .token(token)
                .status(QueueTokenStatus.WAITING)
                .build();

        given(queueTokenRepository.findByToken(token)).willReturn(queueToken);
        given(queueTokenRepository.countByStatusAndIdLessThan(QueueTokenStatus.WAITING, 5L))
                .willReturn(2L);

        // when
        QueueTokenResponse response = queueTokenService.getQueueToken(token);

        // then
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.status()).isEqualTo(QueueTokenStatus.WAITING);
        assertThat(response.num()).isEqualTo(3L);  // 2 + 1
        assertThat(response.expiredAt()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 토큰으로 조회시 토큰 없음 예외 발생")
    void getQueueToken_WhenTokenNotFound() {
        // given
        String token = "non-existing-token";
        given(queueTokenRepository.findByToken(token)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> queueTokenService.getQueueToken(token))
                .isInstanceOf(RuntimeException.class);
    }

}