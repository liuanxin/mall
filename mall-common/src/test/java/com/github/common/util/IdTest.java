package com.github.common.util;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class IdTest {

    private static final int COUNT = 100000;
    private static final ExecutorService THREAD_POOL = new ThreadPoolExecutor(
            U.PROCESSORS,
            U.PROCESSORS + 1,
            5L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Test
    public void generateOrderNo() throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("start : " + FORMATTER.format(LocalDateTime.ofInstant(new Date(start).toInstant(), ZONE_ID)));

        List<Future<Long>> futureList = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) {
            futureList.add(THREAD_POOL.submit(IdUtil::getId));
        }

        Set<Long> set = ConcurrentHashMap.newKeySet();
        for (Future<Long> future : futureList) {
            set.add(future.get());
        }
        long end = System.currentTimeMillis();
        System.out.println("time  : " + (end - start) + "ms");
        System.out.println("end   : " + FORMATTER.format(LocalDateTime.ofInstant(new Date(end).toInstant(), ZONE_ID)));
        System.out.println("------------------");
        System.out.println("all   : " + COUNT);
        System.out.println("real  : " + set.size());

        for (int i = 0; i < 20; i++) {
            System.out.println(IdUtil.getId());
        }
    }
}
