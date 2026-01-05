package com.tigger.closetconnectproject.Common.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정 (DEPRECATED)
 * - 옷 이미지 처리 파이프라인을 백그라운드에서 실행하기 위한 Executor 설정
 *
 * @deprecated RabbitMQ로 마이그레이션됨. RabbitMQConfig 사용
 * @see com.tigger.closetconnectproject.Common.Config.RabbitMQConfig
 */
@Deprecated
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.executor.core-pool-size:5}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:10}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:100}")
    private int queueCapacity;

    /**
     * 옷 이미지 처리 전용 Executor
     *
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = "clothProcessingExecutor")
    public Executor clothProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 스레드 풀 설정
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);

        // 스레드 이름 접두사 (로그 추적 용이)
        executor.setThreadNamePrefix("cloth-processing-");

        // 큐가 가득 찼을 때 정책: 호출한 스레드에서 직접 실행
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 애플리케이션 종료 시 스레드 풀 정리
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
