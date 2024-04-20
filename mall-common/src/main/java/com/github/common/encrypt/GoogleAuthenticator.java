package com.github.common.encrypt;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** google 动态令牌 */
@SuppressWarnings("DuplicatedCode")
final class GoogleAuthenticator {

    /**
     * @param secret       密钥
     * @param ms           时间戳
     * @param offsetSecond 偏移时间(-29 到 29 之间), 正数则往前偏移指定秒, 负数则往后偏移指定秒
     * @return 生成的 google 验证码(6 位数字), 每过 30 秒变化
     */
    public static String getCode(String secret, long ms, int offsetSecond) {
        int offset = (offsetSecond < -29 || offsetSecond > 29) ? 0 : offsetSecond;
        byte[] data = sha1(secret, (ms + offset) / 30000);
        int o = data[19] & 0xf;
        int number = hashToInt(data, o) & 0x7fffffff;
        return output(String.valueOf(number % 1000000));
    }

    private static byte[] sha1(String secret, long msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(Base32String.decode(secret), ""));
            return mac.doFinal(ByteBuffer.allocate(8).putLong(msg).array());
        } catch (Exception e) {
            return new byte[20];
        }
    }

    private static int hashToInt(byte[] bytes, int start) {
        try {
            return new DataInputStream(new ByteArrayInputStream(bytes, start, bytes.length - start)).readInt();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String output(String str) {
        return (str.length() < 6) ? output("0" + str) : str;
    }
}
