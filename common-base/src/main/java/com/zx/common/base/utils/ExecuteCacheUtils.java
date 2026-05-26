package com.zx.common.base.utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author ZhaoXu
 * 方法执行缓存
 * @date 2024/9/18 16:25
 */
@Slf4j
public class ExecuteCacheUtils {
    /**
     * 用于存储键值对
     */
    private static volatile Map<String, CacheValue<?>> CACHE_MAP;

    /**
     * 用于维护插入顺序
     */
    private static volatile Queue<CacheValue<?>> LINKED_DEQUE;

    /**
     * 方法执行缓存
     * @param methodExecutor  方法体
     * @param key    缓存唯一值，最终生成的唯一值为 className + methodName + key参数
     * @param expireTime    过期时间，单位毫秒
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <V> V execute(MethodExecutor<V> methodExecutor, String key, Long expireTime) {
        init();

        key = methodExecutor.getMethodName() + ":" + key;

        CacheValue<?> cacheValue = CACHE_MAP.computeIfAbsent(key, (newKey) -> {
            V result = methodExecutor.execute();

            CacheValue<V> value = new CacheValue<>(newKey, result, expireTime);
            LINKED_DEQUE.add(value);
            return value;
        });
        return (V) cacheValue.getValue();
    }

    public static <V> V execute(MethodExecutor<V> methodExecutor, String key, Long expireTime, TimeUnit timeUnit) {
        long timeUnitMillis = timeUnit.toMillis(expireTime);
        return execute(methodExecutor, key, timeUnitMillis);
    }

    private static void init() {
        if (CACHE_MAP == null) {
            synchronized (ExecuteCacheUtils.class) {
                if (CACHE_MAP == null) {
                    LINKED_DEQUE = new PriorityBlockingQueue<>(8, Comparator.comparingLong(CacheValue::getExpireTimestamp));
                    startCleaner();
                    CACHE_MAP = new ConcurrentHashMap<>(8);
                }
            }
        }
    }

    private static void startCleaner() {
        ScheduleUtils.randomDelaySchedule(() -> {
            Long nowTime = DateUtils.getNowTime();
            CacheValue<?> cacheValue;
            while ((cacheValue = LINKED_DEQUE.peek()) != null) {
                if (cacheValue.getExpireTimestamp() > nowTime) {
                    // 当前元素尚未过期，后面的元素也都未过期
                    break;
                }
                CacheValue<?> poll = LINKED_DEQUE.poll();
                String key = poll.getKey();
                log.info("ExecuteCacheUtils.cleaner, pollKey:{}", key);

                if (Objects.nonNull(key)) {
                    CACHE_MAP.remove(key);
                }
            }
        }, 1000);
    }

    @Data
    private static class CacheValue<V> {
        private final String key;
        private final V value;
        private final Long expireTimestamp;

        public CacheValue(String key, V value, Long expireTime) {
            this.key = key;
            this.value = value;
            Long nowTime = DateUtils.getNowTime();
            this.expireTimestamp = nowTime + expireTime;
        }
    }


    @FunctionalInterface
    public interface MethodExecutor<V> extends Serializable {
        /**
         * 方法执行
         * @return
         */
        V execute();

        /**
         * 获取lambda方法
         *
         * @return
         */
        default SerializedLambda getSerializedLambda() {
            Method write = null;
            try {
                write = this.getClass().getDeclaredMethod("writeReplace");
                write.setAccessible(true);
                return (SerializedLambda) write.invoke(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * 获取方法名称
         * @return
         */
        default String getMethodName() {
            SerializedLambda serializedLambda = getSerializedLambda();
            // 类名
            String className = serializedLambda.getImplClass();
            String[] packageSplit = className.split("/");
            className = packageSplit[packageSplit.length - 1];

            // 方法名
            String methodName = serializedLambda.getImplMethodName();
            return className + "#" + methodName;
        }
    }
}