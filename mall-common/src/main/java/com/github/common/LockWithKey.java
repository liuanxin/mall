package com.github.common;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class LockWithKey {

    private static final Set<String> KEY = ConcurrentHashMap.newKeySet();

    public static boolean tryLock(String key) {
        return KEY.add(key);
    }

    public static void unLock(String key) {
        KEY.remove(key);
    }
}
