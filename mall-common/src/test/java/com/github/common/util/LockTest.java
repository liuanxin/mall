package com.github.common.util;

import com.github.common.LockWithKey;
import com.github.common.date.Dates;

public class LockTest {

    public static void main(String[] args) throws Exception {
        String key = "abc";
        Thread t1 = new Thread(() -> {
            System.out.println(Dates.nowDateTimeMs() + " : " + "start: " + Thread.currentThread());
            if (LockWithKey.tryLock(key)) {
                try {
                    System.out.println(Dates.nowDateTimeMs() + " : " + Thread.currentThread() + " has lock");
                    Thread.sleep(2000L);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    LockWithKey.unLock(key);
                }
            } else {
                System.out.println(Dates.nowDateTimeMs() + " : " + Thread.currentThread() + " no lock");
            }
            System.out.println(Dates.nowDateTimeMs() + " : " + "end: " + Thread.currentThread());
        });

        Thread t2 = new Thread(() -> {
            System.out.println(Dates.nowDateTimeMs() + " : " + "start: " + Thread.currentThread());
            if (LockWithKey.tryLock(key)) {
                try {
                    System.out.println(Dates.nowDateTimeMs() + " : " + Thread.currentThread() + " has lock");
                    Thread.sleep(2000L);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    LockWithKey.unLock(key);
                }
            } else {
                System.out.println(Dates.nowDateTimeMs() + " : " + Thread.currentThread() + " no lock");
            }
            System.out.println(Dates.nowDateTimeMs() + " : " + "end: " + Thread.currentThread());
        });

        System.out.println(Dates.nowDateTimeMs() + " : " + "start");
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(Dates.nowDateTimeMs() + " : " + "end");
        /*
        2024-12-18 14:53:32.979 : start
        2024-12-18 14:53:33.022 : start: Thread[Thread-1,5,main]
        2024-12-18 14:53:33.029 : Thread[Thread-1,5,main] has lock
        2024-12-18 14:53:33.022 : start: Thread[Thread-0,5,main]
        2024-12-18 14:53:33.029 : Thread[Thread-0,5,main] no lock
        2024-12-18 14:53:33.030 : end: Thread[Thread-0,5,main]
        2024-12-18 14:53:35.029 : end: Thread[Thread-1,5,main]
        2024-12-18 14:53:35.030 : end
        */

        /*
        System.out.println(DateUtil.nowDateTimeMs() + " 1: " + Thread.currentThread());
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = executor.schedule(() -> {
            System.out.println(DateUtil.nowDateTimeMs() + " 2: " + Thread.currentThread());
        }, 2000, TimeUnit.MILLISECONDS);
        System.out.println(DateUtil.nowDateTimeMs() + " 3: " + Thread.currentThread());
        Object o = future.get();
        System.out.println(DateUtil.nowDateTimeMs() + " 4: " + Thread.currentThread() + " : " + o);
        executor.shutdownNow();
        System.out.println(DateUtil.nowDateTimeMs() + " 5: " + Thread.currentThread() + " : " + o);
        */
    }
}
