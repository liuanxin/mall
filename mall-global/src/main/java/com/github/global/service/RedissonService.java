package com.github.global.service;

import lombok.AllArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
@AllArgsConstructor
@Configuration
@ConditionalOnClass({ RedissonClient.class })
public class RedissonService {

    // https://github.com/redisson/redisson/wiki/11.-Redis-commands-mapping

    private static final Codec USE_CODEC = StringCodec.INSTANCE;

    /** @see org.redisson.spring.starter.RedissonAutoConfiguration */
    private final RedissonClient redisson;


    /** 往 redis 中放值 */
    public <T> void set(String key, T value) {
        redisson.getBucket(key, USE_CODEC).set(value);
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
    /** 往 redis 放值, 并设定超时时间 */
    public <T> void set(String key, T value, long time, TimeUnit unit) {
        redisson.getBucket(key, USE_CODEC).set(value, time, unit);
    }

    public void expire(String key, long time, TimeUnit unit) {
        redisson.getBucket(key, USE_CODEC).expire(time, unit);
    }

    public long incr(String key) {
        return redisson.getAtomicLong(key).incrementAndGet();
    }

    /** 从 redis 中取值 */
    public <T> T get(String key) {
        return (T) redisson.getBucket(key, USE_CODEC).get();
    }
    /** 从 redis 中删值 */
    public void delete(String key) {
        redisson.getBucket(key, USE_CODEC).delete();
    }

    /** 获取键的存活时间, 单位: 毫秒 */
    public long getExpireMs(String key) {
        return redisson.getBucket(key, USE_CODEC).remainTimeToLive();
    }


    /**
     * 用 redis 获取分布式锁
     * <pre>
     *
     * String key = "xxx";
     *
     * lock(key);
     * try {
     *   // 获取到锁之后的处理
     * } finally {
     *   unlock(key);
     * }
     * </pre>
     * @param key 键
     */
    public void lock(String key) {
        redisson.getLock(key).lock();
    }

    /**
     * 用 redis 获取分布式锁, 并设置锁的占用时间
     * <pre>
     *
     * String key = "xxx";
     * int lockTime = 10;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * lock(key, lockTime, unit);
     * try {
     *   // 获取到锁之后的处理, 此锁会保持 10 秒
     * } finally {
     *   unlock(key);
     * }
     * </pre>
     * @param key 键
     */
    public void lock(String key, int lockTime, TimeUnit unit) {
        redisson.getLock(key).lock(lockTime, unit);
    }

    /**
     * 用 redis 获取分布式锁, 获取成功则返回 true
     * <pre>
     *
     * String key = "xxx";
     * if (tryLock(key)) {
     *   try {
     *     // 获取到锁之后的处理
     *   } finally {
     *     unlock(key);
     *   }
     * } else {
     *   log.info("未获取到锁");
     * }
     * </pre>
     * @param key 键
     */
    public boolean tryLock(String key) {
        return redisson.getLock(key).tryLock();
    }

    /**
     * 用 redis 获取分布式锁, 获取成功则返回 true, 获取到的锁将会在设定的时间后失效
     * <pre>
     *
     * String key = "xxx";
     * int waitTime = 5;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * try {
     *   if (tryLock(key, waitTime, unit)) {
     *     try {
     *       // 在 5 秒之内获取到了锁
     *     } finally {
     *       unlock(key);
     *     }
     *   } else {
     *     log.info("未获取到锁");
     *   }
     * } catch (InterruptedException e) {
     *   log.info("获取锁时线程被中断了");
     * }
     * </pre>
     * @param key 键
     */
    public boolean tryLock(String key, int waitTime, TimeUnit unit) throws InterruptedException {
        return redisson.getLock(key).tryLock(waitTime, unit);
    }

    /**
     * 用 redis 获取分布式锁, 并指定获取锁的时间, 获取到锁后设置锁的占用时间
     * <pre>
     *
     * String key = "xxx";
     * int waitTime = 5;
     * int lockTime = 10;
     * TimeUnit unit = TimeUnit.SECONDS;
     *
     * try {
     *   if (tryLock(key, getTime, waitTime, unit)) {
     *     try {
     *       // 在 5 秒之内获取到了锁, 此锁会保持 10 秒
     *     } finally {
     *       unlock(key);
     *     }
     *   } else {
     *     log.info("未获取到锁");
     *   }
     * } catch (InterruptedException e) {
     *   log.info("获取锁时线程被中断了");
     * }
     * </pre>
     * @param key 键
     */
    public boolean tryLock(String key, int waitTime, int lockTime, TimeUnit unit) throws InterruptedException {
        return redisson.getLock(key).tryLock(waitTime, lockTime, unit);
    }

    /** 释放 redis 锁 */
    public void unlock(String key) {
        redisson.getLock(key).unlock();
    }


    // list: 左进左出则可以构建栈(右进右出也是), 左进右出则可以构建队列(右进左出也是)

    /** 向 list 写值(从左边入): lpush */
    public <T> void leftPush(String key, T value) {
        redisson.getDeque(key, USE_CODEC).push(value);
    }
    /** 向 list 写值(从右边入): rpush */
    public <T> void rightPush(String key, T value) {
        redisson.getDeque(key, USE_CODEC).add(value);
    }
    /** 向 list 取值(从左边出): lpop */
    public <T> T leftPop(String key) {
        return (T) redisson.getDeque(key, USE_CODEC).pollFirst();
    }
    /** 向 list 取值(从左边出, 并阻塞指定的时间): blpop */
    public <T> T leftPop(String key, int time, TimeUnit unit) throws InterruptedException {
        return (T) redisson.getBlockingDeque(key, USE_CODEC).pollFirst(time, unit);
    }
    /** 向 list 取值(从右边出): rpop */
    public <T> T rightPop(String key) {
        return (T) redisson.getDeque(key, USE_CODEC).pollLast();
    }
    /** 向 list 取值(从右边出, 并阻塞指定的时间): brpop */
    public <T> T rightPop(String key, int time, TimeUnit unit) throws InterruptedException {
        return (T) redisson.getBlockingDeque(key, USE_CODEC).pollLast(time, unit);
    }


    // set 的特点是每个元素唯一, 但是不保证排序

    /** 获取指定 set 的长度: scard key */
    public long setSize(String key) {
        return redisson.getSet(key, USE_CODEC).size();
    }
    /** 将指定的 set 存进 redis 并返回成功条数: sadd key v1 v2 v3 ... */
    public <T> boolean setAdd(String key, Collection<T> set) {
        // return add != null ? add : 0L;
        return redisson.getSet(key, USE_CODEC).addAll(set);
    }
    /** 获取 set: smembers key */
    public <T> Set<T> setGet(String key) {
        return redisson.getSet(key, USE_CODEC);
    }
    /** 从 set 中移除一个值: srem key member */
    public <T> void setRemove(String key, T value) {
        redisson.getSet(key, USE_CODEC).remove(value);
    }
    /** 从指定的 set 中随机取出一个值: spop key */
    public <T> T setPop(String key) {
        return (T) redisson.getSet(key, USE_CODEC).removeRandom();
    }
    /** 从指定的 set 中随机取出一些值: spop key count */
    public <T> Set<T> setPop(String key, int count) {
        return (Set<T>) redisson.getSet(key, USE_CODEC).removeRandom(count);
    }


    // hash 键值对

    /** 写 hash: hmset key field value [field value ...] */
    public <T> void hashPutAll(String key, Map<String, T> hashMap) {
        redisson.getMap(key, USE_CODEC).putAll(hashMap);
    }
    /** 写一个 hash: hset key field value */
    public <T> void hashPut(String key, String hashKey, T hashValue) {
        redisson.getMap(key, USE_CODEC).put(hashKey, hashValue);
    }
    /** 写一个 hash, 只有 hash 中没有这个 key 才能写成功, 有了就不写: hsetnx key field value */
    public <T> void hashPutIfAbsent(String key, String hashKey, T hashValue) {
        redisson.getMap(key, USE_CODEC).putIfAbsent(hashKey, hashValue);
    }
    /** 获取一个 hash 的长度: hlen key */
    public int hashSize(String key) {
        return redisson.getMap(key, USE_CODEC).size();
    }
    /** 获取一个 hash 的值: hgetall key */
    public <T> Map<String, T> hashGetAll(String key) {
        RMap<String, T> map = redisson.getMap(key, USE_CODEC);
        return map.readAllMap();
    }
    /** 获取一个 hash 中指定 key 的值: hget key field */
    public <T> T hashGet(String key, String hashKey) {
        return (T) redisson.getMap(key, USE_CODEC).get(hashKey);
    }
    /** 给 hash 中指定的 key 的值累加 1: hincrby key field 1 */
    public void hashIncr(String key, String hashKey) {
        redisson.getMap(key, USE_CODEC).addAndGet(hashKey, 1);
    }
    /** 从 hash 中移除指定的 key: hdel key field */
    public void hashRemove(String key, String hashKey) {
        redisson.getMap(key, USE_CODEC).fastRemove(hashKey);
    }
}
