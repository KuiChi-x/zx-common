package com.zx.common.cache.aspect;

import com.zx.common.cache.annotation.RedisLock;
import com.zx.common.cache.exception.RedLockException;
import com.zx.common.cache.redlock.RedLock;
import com.zx.common.base.utils.AopHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author ZhaoXu
 * @date 2022/6/13 14:46
 */
@Component
@Aspect
@Slf4j
public class RedisLockAspect {
    private static final Integer slotCountSum = 16384;

    public static List<Jedis> jedisList = new ArrayList<>();

    @Around("@annotation(redisLock)")
    public Object doAround(ProceedingJoinPoint joinPoint, RedisLock redisLock) throws Throwable {
        String lockKey = AopHelper.parseAnnotationText(joinPoint, redisLock.lockKey());

        List<RedLock> redLockList = buildRedLocks(lockKey);
        boolean locked = false;
        try {
            locked = lock(redisLock, redLockList);
            if (locked) {
                return joinPoint.proceed();
            }
        } finally {
            if (locked) {
                unlock(redLockList);
            }
        }
        return null;
    }

    private List<RedLock> buildRedLocks(String lockKey) {
        if (ObjectUtils.isEmpty(jedisList)) {
            throw new RedLockException("redis节点列表为空");
        }
        List<RedLock> redLockList = new ArrayList<>();
        int offset = lockKey.hashCode() & (slotCountSum - 1);
        // redis槽间隔
        int step = slotCountSum / jedisList.size();
        for (int i = 0; i < jedisList.size(); i++) {
            String newKey = lockKey + ".{lock-" + (offset + (i * step) & (slotCountSum - 1)) + "}";
            Jedis jedis = jedisList.get(i);
            RedLock redLock = new RedLock(jedis, newKey, UUID.randomUUID().toString());
            redLockList.add(redLock);
        }
        return redLockList;
    }

    private boolean lock(RedisLock redisLock, List<RedLock> redLockList) throws InterruptedException {
        // 最小上锁节点数
        int minCount = (jedisList.size() >> 1) + 1;
        long leaseTime = redisLock.leaseTime();
        // 整体开始上锁时间
        long startTime = System.currentTimeMillis();

        // 上锁成功节点数
        int successCount = 0;
        while (checkoutWaitTimeOut(redisLock.waitTime(), startTime)) {
            // 这一批开始上锁时间
            long batchStartTime = System.currentTimeMillis();
            for (RedLock redLock : redLockList) {
                if (redLock.tryLock(batchStartTime, leaseTime)) {
                    successCount++;
                }
                // 到达等待时间
                if (redisLock.waitTime() > 0 && System.currentTimeMillis() - startTime >= redisLock.waitTime()) {
                    break;
                }
            }
            // 这一批上锁没有过期
            if (successCount >= minCount && System.currentTimeMillis() - batchStartTime < leaseTime) {
                return true;
            }
            unlock(redLockList);
            if (!checkoutWaitTimeOut(redisLock.waitTime(), startTime - 10L)) {
                break;
            }
            successCount = 0;
            Thread.sleep(10L);
        }
        log.error("lock timeout, redisLock:{}", redisLock);
        throw new RedLockException("red lock timeout, please retry!");
    }

    private boolean checkoutWaitTimeOut(long waitTime, long startTime) {
        return waitTime <= 0 || System.currentTimeMillis() - startTime < waitTime;
    }

    private void unlock(List<RedLock> redLockList) {
        for (RedLock redLock : redLockList) {
            redLock.unlock();
        }
    }
}
