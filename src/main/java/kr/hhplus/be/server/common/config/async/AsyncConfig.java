package kr.hhplus.be.server.common.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 기본 스레드 풀 크기 설정
        // 항상 이 개수만큼의 스레드를 유지 (2개)
        executor.setCorePoolSize(2);

        // 최대 스레드 풀 크기 설정
        // 큐가 가득 찼을 때 추가로 생성할 수 있는 최대 스레드 수 (5개)
        executor.setMaxPoolSize(5);

        // 작업 큐의 용량 설정
        // CorePoolSize가 모두 사용중일 때 대기할 수 있는 작업 수 (10개)
        executor.setQueueCapacity(10);
        // 생성되는 스레드의 이름 접두사 설정
        // 스레드 이름: DataPlatform-1, DataPlatform-2, ...
        executor.setThreadNamePrefix("DataPlatform-");

        // 설정한 내용으로 ThreadPool 초기화
        executor.initialize();
        return executor;
    }
}
