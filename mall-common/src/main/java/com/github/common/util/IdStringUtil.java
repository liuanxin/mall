package com.github.common.util;

import java.net.NetworkInterface;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

public class IdStringUtil {

    private static final int LOW_ORDER_THREE_BYTES = 0x00ffffff;

    private static final int MACHINE_IDENTIFIER;
    private static final short PROCESS_IDENTIFIER;
    private static final AtomicInteger NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());

    private static final char[] HEX_CHARS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private final int timestamp;
    private final int machineIdentifier;
    private final short processIdentifier;
    private final int counter;

    public static String get() {
        return new IdStringUtil().toString();
    }

    private IdStringUtil() {
        this(dateToTimestampSeconds(new Date()), MACHINE_IDENTIFIER, PROCESS_IDENTIFIER, NEXT_COUNTER.getAndIncrement(), false);
    }

    private IdStringUtil(final int timestamp, final int machineIdentifier, final short processIdentifier, final int counter,
                         final boolean checkCounter) {
        if ((machineIdentifier & 0xff000000) != 0) {
            throw new IllegalArgumentException("The machine identifier must be between 0 and 16777215 (it must fit in three bytes).");
        }
        if (checkCounter && ((counter & 0xff000000) != 0)) {
            throw new IllegalArgumentException("The counter must be between 0 and 16777215 (it must fit in three bytes).");
        }
        this.timestamp = timestamp;
        this.machineIdentifier = machineIdentifier;
        this.processIdentifier = processIdentifier;
        this.counter = counter & LOW_ORDER_THREE_BYTES;
    }

    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(12);
        putToByteBuffer(buffer);
        return buffer.array();  // using .allocate ensures there is a backing array that can be returned
    }

    public void putToByteBuffer(final ByteBuffer buffer) {
        buffer.put(int3(timestamp));
        buffer.put(int2(timestamp));
        buffer.put(int1(timestamp));
        buffer.put(int0(timestamp));
        buffer.put(int2(machineIdentifier));
        buffer.put(int1(machineIdentifier));
        buffer.put(int0(machineIdentifier));
        buffer.put(short1(processIdentifier));
        buffer.put(short0(processIdentifier));
        buffer.put(int2(counter));
        buffer.put(int1(counter));
        buffer.put(int0(counter));
    }

    public String toHexString() {
        char[] chars = new char[24];
        int i = 0;
        for (byte b : toByteArray()) {
            chars[i++] = HEX_CHARS[b >> 4 & 0xF];
            chars[i++] = HEX_CHARS[b & 0xF];
        }
        return new String(chars);
    }

    @Override
    public String toString() {
        return toHexString();
    }

    static {
        try {
            MACHINE_IDENTIFIER = createMachineIdentifier();
            PROCESS_IDENTIFIER = createProcessIdentifier();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                    } catch (BufferUnderflowException shortHardwareAddressException) { //NOPMD
                        // mac with less than 6 bytes. continue
                    }
                }
            }
            machinePiece = sb.toString().hashCode();
        } catch (Throwable t) {
            // exception sometimes happens with IBM JVM, use random
            machinePiece = (new SecureRandom().nextInt());
        }
        machinePiece = machinePiece & LOW_ORDER_THREE_BYTES;
        return machinePiece;
    }

    // Creates the process identifier.  This does not have to be unique per class loader because
    // NEXT_COUNTER will provide the uniqueness.
    private static short createProcessIdentifier() {
        short processId;
        try {
            String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            if (processName.contains("@")) {
                processId = (short) Integer.parseInt(processName.substring(0, processName.indexOf('@')));
            } else {
                processId = (short) java.lang.management.ManagementFactory.getRuntimeMXBean().getName().hashCode();
            }

        } catch (Throwable t) {
            processId = (short) new SecureRandom().nextInt();
        }

        return processId;
    }

    private static int dateToTimestampSeconds(final Date time) {
        return (int) (time.getTime() / 1000);
    }

    private static byte int3(final int x) {
        return (byte) (x >> 24);
    }

    private static byte int2(final int x) {
        return (byte) (x >> 16);
    }

    private static byte int1(final int x) {
        return (byte) (x >> 8);
    }

    private static byte int0(final int x) {
        return (byte) (x);
    }

    private static byte short1(final short x) {
        return (byte) (x >> 8);
    }

    private static byte short0(final short x) {
        return (byte) (x);
    }
}
