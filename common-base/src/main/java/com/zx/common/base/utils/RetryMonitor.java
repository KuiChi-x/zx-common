package com.zx.common.base.utils;

import lombok.Data;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhaoXu
 * @date 2022/5/1 11:54
 */
public class RetryMonitor {
    @FunctionalInterface
    public interface Execute {
        /**
         * 执行方法
         */
        void execute();
    }

    @Data
    private static class RetryBO {
        private Execute execute;

        private Integer retryCount;
    }

    private static final Logger log = LoggerFactory.getLogger(RetryMonitor.class);

    private static final BlockingQueue<Pair<RetryBO, Integer>> FAILED_QUEUE = new LinkedBlockingQueue<>();

    private static final ScheduledExecutorService RETRY_TASK_EXECUTOR = new ScheduledThreadPoolExecutor(1,
            new BasicThreadFactory.Builder().namingPattern("retryTaskExecutor").build());

    static {
        RETRY_TASK_EXECUTOR.scheduleAtFixedRate(() -> {
            Pair<RetryBO, Integer> take = null;
            try {
                take = FAILED_QUEUE.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            RetryBO retryBO = take.getKey();
            Execute execute = retryBO.getExecute();
            Integer retryCount = retryBO.getRetryCount();
            try {
                execute.execute();
            } catch (Exception e) {
                Integer failedNumber = take.getValue();
                log.error("重试第 {} 次失败", ++failedNumber, e);
                if (failedNumber < retryCount) {
                    FAILED_QUEUE.offer(Pair.of(retryBO, failedNumber));
                }
            }
        }, 3, 30, TimeUnit.SECONDS);
    }

    public static void registry(Execute retryFunction, Integer retryCount) {
        if (Objects.isNull(retryFunction)) {
            return;
        }
        try {
            retryFunction.execute();
        } catch (Exception e) {
            RetryBO retryBO = new RetryBO();
            retryBO.setRetryCount(retryCount);
            retryBO.setExecute(retryFunction);
            FAILED_QUEUE.offer(Pair.of(retryBO, 1));
            log.error("执行第 {}/{} 次失败，进入重试队列", 1, retryCount, e);
        }
    }
}
