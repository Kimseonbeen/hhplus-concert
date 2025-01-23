package kr.hhplus.be.server.queueToken.domain;

import kr.hhplus.be.server.queueToken.domain.model.QueueToken;
import kr.hhplus.be.server.queueToken.domain.model.QueueTokenStatus;
import kr.hhplus.be.server.queueToken.domain.repository.QueueTokenRepository;
import kr.hhplus.be.server.queueToken.domain.service.QueueTokenService;
import kr.hhplus.be.server.queueToken.presentation.dto.response.QueueTokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        given(queueTokenRepository.findByToken(token)).willReturn(Optional.of(queueToken));

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

        given(queueTokenRepository.findByToken(token)).willReturn(Optional.of(queueToken));
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

    @Test
    @DisplayName("ACTIVE 상태이고 만료시간이 지난 토큰 목록을 조회한다")
    void findExpiredActiveTokens_WhenTokensExpired_ReturnExpiredActiveTokens() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<QueueToken> expiredTokens = List.of(
                QueueToken.builder()
                        .status(QueueTokenStatus.ACTIVE)
                        .expiredAt(now.minusMinutes(11))    // 만료 시간 설정
                        .build()
        );

        given(queueTokenRepository.findByStatusAndExpiredAtBefore(
                eq(QueueTokenStatus.ACTIVE),
                any(LocalDateTime.class))).willReturn(expiredTokens);

        // when
        List<QueueToken> result = queueTokenService.findExpiredActiveTokens();

        // then
        assertThat(result).hasSize(1);
        verify(queueTokenRepository).findByStatusAndExpiredAtBefore(
                any(QueueTokenStatus.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("토큰의 상태를 EXPIRED로 변경하고 만료 처리한다")
    void expireToken_WhenTokenActive_ChangeStatusToExpired() {
        // given
        Long userId = 1L;
        QueueToken token = QueueToken.builder()
                .userId(userId)
                .status(QueueTokenStatus.ACTIVE)
                .build();

        given(queueTokenRepository.findByUserId(userId)).willReturn(Optional.of(token));
        given(queueTokenRepository.save(token)).willReturn(token);

        // when
        queueTokenService.expireToken(userId);

        // then
        assertThat(token.getStatus()).isEqualTo(QueueTokenStatus.EXPIRED);
    }

    @Test
    @DisplayName("가장 오래된 WAITING 토큰을 찾아서 ACTIVE로 변경하고 10분 뒤 만료되도록 설정한다")
    void activateNextWaitingToken_WhenWaitingTokenExists_ChangeToActiveWithExpiration() {
        // given
        QueueToken waitingToken = QueueToken.builder()
                .status(QueueTokenStatus.WAITING)
                .build();

        given(queueTokenRepository.findFirstByStatusOrderByIdAsc(QueueTokenStatus.WAITING))
                .willReturn(Optional.of(waitingToken));

        // when
        queueTokenService.activateNextWaitingToken();

        // then
        assertThat(waitingToken.getStatus()).isEqualTo(QueueTokenStatus.ACTIVE);
        assertThat(waitingToken.getExpiredAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("WAITING 상태인 토큰이 없을 경우 활성화 처리를 수행하지 않는다")
    void activateNextWaitingToken_WhenNoWaitingToken_DoNothing() {
        // given
        given(queueTokenRepository.findFirstByStatusOrderByIdAsc(QueueTokenStatus.WAITING))
                .willReturn(Optional.empty());

        // when
        queueTokenService.activateNextWaitingToken();

        // then
        verify(queueTokenRepository).findFirstByStatusOrderByIdAsc(QueueTokenStatus.WAITING);
    }

}