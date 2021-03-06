package com.github.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>https://github.com/mongodb/mongo-java-driver/blob/master/bson/src/main/org/bson/types/ObjectId.java</p>
 *
 * <p>由以下几个部分组成</p>
 * <p>1. 生成序列号的行为. 如 1 表示订单, 2 表示提现, 3 表示退款 等</p>
 * <p>2. 时间规则的 hashcode 值</p>
 * <p>3. 自增值. 这个值基于当前进程是同步的. 基于 concurrent 下的 atomic 类实现, 避免 synchronized 锁</p>
 * <p>4. 机器码(当前机器的 mac 地址) + 进程号 合并后的 hashcode 值:</p>
 */
public final class NoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoUtil.class);

    /** 机器码 加 进程号 会导致生成的序列号很长, 基于这两个值做一些截取 */
    private static final String MP;

    private static final int HORIZONTAL_LEN = 4; // 时间 和 (mac 地址 + ip) 会比较长, 如果超出则只取尾部的固定位数
    private static final int MAX_LEN = 15; // long.max = 9223372036854775807 如果返回 long 要注意: 长度不要超出 19
    static {
        // 机器码 --> 本机 mac 地址的 hashcode 值
        int machineIdentifier = createMachineIdentifier();
        // 进程号 --> 当前运行的 jvm 进程号的 hashcode 值
        int processIdentifier = createProcessIdentifier();

        String mp = Integer.toString(Math.abs((machineIdentifier + "" + processIdentifier).hashCode()));
        MP = (mp.length() > HORIZONTAL_LEN) ? mp.substring(mp.length() - HORIZONTAL_LEN) : mp;
    }

    private static int createMachineIdentifier() {
        // build a 2-byte machine piece based on NICs info
        int machinePiece;
        try {
            StringBuilder sb = new StringBuilder();
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                sb.append(ni.toString());
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    ByteBuffer bb = ByteBuffer.wrap(mac);
                    try {
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                        sb.append(bb.getChar());
                    } catch (BufferUnderflowException ignore) { //NOPMD
                        // mac with less than 6 bytes. continue
                    }
                }
            }
            machinePiece = sb.toString().hashCode();
        } catch (Throwable t) {
            // exception sometimes happens with IBM JVM, use random
            machinePiece = new SecureRandom().nextInt();
            LOGGER.warn("Failed to get machine identifier from network interface, using random number instead", t);
        }
        return machinePiece;
    }

    // Creates the process identifier. This does not have to be unique per class loader because
    // NEXT_COUNTER will provide the uniqueness.
    private static int createProcessIdentifier() {
        int processId;
        try {
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            if (processName.contains("@")) {
                processId = Integer.parseInt(processName.substring(0, processName.indexOf('@')));
            } else {
                processId = processName.hashCode();
            }
        } catch (Throwable t) {
            processId = new SecureRandom().nextInt();
            LOGGER.warn("Failed to get process identifier from JMX, using random number instead", t);
        }
        return processId;
    }

    private static String unitCode() {
        String unit = Integer.toString(Math.abs(Long.valueOf(System.currentTimeMillis()).hashCode()));
        return unit.substring(unit.length() - HORIZONTAL_LEN);
    }

    /** 生成序列号的类型 */
    private enum Category {
        /**   标识, 初始值, 步长, 最大值(只要保证之内, 从初始化值加步长在一个周期内没有超过最大值就不会有重复) */
        Order("1",  16,    3,   10000000);

        String behavior;
        int init, step, max;
        AtomicLong counter;
        Lock lock;
        Category(String behavior, int init, int step, int max) {
            this.behavior = behavior;
            this.init = init;
            this.step = step;
            this.max = max - step;

            counter = new AtomicLong(init);
            lock = new ReentrantLock();
        }
        public String no() {
            long increment = counter.addAndGet(step);
            if (increment >= max) {
                lock.lock();
                try {
                    if (increment >= max) {
                        increment = counter.getAndSet(init);
                    }
                } finally {
                    lock.unlock();
                }
            }

            String no;
            String beginNo = unitCode() + increment + MP;
            if (beginNo.length() < MAX_LEN) {
                StringBuilder sbd = new StringBuilder();
                for (int i = 0; i < MAX_LEN - beginNo.length(); i++) {
                    sbd.append("0");
                }
                no = unitCode() + sbd.toString() + increment + MP;
            } else {
                no = beginNo;
            }
            return behavior + no;
        }
    }

    /** 生成订单号 */
    public static Long getOrderNo() {
        return U.toLong(Category.Order.no());
    }
}
