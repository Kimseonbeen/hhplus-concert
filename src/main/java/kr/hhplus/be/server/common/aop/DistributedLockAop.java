package kr.hhplus.be.server.common.aop;

import kr.hhplus.be.server.common.annotation.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락을 구현한 AOP 클래스
 */

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {

    // Redis 락의 키 접두사
    private static final String REDISSON_LOCK_PREFIX = "LOCK:";

    // Redis 클라이언트
    private final RedissonClient redissonClient;

    // 트렌젝션 처리를 위한 AOP
    private final AopForTransaction aopForTransaction;

    /**
     * @DistributedLock 어노테이션이 붙은 메서드에 적용되는 AOP
     */
    private static final String REQUEST_HISTORY_PREFIX = "REQUEST_HISTORY:";
    private static final long BLOCK_TIME_SECONDS = 2;  // 2초 동안 추가 요청 차단

    @Around("@annotation(kr.hhplus.be.server.common.annotation.DistributedLock)")
    public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(signature.getParameterNames(), joinPoint.getArgs(), distributedLock.key());

        log.info("생성된 락 키: {}", key);

        RLock rLock = redissonClient.getLock(key);

        try {
            log.info("락 획득 시도 - 키: {}, 대기시간: {}초, 유지시간: {}초", key, distributedLock.waitTime(), distributedLock.leaseTime());

            /**
             * 0: 락 획득 대기시간 (즉시 시도)
             */
            boolean acquired = rLock.tryLock(0, distributedLock.waitTime(), distributedLock.timeUnit());
            if (!acquired) {
                log.warn("연속 요청 차단: {}", key);
                throw new IllegalStateException("연속 요청은 처리할 수 없습니다");
            }

            log.info("락 획득 성공: {}", key);

            return aopForTransaction.proceed(joinPoint);
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }
    }
}
