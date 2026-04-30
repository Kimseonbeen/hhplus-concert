package kr.hhplus.be.server.queueToken.domain;

import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenErrorCode;
import kr.hhplus.be.server.queueToken.domain.exception.QueueTokenException;
import kr.hhplus.be.server.queueToken.domain.model.QueueConstants;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueTokenServiceTest {
    @InjectMocks
    private QueueTokenService queueTokenService;

    @Mock
    private QueueTokenRepository queueTokenRepository;

    static final Long USER_ID = 1L;
    static final String TEST_TOKEN = "testToken";
    static final Long MAX_ACTIVE_USERS = QueueConstants.MAX_ACTIVE_USERS;

    @Test
    @DisplayName("MAX_ACTIVE_USERS 미만 일 시 토큰 ACTIVE_TOKEN 할당")
    void createToken_whenCountIsZero() {
        // given
        given(queueTokenRepository.getActiveTokenCount()).willReturn(0L);

        // when
        QueueToken response = queueTokenService.createToken(USER_ID);

        // then
        assertThat(response.getStatus()).isEqualTo(QueueTokenStatus.ACTIVE);
        verify(queueTokenRepository, times(1)).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("MAX_ACTIVE_USERS 이상 일 시 토큰 WAITING_TOKEN 할당")
    void createToken_whenCountIsEqualToMax() {
        // given
        given(queueTokenRepository.getActiveTokenCount()).willReturn(MAX_ACTIVE_USERS);

        // when
        QueueToken response = queueTokenService.createToken(USER_ID);

        // then
        assertThat(response.getStatus()).isEqualTo(QueueTokenStatus.WAITING);
        verify(queueTokenRepository, times(1)).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("최대치 - 1 일 시 ACTIVE 할당 (경계값)")
    void createToken_WhenBoundaryConditionActive() {
        // given
        given(queueTokenRepository.getActiveTokenCount()).willReturn(MAX_ACTIVE_USERS - 1);

        // when
        QueueToken response = queueTokenService.createToken(USER_ID);

        // then
        assertThat(response.getStatus()).isEqualTo(QueueTokenStatus.ACTIVE);
        verify(queueTokenRepository, times(1)).save(any(QueueToken.class));
    }

    @Test
    @DisplayName("조회 성공: ACTIVE 상태 토큰 반환")
    void getQueueToken_WhenStatusIsActive() {
        // given
        QueueToken createToken = QueueToken.createToken(USER_ID, QueueTokenStatus.ACTIVE);
        given(queueTokenRepository.findByToken(TEST_TOKEN)).willReturn(Optional.of(createToken));
        // when
        QueueTokenResponse response = queueTokenService.getQueueToken(TEST_TOKEN);

        // then
        assertThat(response.token()).isEqualTo(createToken.getToken());
        assertThat(response.status()).isEqualTo(createToken.getStatus());
    }

    @Test
    @DisplayName("토큰 조회 시 데이터 부재로 QUEUE_TOKEN_NOT_FOUND 에러 발생")
    void getActiveToken_WhenReadToken_ThrowsQUEUE_TOKEN_NOT_FOUND_ERROR() {
        // given
        given(queueTokenRepository.findByToken(TEST_TOKEN)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queueTokenService.getQueueToken(TEST_TOKEN))
                .isInstanceOf(QueueTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("유효성 검사 통과: ACTIVE 상태이며 만료되지 않은 토큰")
    void validateToken_should_pass_when_tokenIsActiveAndNotExpired() {
        // given
        QueueToken createToken = QueueToken.createToken(USER_ID, QueueTokenStatus.ACTIVE);
        given(queueTokenRepository.findByToken(TEST_TOKEN)).willReturn(Optional.of(createToken));

        // when & then
        queueTokenService.validateToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("서비스 진입 전 토큰 유효성 검사 : 실패 토큰 없음")
    void validateToken_throwsNotFoundException_when_tokenIsNotFound() {
        // given
        given(queueTokenRepository.findByToken(TEST_TOKEN)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> queueTokenService.validateToken(TEST_TOKEN))
                .isInstanceOf(QueueTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", QueueTokenErrorCode.QUEUE_TOKEN_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("서비스 진입 전 토큰 유효성 검사 : 실패 WAITING 상태")
    void validateToken_throwsNotActiveException_when_tokenIsWaiting() {
        // given
        QueueToken waitingToken = mock(QueueToken.class);
        given(queueTokenRepository.findByToken(TEST_TOKEN)).willReturn(Optional.of(waitingToken));

        // 토큰 비 활성화
        given(waitingToken.isActive()).willReturn(false);

        // when & then
        assertThatThrownBy(() -> queueTokenService.validateToken(TEST_TOKEN))
                .isInstanceOf(QueueTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", QueueTokenErrorCode.QUEUE_TOKEN_NOT_ACTIVE.getCode());
    }

    @Test
    @DisplayName("서비스 진입 전 토큰 유효성 검사 : 실패 만료된 토큰")
    void validateToken_WhenExpiredToken() {
        // given
        QueueToken expiredToken = mock(QueueToken.class);
        given(queueTokenRepository.findByToken(TEST_TOKEN)).willReturn(Optional.of(expiredToken));

        // 만료되었음을 설정
        given(expiredToken.isExpired()).willReturn(true);

        // when & then
        assertThatThrownBy(() -> queueTokenService.validateToken(TEST_TOKEN))
                .isInstanceOf(QueueTokenException.class)
                .hasFieldOrPropertyWithValue("errorCode", QueueTokenErrorCode.QUEUE_TOKEN_EXPIRED.getCode());
    }

    @Test
    @DisplayName("토큰 삭제: Repository removeToken 호출 검증")
    void expireToken_shouldCallRemoveToken() {
        // when
        queueTokenService.expireToken(TEST_TOKEN);

        // then
        verify(queueTokenRepository, times(1)).removeToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("활성화 필요 시, 정확한 Needs 값으로 원자적 활성화 메서드 호출")
    void activateNextWaitingToken_callsActivateMethodWithCorrectNeeds() {
        // 가정: QueueConstants.MAX_ACTIVE_TOKEN_COUNT = 200L (서비스가 허용하는 최대치)
        // 가정: QueueConstants.MAX_ACTIVE_USERS = 100L (createToken에서 사용되는 상수)

        // 1. Given: 현재 활성 토큰 개수가 50개라고 가정
        long currentActiveCount = 50L;

        // 2. Needs 계산: 200L - 50L = 150L
        long expectedNeeds = QueueConstants.MAX_ACTIVE_USERS - currentActiveCount; // Service 로직과 동일해야 함

        // 3. Stubbing: 서비스 로직이 의존하는 getActiveTokenCount()를 Mocking
        given(queueTokenRepository.getActiveTokenCount()).willReturn(currentActiveCount);

        // 4. Stubbing: atomicallyActivateWaitingTokens가 호출될 때 10개 활성화되었다고 가정
        given(queueTokenRepository.atomicallyActivateWaitingTokens(expectedNeeds)).willReturn(10L);

        // when
        queueTokenService.activateNextWaitingToken();

        // then
        // 1. needs 값이 정확히 계산되어 전달되었는지 검증 (가장 중요)
        verify(queueTokenRepository, times(1)).atomicallyActivateWaitingTokens(expectedNeeds);

        // 2. getActiveTokenCount가 한 번 호출되었는지 확인
        verify(queueTokenRepository, times(1)).getActiveTokenCount();

        // 참고: getWaitingTokenCount는 이 로직에서 호출되지 않았으므로 검증하지 않음.
    }
}