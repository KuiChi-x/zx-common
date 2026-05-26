package com.zx.common.base.utils;

import com.zx.common.base.model.Command;
import lombok.Setter;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhaoXu
 * @date 2024/7/5 10:35
 */
public class ThreadPoolUtils {
    private static volatile ThreadPoolExecutor executor;

    @Setter
    private static Integer MIN_SIZE = 10;

    public static ThreadPoolExecutor getExecutor() {
        if (executor == null) {
            synchronized (ThreadPoolUtils.class) {
                if (executor == null) {
                    int availableProcessors = Math.max(Runtime.getRuntime().availableProcessors() * 2, MIN_SIZE);
                    executor = new ThreadPoolExecutor(availableProcessors,
                            availableProcessors * 2,
                            60,
                            TimeUnit.SECONDS,
                            new ArrayBlockingQueue<>(512),
                            new ThreadPoolExecutor.CallerRunsPolicy());
                }
            }
        }
        return executor;
    }

    public static void execute(Runnable runnable) {
        Command command = new Command(runnable);
        getExecutor().execute(command);
    }
}
