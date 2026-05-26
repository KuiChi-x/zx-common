package com.zx.common.base.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author ZhaoXu
 * @date 2023/11/1 11:02
 */
@Configuration
public class BasicThreadPoolConfig {
    @Bean(name = "basicThreadPoolExecutor")
    public ThreadPoolTaskExecutor configThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        taskExecutor.setCorePoolSize(availableProcessors * 2);
        taskExecutor.setMaxPoolSize(availableProcessors * 2);
        taskExecutor.setQueueCapacity(1024);
        taskExecutor.setThreadNamePrefix("repositoryPool-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setKeepAliveSeconds(60);
        taskExecutor.initialize();
        return taskExecutor;
    }
}
