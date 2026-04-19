package com.mtmn.smartrag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 *
 * @author charmingdaidai
 * @version 2.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 索引构建专用线程池
     * 核心线程数: 2 - 控制并发索引任务数量
     * 最大线程数: 4 - 允许短时间内的峰值处理
     * 队列容量: 100 - 等待执行的任务数
     */
    @Bean("indexingExecutor")
    public Executor indexingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("indexing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
