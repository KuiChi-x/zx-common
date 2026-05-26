package com.zx.common.cache.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import redis.clients.jedis.Jedis;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 令牌桶限流算法实现
 *
 * @author ZhaoXu
 * @date 2023/5/11
 */
public class FlowLimiterUtils {
    private static final Long SUCCESS_FLAG = 1L;
    private static final String LIMITER_KEY = "common:limiter:";

    private static Supplier<Jedis> jedisSupplier;

    /**
     * 设置Jedis实例提供者
     * @param supplier 提供Jedis实例的Supplier
     */
    public static void setJedisSupplier(Supplier<Jedis> supplier) {
        jedisSupplier = supplier;
    }

    /**
     * 是否允许访问（默认秒级限流，消耗1个令牌）
     *
     * @param rate 每秒填充速率
     * @param capacity 令牌桶容量
     * @param businessType 业务类型标识
     * @return true 允许访问 false 不允许访问
     */
    public static boolean isAllowed(Integer rate, Integer capacity, String businessType) {
        return isAllowed(rate, capacity, businessType, LimitUnitEnums.SECONDS, 1);
    }

    /**
     * 是否允许访问
     *
     * @param rate 填充速率
     * @param capacity 令牌桶容量
     * @param businessType 业务类型标识
     * @param limitUnit 限流时间单位
     * @param requested 请求消耗的令牌数
     * @return true 允许访问 false 不允许访问
     */
    public static boolean isAllowed(Integer rate, Integer capacity, String businessType, LimitUnitEnums limitUnit, Integer requested) {
        if (ObjectUtils.anyNull(rate, capacity, businessType, requested)) {
            return false;
        }
        if (jedisSupplier == null) {
            throw new IllegalStateException("FlowLimiterUtils: jedisSupplier未初始化，请先调用setJedisSupplier()");
        }
        List<String> keys = getKey(businessType);
        try (Jedis jedis = jedisSupplier.get()) {
            String scriptLoad = jedis.scriptLoad(SCRIPT);
            LimitUnitEnums limitUnitEnum = Optional.ofNullable(limitUnit).orElse(LimitUnitEnums.SECONDS);
            Long nowTime = limitUnitEnum.getNowTime();
            Object result = jedis.evalsha(scriptLoad, keys, Arrays.asList(rate.toString(), capacity.toString(), String.valueOf(nowTime), requested.toString()));
            return Objects.equals(result, SUCCESS_FLAG);
        }
    }

    private static List<String> getKey(String id) {
        String prefix = LIMITER_KEY + id;
        String tokenKey = prefix + ":tokens";
        String timestampKey = prefix + ":timestamp";
        return Arrays.asList(tokenKey, timestampKey);
    }

    private static final String SCRIPT =
            "local tokens_key = KEYS[1]\n" +
                    "local timestamp_key = KEYS[2]\n" +
                    "local rate = tonumber(ARGV[1])\n" +
                    "local capacity = tonumber(ARGV[2])\n" +
                    "local now = tonumber(ARGV[3])\n" +
                    "local requested = tonumber(ARGV[4])\n" +
                    "local fill_time = capacity/rate\n" +
                    "local ttl = math.floor(fill_time*2)\n" +
                    "local last_tokens = tonumber(redis.call('get', tokens_key))\n" +
                    "if last_tokens == nil then\n" +
                    "  last_tokens = capacity\n" +
                    "end\n" +
                    "local last_refreshed = tonumber(redis.call('get', timestamp_key))\n" +
                    "if last_refreshed == nil then\n" +
                    "  last_refreshed = 0\n" +
                    "end\n" +
                    "local diff_time = math.max(0, now-last_refreshed)\n" +
                    "local filled_tokens = math.min(capacity, last_tokens+(diff_time*rate))\n" +
                    "local allowed = filled_tokens >= requested\n" +
                    "local new_tokens = filled_tokens\n" +
                    "local allowed_num = 0\n" +
                    "if allowed then\n" +
                    "  new_tokens = filled_tokens - requested\n" +
                    "  allowed_num = 1\n" +
                    "end\n" +
                    "if ttl > 0 then\n" +
                    "  redis.call('setex', tokens_key, ttl, new_tokens)\n" +
                    "  redis.call('setex', timestamp_key, ttl, now)\n" +
                    "end\n" +
                    "return allowed_num\n";

    @Getter
    @AllArgsConstructor
    public enum LimitUnitEnums {
        SECONDS,
        MILLISECONDS;

        public Long getNowTime() {
            if (Objects.equals(this, SECONDS)) {
                LocalDateTime localDateTime = LocalDateTime.now();
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Shanghai"));
                return zonedDateTime.toEpochSecond();
            }
            return System.currentTimeMillis();
        }
    }
}
