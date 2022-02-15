package com.github.common.encrypt;

import com.github.common.util.A;
import com.github.common.util.U;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EncryptTest {

    private static final String SOURCE = "password--$%^&*()我中文easy~_+-321 123=/.,";

    @Test
    public void aesCheck() {
        String encode = Encrypt.aesEncode(SOURCE);
        System.out.println(encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.aesDecode(encode);
        System.out.println(decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void desCheck() {
        String key = "12345678";
        String abc = Encrypt.desEncode("abc", key);
        System.out.println(abc);
        String dec = Encrypt.desDecode(abc, key);
        System.out.println(dec);

        String encode = Encrypt.desEncode(SOURCE);
        System.out.println("des: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.desDecode(encode);
        System.out.println("des: " + decode);
        Assert.assertEquals(SOURCE, decode);

        encode = Encrypt.desCbcEncode(SOURCE);
        System.out.println("des/cbc: " + encode);
        Assert.assertTrue(encode.length() > 0);

        decode = Encrypt.desCbcDecode(encode);
        System.out.println("des/cbc: " + decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void rsaCheck() {
        int size = 1024;
        Encrypt.RsaPair pair = Encrypt.genericRsaKeyPair(size);
        String publicKey = pair.getPublicKey();
        String privateKey = pair.getPrivateKey();
        System.out.println("密码长度是 " + size + " 时的公钥:\n" + publicKey + "\n");
        System.out.println("密码长度是 " + size + " 时的私钥:\n" + privateKey + "\n");

        String encode = Encrypt.rsaEncode(publicKey, SOURCE);
        System.out.println("密码长度是 " + size + " 时拿公钥加密后的值是:\n" + encode + "\n");
        try {
            System.out.println("密码长度是 " + size + " 时拿公钥解密后的值是: " + Encrypt.rsaDecode(publicKey, encode));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String rsa = Encrypt.rsaDecode(privateKey, encode);
        System.out.println("密码长度是 " + size + " 时拿私钥解密后的值是: " + rsa);
        System.out.println("密码长度是 " + size + " 时解码是否一致: " + rsa.equals(SOURCE));

        System.out.println("\n\n");

        size = 2048;
        pair = Encrypt.genericRsaKeyPair(size);
        publicKey = pair.getPublicKey();
        privateKey = pair.getPrivateKey();
        System.out.println("密码长度是 " + size + " 时的公钥:\n" + publicKey + "\n");
        System.out.println("密码长度是 " + size + " 时的私钥:\n" + privateKey + "\n");

        encode = Encrypt.rsaEncode(publicKey, SOURCE);
        System.out.println("密码长度是 " + size + " 时拿公钥加密后的值是:\n" + encode + "\n");
        try {
            System.out.println("密码长度是 " + size + " 时拿公钥解密后的值是: " + Encrypt.rsaDecode(publicKey, encode));
        } catch (Exception e) {
            e.printStackTrace();
        }
        rsa = Encrypt.rsaDecode(privateKey, encode);
        System.out.println("密码长度是 " + size + " 时拿私钥解密后的值是: " + rsa);
        System.out.println("密码长度是 " + size + " 时解码是否一致: " + rsa.equals(SOURCE));
    }

    @Test
    public void jwtCheck() {
        String encode = Encrypt.jwtEncode(A.maps(
                "id", 123,
                "name", System.currentTimeMillis()
        ));
        System.out.println(encode);
        Assertions.assertTrue(encode.length() > 0);

        Map<String, Object> decode = Encrypt.jwtDecode(encode);
        Assertions.assertEquals(123, decode.get("id"));
        Assertions.assertTrue(System.currentTimeMillis() > U.toLong(decode.get("name").toString()));


        encode = Encrypt.jwtEncode(A.maps("id", 123), 2L, TimeUnit.SECONDS);
        Assertions.assertTrue(encode.length() > 0);

        decode = Encrypt.jwtDecode(encode);
        Assertions.assertEquals(123, decode.get("id"));


        encode = Encrypt.jwtEncode(A.maps("id", 123), 10L, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(encode.length() > 0);
        try {
            Thread.sleep(11L);
            Encrypt.jwtDecode(encode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void base64Test() {
        String encode = Encrypt.base64Encode(SOURCE);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.base64Decode(encode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void bcryptTest() {
        String encode = Encrypt.bcryptEncode(SOURCE);
        Assert.assertTrue(encode.length() > 0);

        String encode2 = Encrypt.bcryptEncode(SOURCE);
        // 两次密码的值不同
        Assert.assertNotEquals(encode, encode2);

        // 加一个空格, 密码就不同了
        Assert.assertTrue(Encrypt.checkNotBcrypt(SOURCE + " ", encode));

        Assert.assertTrue(Encrypt.checkBcrypt(SOURCE, encode));
        Assert.assertTrue(Encrypt.checkBcrypt(SOURCE, encode2));
    }

    @Test
    public void rc4Test() {
        String encode = Encrypt.rc4Encode(SOURCE);
        System.out.println(encode);

        String decode = Encrypt.rc4Decode(encode);
        System.out.println(decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void digestTest() {
        String encode = Encrypt.to16Md5(SOURCE);
        Assert.assertEquals(16, encode.length());

        encode = Encrypt.toMd5(SOURCE);
        Assert.assertEquals(32, encode.length());

        encode = Encrypt.toSha1(SOURCE);
        Assert.assertEquals(40, encode.length());

        encode = Encrypt.toSha224(SOURCE);
        Assert.assertEquals(56, encode.length());

        encode = Encrypt.toSha256(SOURCE);
        Assert.assertEquals(64, encode.length());

        encode = Encrypt.toSha384(SOURCE);
        Assert.assertEquals(96, encode.length());

        encode = Encrypt.toSha512(SOURCE);
        Assert.assertEquals(128, encode.length());
    }

    @Test
    public void hmacTest() {
        String k = "192006250b4c09247ec02edce69f6a2d";
        String p = "appid=wxd930ea5d5a258f4f&body=test&device_info=1000&mch_id=10000100&nonce_str=ibuaiVcKdpRxkhJA";
        String str = p + "&key=" + k;

        String encode = Encrypt.toHmacMd5(str, k);
        Assert.assertEquals(32, encode.length());

        encode = Encrypt.toHmacSha1(str, k);
        Assert.assertEquals(40, encode.length());

        encode = Encrypt.toHmacSha224(str, k);
        Assert.assertEquals(56, encode.length());

        encode = Encrypt.toHmacSha256(str, k);
        Assert.assertEquals(64, encode.length());

        encode = Encrypt.toHmacSha384(str, k);
        Assert.assertEquals(96, encode.length());

        encode = Encrypt.toHmacSha512(str, k);
        Assert.assertEquals(128, encode.length());
    }
}
