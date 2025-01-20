package com.github.common;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>使用 key 来获取锁
 *
 * String key = "...";
 * if (LockWithKey.tryLock(key)) {
 *     try {
 *         ...
 *     } finally {
 *         LockWithKey.unLock(key);
 *     }
 * }</pre>
 */
public class LockWithKey {

    private static final Set<String> KEY = ConcurrentHashMap.newKeySet();

    public static boolean tryLock(String key) {
        return KEY.add(key);
    }

    public static void unLock(String key) {
        KEY.remove(key);
    }
}
