package com.zx.common.base.utils;

import com.zx.common.base.model.Command;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhaoXu
 * @date 2024/7/5 10:35
 */
public class ScheduleUtils {
    private static volatile ScheduledExecutorService SCHEDULED_EXECUTOR;

    private static ScheduledExecutorService getScheduledExecutor() {
        if (SCHEDULED_EXECUTOR == null) {
            synchronized (ScheduleUtils.class) {
                if (SCHEDULED_EXECUTOR == null) {
                    int availableProcessors = Math.max(Runtime.getRuntime().availableProcessors() * 2, 10);
                    SCHEDULED_EXECUTOR = new ScheduledThreadPoolExecutor(availableProcessors, new ThreadPoolExecutor.AbortPolicy());
                }
            }
        }
        return SCHEDULED_EXECUTOR;
    }

    /**
     * 执行定时任务，延时以及间隔单位都是毫秒
     * @param runnable
     * @param delay
     * @param period
     */
    public static void schedule(Runnable runnable, int delay, int period) {
        Command command = new Command(runnable);
        getScheduledExecutor().scheduleWithFixedDelay(command, delay, period, TimeUnit.MILLISECONDS);
    }

    /**
     * 随机延时 间隔时间/2 - 间隔时间 进行定时任务执行
     * @param runnable
     * @param period
     */
    public static void randomDelaySchedule(Runnable runnable, int period) {
        int randomDelay = ThreadLocalRandom.current().nextInt(period / 2, period);
        schedule(runnable, randomDelay, period);
    }
}
