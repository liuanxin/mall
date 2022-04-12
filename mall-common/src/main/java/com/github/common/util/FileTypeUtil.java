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

    public static boolean isOffice(byte[] bytes) {
        return FileMagicNumber.hasOffice(getByteMagicNumber(bytes));
    }

    public static boolean isPdf(byte[] bytes) {
        return FileMagicNumber.hasPdf(getByteMagicNumber(bytes));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String getFileMagicNumber(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] b = new byte[MAGIC_SIZE];
            inputStream.read(b, 0, MAGIC_SIZE);
            return toHex(b);
        } catch (Exception e) {
            return null;
        }
    }
    public static String getByteMagicNumber(byte[] bytes) {
        if (bytes != null) {
            byte[] b = new byte[MAGIC_SIZE];
            System.arraycopy(bytes, 0, b, 0, Math.min(bytes.length, MAGIC_SIZE));
            return toHex(b);
        } else {
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
