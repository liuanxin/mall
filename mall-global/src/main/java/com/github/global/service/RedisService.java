package com.github.global.service;

import com.github.common.util.U;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ RedisTemplate.class })
public class RedisService {

    /** @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration */
    private final RedisTemplate<Object, Object> redisTemplate;


    /** 从 redis 中删值, 对应命令: DEL key */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
    /** 从 redis 中删值, value 非 string 时尽量用这个. 对应命令: UNLINK key */
    public void unlink(String key) {
        redisTemplate.unlink(key);
    }

    /** 往 redis 中放值, 对应命令: SET key value */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }
    /** 往 redis 放值, 并设定超时时间, 对应命令: SET key value PX ms */
    public <T> void set(String key, T value, long time, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, time, unit);
    }
    /** 往 redis 放值, 并设定在什么时间超时, 对应命令: SET key value PX ms */
    public <T> void set(String key, T value, Date expireTime) {
        if (expireTime != null) {
            Date now = new Date();
            if (expireTime.after(now)) {
                set(key, value, now.getTime() - expireTime.getTime(), TimeUnit.MILLISECONDS);
            }
        }
    }

    /** 设置超时时间, 对应命令: PEXPIRE key ms */
    public void expire(String key, long time, TimeUnit unit) {
        redisTemplate.expire(key, time, unit);
    }

    /** 自增, 对应命令: INCRBY key increment */
    public long incr(String key) {
        Long inc = redisTemplate.opsForValue().increment(key, 1L);
        return inc == null ? 0L : inc;
    }

    /** 从 redis 中取值, 对应命令: GET key */
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }


    /**
     * 用 redis 获取分布式锁, 获取成功则运行并返回(释放不用调用方处理)
     * <pre>
     *
     * String key = "xxx";
     * return tryLockAndRun(key, () -> {
     *     // 获取到锁之后的处理
     *     return xxx;
     * });
     * </pre>
     * @param key 键
     */
    public <T> T tryLockAndRun(String key, Supplier<T> supplier) {
        String value = U.uuid16();
        if (tryLock(key, value)) {
            try {
                return supplier.get();
            } finally {
                unlock(key, value);
            }
        }
        return null;
    }

    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * if (tryLock(key, value)) {
     *   try {
     *     // 获取到锁之后的业务处理, 此锁会保持 10 秒
     *   } finally {
     *     unlock(key, value);
     *   }
     * } else {
     *   log.info("未获取到锁", time);
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     */
    public boolean tryLock(String key, String value) {
        return tryLock(key, value, 10, TimeUnit.SECONDS);
    }

    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * int lockTime = 5;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * if (tryLock(key, value, lockTime, unit)) {
     *   try {
     *     // 获取到锁之后的处理, 此锁会保持 5 秒
     *   } finally {
     *     unlock(key, value);
     *   }
     * } else {
     *   log.info("未获取到锁");
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     * @param lockTime 锁的超时时间
     * @param unit 锁超时的时间单位
     * @return 返回 true 则表示获取到了锁
     */
    public boolean tryLock(String key, String value, long lockTime, TimeUnit unit) {
        String script = "if redis.call('set', KEYS[1], KEYS[2], 'PX', KEYS[3], 'NX') then return 1 else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        List<Object> keys = Arrays.asList(key, value, unit.toMillis(lockTime));
        Long flag = redisTemplate.execute(redisScript, keys);
        return flag != null && flag == 1L;
    }

    /**
     * 解锁, key 和 value 都需要
     *
     * @param key 键
     * @param value 值
     */
    public void unlock(String key, String value) {
        // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作, 避免当前线程执行太长, 超时后其他线程又设置了值在处理. 之后当前线程又执行此动作
        String script = "if redis.call('get', KEYS[1]) == KEYS[2] then redis.call('del', KEYS[1]); end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(key, value));
    }


    // list: 左进左出则可以构建栈(右进右出也是), 左进右出则可以构建队列(右进左出也是)

    /** 向 list 写值(从左边入), 对应命令: LPUSH key value */
    public <T> void leftPush(String key, T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }
    /** 向 list 写值(从右边入), 对应命令: RPUSH key value */
    public <T> void rightPush(String key, T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }
    /** 向 list 取值(从左边出), 对应命令: LPOP key */
    public <T> T leftPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }
    /** 向 list 取值(从左边出, 并阻塞指定的时间), 对应命令: BLPOP key timeout */
    public <T> T leftPop(String key, int time, TimeUnit unit) {
        return (T) redisTemplate.opsForList().leftPop(key, time, unit);
    }
    /** 向 list 取值(从右边出), 对应命令: RPOP key */
    public <T> T rightPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }
    /** 向 list 取值(从右边出, 并阻塞指定的时间), 对应命令: BRPOP key timeout */
    public <T> T rightPop(String key, int time, TimeUnit unit) {
        return (T) redisTemplate.opsForList().rightPop(key, time, unit);
    }


    // set 的特点是每个元素唯一, 但是不保证排序

    /** 获取指定 set 的长度, 对应命令: SCARD key */
    public int setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size.intValue() : 0;
    }
    /** 将指定的 set 存进 redis 并返回成功条数, 对应命令: SADD key v1 v2 v3 ... */
    public <T> int setAdd(String key, T[] set) {
        Long add = redisTemplate.opsForSet().add(key, set);
        return add != null ? add.intValue() : 0;
    }
    /** 获取 set, 对应命令: SMEMBERS key */
    public <T> Set<T> setGet(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }
    /** 从 set 中移除一个值, 对应命令: SREM key value */
    public <T> void setRemove(String key, T value) {
        redisTemplate.opsForSet().remove(key, value);
    }
    /** 从指定的 set 中随机取出一个值, 对应命令: SPOP key */
    public <T> T setPop(String key) {
        return (T) redisTemplate.opsForSet().pop(key);
    }
    /** 从指定的 set 中随机取出一些值, 对应命令: SPOP key count */
    public <T> T setPop(String key, int count) {
        return (T) redisTemplate.opsForSet().pop(key, count);
    }


    // hash 键值对

    /** 写 hash, 对应命令: HMSET key field value [field value ...] */
    public <T> void hashPutAll(String key, Map<String, T> hashMap) {
        redisTemplate.opsForHash().putAll(key, hashMap);
    }
    /** 写 hash, 对应命令: HSET key field value */
    public <T> void hashPut(String key, String hashKey, T hashValue) {
        redisTemplate.opsForHash().put(key, hashKey, hashValue);
    }
    /** 写 hash, 只有 hash 中没有这个 key 才能写成功, 有了就不写, 对应命令: HSETNX key field value */
    public <T> void hashPutIfAbsent(String key, String hashKey, T hashValue) {
        redisTemplate.opsForHash().putIfAbsent(key, hashKey, hashValue);
    }
    /** 获取 hash 的长度, 对应命令: HLEN key */
    public int hashSize(String key) {
        return redisTemplate.opsForHash().size(key).intValue();
    }
    /** 获取 hash, 对应命令: HGETALL key */
    public <T> Map<String, T> hashGetAll(String key) {
        HashOperations<Object, String, T> hashOperation = redisTemplate.opsForHash();
        return hashOperation.entries(key);
    }
    /** 获取 hash 中指定 key 的值, 对应命令: HGET key field */
    public <T> T hashGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(key, hashKey);
    }
    /** 自增 hash 中指定的 key 的值, 对应命令: HINCRBY key field 1 */
    public void hashIncr(String key, String hashKey) {
        redisTemplate.opsForHash().increment(key, hashKey, 1);
    }
    /** 累加 hash 中指定的 key 的值, 对应命令: HINCRBY key field increment */
    public void hashIncr(String key, String hashKey, int incr) {
        redisTemplate.opsForHash().increment(key, hashKey, incr);
    }
    /** 从 hash 中移除指定的 key, 对应命令: HDEL key field */
    public void hashRemove(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }
}
