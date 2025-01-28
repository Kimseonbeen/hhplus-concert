package kr.hhplus.be.server.common.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분산락과 트랜잭션을 함께 사용하기 위한 AOP 컴포넌트
 */
@Component
public class AopForTransaction {

    /**
     *
     REQUIRES_NEW 트랜잭션 속성 사용 이유:
     분산락과 트랜잭션 격리
     기존 트랜잭션과 독립적 실행
     부모 트랜잭션 롤백시에도 자식 트랜잭션 독립성 보장
     이를 통해 락 해제 전에 트랜잭션이 완료되도록 보장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object proceed(final ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
