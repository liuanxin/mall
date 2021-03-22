package com.github.global.service;

import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@AllArgsConstructor
@Configuration
@ConditionalOnClass({ RedisTemplate.class })
public class CacheService {

    /** @see org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration */
    private final RedisTemplate<Object, Object> redisTemplate;


    /** 往 redis 中放值 */
    public <T> void set(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }
    /** 往 redis 放值, 并设定超时时间 */
    public <T> void set(String key, T value, long time, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, time, unit);
    }
    /** 往 redis 放值, 并设定在什么时间超时 */
    public <T> void set(String key, T value, Date expireTime) {
        if (expireTime != null) {
            Date now = new Date();
            if (expireTime.after(now)) {
                set(key, value, now.getTime() - expireTime.getTime(), TimeUnit.MILLISECONDS);
            }
        }
    }

    public void expire(String key, long time, TimeUnit timeUnit) {
        redisTemplate.expire(key, time, timeUnit);
    }

    public long incr(String key) {
        Long inc = redisTemplate.opsForValue().increment(key, 1L);
        return inc == null ? 0 : inc;
    }

    /** 从 redis 中取值 */
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }
    /** 从 redis 中删值 */
    public void delete(String key) {
        redisTemplate.delete(key);
    }


    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * if (tryLock(key, value)) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
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
     * int time = 3;
     * if (tryLock(key, value, 10, TimeUnit.SECONDS)) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * } else {
     *   log.info("重试 {} 次依然没有获取到锁", time);
     * }
     * </pre>
     *
     * @param key 键
     * @param value 值
     * @param time 锁的超时时间
     * @param unit 锁超时的时间单位
     * @return 返回 true 则表示获取到了锁
     */
    public boolean tryLock(String key, String value, long time, TimeUnit unit) {
        String script = "if redis.call('set', KEYS[1], KEYS[2], 'PX', KEYS[3], 'NX') then return 1 else return 0 end";
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        List<Object> keys = Arrays.asList(key, value, unit.toMillis(time));
        Long flag = redisTemplate.execute(redisScript, keys);
        return flag != null && flag == 1L;
    }
    /**
     * <pre>
     * 用 redis 获取分布式锁, 获取成功则返回 true
     *
     * String key = "xxx", value = uuid(); // value 用 uuid 确保每个线程都不一样
     * if (tryLock(key, value)) {
     *   try {
     *     // 获取到锁之后的业务处理
     *   } finally {
     *     // 解锁时 key 和 value 都需要
     *     unlock(key, value);
     *   }
     * } else {
     *   log.info("未获取到锁", time);
     * }
     * </pre>
     * @param key 键
     * @param value 值
     */
    public void unlock(String key, String value) {
        // 释放锁的时候先去缓存中取, 如果值跟之前存进去的一样才进行删除操作, 避免当前线程执行太长, 超时后其他线程又设置了值在处理
        String script = "if redis.call('get', KEYS[1]) == KEYS[2] then redis.call('del', KEYS[1]); end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(key, value));
    }


    // list: 左进左出则可以构建栈(右进右出也是), 左进右出则可以构建队列(右进左出也是)

    /** 向 list 写值(从左边入): lpush */
    public <T> void leftPush(String key, T value) {
        redisTemplate.opsForList().leftPush(key, value);
    }
    /** 向 list 写值(从右边入): rpush */
    public <T> void rightPush(String key, T value) {
        redisTemplate.opsForList().rightPush(key, value);
    }
    /** 向 list 读值(从左边出): lpop */
    public <T> T leftPop(String key) {
        return (T) redisTemplate.opsForList().leftPop(key);
    }
    /** 向 list 读值(从左边出, 并阻塞指定的时间) */
    public <T> T leftPop(String key, int time, TimeUnit unit) {
        return (T) redisTemplate.opsForList().leftPop(key, time, unit);
    }
    /** 向 list 读值(从右边出): rpop */
    public <T> T rightPop(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }
    /** 向 list 读值(从右边出, 并阻塞指定的时间) */
    public <T> T rightPop(String key, int time, TimeUnit unit) {
        return (T) redisTemplate.opsForList().rightPop(key, time, unit);
    }


    // set 的特点是每个元素唯一, 但是不保证排序

    /** 获取指定 set 的长度: scard key */
    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0L;
    }
    /** 将指定的 set 存进 redis 并返回成功条数: sadd key v1 v2 v3 ... */
    public <T> long setAdd(String key, T[] set) {
        Long add = redisTemplate.opsForSet().add(key, set);
        return add != null ? add : 0L;
    }
    /** 获取 set: smembers key */
    public <T> Set<T> setGet(String key) {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }
    /** 从 set 中移除一个值: srem key member */
    public <T> void setRemove(String key, T value) {
        redisTemplate.opsForSet().remove(key, value);
    }
    /** 从指定的 set 中随机取出一个值: spop key */
    public <T> T setPop(String key) {
        return (T) redisTemplate.opsForSet().pop(key);
    }
    /** 从指定的 set 中随机取出一些值: spop key count */
    public <T> T setPop(String key, long count) {
        return (T) redisTemplate.opsForSet().pop(key, count);
    }


    // hash 键值对

    /** 写 hash: hmset key field value [field value ...] */
    public <T> void hashPutAll(String key, Map<String, T> hashMap) {
        redisTemplate.opsForHash().putAll(key, hashMap);
    }
    /** 写一个 hash: hset key field value */
    public <T> void hashPut(String key, String hashKey, T hashValue) {
        redisTemplate.opsForHash().put(key, hashKey, hashValue);
    }
    /** 写一个 hash, 只有 hash 中没有这个 key 才能写成功, 有了就不写: hsetnx key field value */
    public <T> void hashPutIfAbsent(String key, String hashKey, T hashValue) {
        redisTemplate.opsForHash().putIfAbsent(key, hashKey, hashValue);
    }
    /** 获取一个 hash 的长度: hlen key */
    public long hashSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }
    /** 获取一个 hash 的值: hgetall key */
    public <T> Map<String, T> hashGetAll(String key) {
        HashOperations<Object, String, T> hashOperation = redisTemplate.opsForHash();
        return hashOperation.entries(key);
    }
    /** 获取一个 hash 中指定 key 的值: hget key field */
    public <T> T hashGet(String key, String hashKey) {
        return (T) redisTemplate.opsForHash().get(key, hashKey);
    }
    /** 给 hash 中指定的 key 的值累加 1: hincrby key field 1 */
    public void hashIncr(String key, String hashKey) {
        redisTemplate.opsForHash().increment(key, hashKey, 1);
    }
    /** 从 hash 中移除指定的 key: hdel key field */
    public void hashRemove(String key, String hashKey) {
        redisTemplate.opsForHash().delete(key, hashKey);
    }
}
