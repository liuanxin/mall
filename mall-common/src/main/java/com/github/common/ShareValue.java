package com.github.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * 跟 ThreadLocal 一样, 在同一线程不同的调用处(比如一个请求的 web service 等)共享值, 如下面的示例
 *
 * public class Xx {
 *
 *     // 其独立于所有的线程, 可以用 static final 来修饰
 *     private static final ShareValue&lt;String&gt; SHARE_STRING = new ShareValue&lt;&gt;;
 *     // 要共享多个独立数据就申明多个
 *     // private static final ShareValue&lt;XXX&gt; SHARE_XXX = new ShareValue&lt;&gt;;
 *
 *     // 在 try 中 put 值, 在 finally 中 remove
 *     public void xxx() {
 *         try {
 *             SHARE_STRING.put("xxxx");
 *             // ... do something ...
 *         } finally {
 *             SHARE_STRING.remove();
 *         }
 *     }
 * }
 * </pre>
 */
public class ShareValue<T> {

    private final Map<Thread, T> shareMap = new ConcurrentHashMap<>();

    public void put(T value) {
        shareMap.put(Thread.currentThread(), value);
    }
    public void remove() {
        shareMap.remove(Thread.currentThread());
    }
    public T get() {
        return shareMap.get(Thread.currentThread());
    }
}
