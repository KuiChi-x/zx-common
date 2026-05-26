package com.zx.common.base.model;

import lombok.extern.slf4j.Slf4j;

/**
 * @author ZhaoXu
 * @date 2024/7/12 15:08
 */
@Slf4j
public class Command implements Runnable {
    private final Runnable runnable;

    public Command(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Throwable e) {
            log.error("{}.run.error", runnable.getClass().getName(), e);
        }
    }
}
