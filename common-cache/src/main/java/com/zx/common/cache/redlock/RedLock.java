package com.zx.common.cache.redlock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

/**
 * @author ZhaoXu
 * @date 2022/6/13 14:42
 */
public class RedLock {
    private final String key;
    private final String value;
    private final Jedis jedis;

    public RedLock(Jedis jedis, String key, String value) {
        this.jedis = jedis;
        this.key = key;
        this.value = value;
    }

    public boolean tryLock(long batchStartTime, long leaseTime) {
        try {
            SetParams setParams = new SetParams();
            setParams.nx();
            setParams.px(leaseTime);
            String result = jedis.set(key, value, setParams);
            if ("OK".equals(result) && System.currentTimeMillis() - batchStartTime < leaseTime) {
                return true;
            }
            unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void unlock() {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        try {
            jedis.eval(script, 1, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
