package kr.hhplus.be.server.common.aop;

import kr.hhplus.be.server.common.annotation.DistributedLock;
import kr.hhplus.be.server.common.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DistributedLockAopTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private AopForTransaction aopForTransaction;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @Mock
    private RLock rLock;

    @InjectMocks
    private DistributedLockAop distributedLockAop;

    // 어노테이션 추출용 테스트 메서드
    @DistributedLock(key = "'lock:' + #userId")
    public void testMethod(Long userId) {}

    @Test
    @DisplayName("락 획득 실패 시 TooManyRequestsException(429)이 발생한다")
    void lock_WhenAcquisitionFails_ThrowsTooManyRequestsException() throws Throwable {
        // given
        Method method = this.getClass().getMethod("testMethod", Long.class);

        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getMethod()).willReturn(method);
        given(signature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{1L});
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any())).willReturn(false);

        // when & then
        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> distributedLockAop.lock(joinPoint));

        assertEquals(429, exception.getStatus());
    }

    @Test
    @DisplayName("락 획득 성공 시 정상적으로 처리된다")
    void lock_WhenAcquisitionSucceeds_ProceedsNormally() throws Throwable {
        // given
        Method method = this.getClass().getMethod("testMethod", Long.class);
        Object expected = new Object();

        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getMethod()).willReturn(method);
        given(signature.getParameterNames()).willReturn(new String[]{"userId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{1L});
        given(redissonClient.getLock(anyString())).willReturn(rLock);
        given(rLock.tryLock(anyLong(), anyLong(), any())).willReturn(true);
        given(rLock.isHeldByCurrentThread()).willReturn(true);
        given(aopForTransaction.proceed(joinPoint)).willReturn(expected);

        // when
        Object result = distributedLockAop.lock(joinPoint);

        // then
        assertEquals(expected, result);
        verify(rLock).unlock();
    }
}
