package com.github.common.encrypt;

import com.github.common.util.U;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/** google 动态令牌 */
final class GoogleAuthenticator {

    /**
     * @param secret 密钥. 由 base32 字母表组成: 字母 a-z 及数字 2-7(字母不区分大小写, 26 个字母 + 6 个数字, 共 32 位), 数字 0 1 8 被跳过(它们与字母 O I B 相似)
     * @param handleSecretError true: 如果 secret 中包含非 base32 的字符则去除, 否则将会抛出异常
     * @param ms 时间戳
     * @param offsetSecond 偏移时间(-29 到 29 之间), 正数则往前偏移指定秒, 负数则往后偏移指定秒
     * @return 生成的 google 验证码(6 位数字), 每过 30 秒变化
     */
    static String getCode(String secret, boolean handleSecretError, long ms, int offsetSecond) {
        if (U.isBlank(secret)) {
            return "000000";
        }

        StringBuilder sbd = new StringBuilder();
        for (char c : secret.toCharArray()) {
            // base32 字母表: 字母 a-z 及数字 2-7(字母不区分大小写, 26 个字母 + 6 个数字, 共 32 位), 数字 0 1 8 被跳过(它们与字母 O I B 相似)
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '2' && c <= '7')) {
                sbd.append(c);
            } else {
                if (!handleSecretError) {
                    throw new IllegalArgumentException("密钥只能由字母 a-z 及数字 2-7 组成(字母不区分大小写)");
                }
            }
        }
        String s = sbd.toString();
        // 密钥为空或只有一位时, 执行后面的结果也是 000000, 在这里直接就返回了
        if (s.length() <= 1) {
            return "000000";
        }

        int offset = (offsetSecond < -29 || offsetSecond > 29) ? 0 : offsetSecond;
        byte[] data = sha1(s, (ms + offset) / 30000);
        int o = data[19] & 0xf;
        int number = hashToInt(data, o) & 0x7fffffff;
        return output(String.valueOf(number % 1000000));
    }

    private static byte[] sha1(String secret, long msg) {
        try {
            byte[] d = Base32String.decode(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(d, ""));
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
        int l = str.length();
        if (l > 6) {
            return str;
        }

        StringBuilder sbd = new StringBuilder();
        int loop = 6 - l;
        for (int i = 0; i < loop; i++) {
            sbd.append("0");
        }
        sbd.append(str);
        return sbd.toString();
    }
}
