package com.github.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileTypeUtil {

    /** 魔数只读前 3 位 */
    private static final int MAGIC_SIZE = 3;

    public static boolean isImage(File file) {
        /* try {
            BufferedImage image = ImageIO.read(file);
            int width = image.getWidth();
            int height = image.getHeight();
            return width > 0 && height > 0;
        } catch (Exception e) {
            return false;
        } */
        return FileMagicNumber.hasImage(getFileMagicNumber(file));
    }
    public static boolean isImage(byte[] bytes) {
        return FileMagicNumber.hasImage(getByteMagicNumber(bytes));
    }
    /**
     * <pre>
     * try (InputStream input = ...) {
     *     if (isImage(input)) {
     *         // do something
     *     }
     * }
     * </pre>
     */
    public static boolean isImage(InputStream input) {
        return FileMagicNumber.hasImage(getStreamMagicNumber(input));
    }

    public static boolean isOffice(File file) {
        return FileMagicNumber.hasOffice(getFileMagicNumber(file));
    }
    public static boolean isOffice(byte[] bytes) {
        return FileMagicNumber.hasOffice(getByteMagicNumber(bytes));
    }
    /**
     * <pre>
     * try (InputStream input = ...) {
     *     if (isOffice(input)) {
     *         // do something
     *     }
     * }
     * </pre>
     */
    public static boolean isOffice(InputStream input) {
        return FileMagicNumber.hasOffice(getStreamMagicNumber(input));
    }

    public static boolean isPdf(File file) {
        return FileMagicNumber.hasPdf(getFileMagicNumber(file));
    }
    public static boolean isPdf(byte[] bytes) {
        return FileMagicNumber.hasPdf(getByteMagicNumber(bytes));
    }
    /**
     * <pre>
     * try (InputStream input = ...) {
     *     if (isPdf(input)) {
     *         // do something
     *     }
     * }
     * </pre>
     */
    public static boolean isPdf(InputStream input) {
        return FileMagicNumber.hasPdf(getStreamMagicNumber(input));
    }


    private static String getFileMagicNumber(File file) {
        try (InputStream input = new FileInputStream(file)) {
            byte[] b = new byte[MAGIC_SIZE];
            // noinspection ResultOfMethodCallIgnored
            input.read(b, 0, MAGIC_SIZE);
            return toHex(b);
        } catch (Exception e) {
            return null;
        }
    }
    private static String getByteMagicNumber(byte[] bytes) {
        if (bytes != null) {
            byte[] b = new byte[MAGIC_SIZE];
            System.arraycopy(bytes, 0, b, 0, Math.min(bytes.length, MAGIC_SIZE));
            return toHex(b);
        } else {
            return null;
        }
    }
    private static String getStreamMagicNumber(InputStream input) {
        try {
            byte[] b = new byte[MAGIC_SIZE];
            // noinspection ResultOfMethodCallIgnored
            input.read(b, 0, MAGIC_SIZE);
            return toHex(b);
        } catch (Exception e) {
            return null;
        }
    }
    private static String toHex(byte[] bytes) {
        if (bytes != null) {
            StringBuilder sbd = new StringBuilder();
            for (byte b : bytes) {
                sbd.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sbd.toString();
        } else {
            return null;
        }
    }
}
