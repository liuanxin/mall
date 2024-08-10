package com.github.common.encrypt;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EncryptTest {

    private static final String SOURCE = "password--$%^&*()我中文easy~_+-321 123=/.,";

    @Test
    public void aesCheck() {
        String secret = U.uuid();

        String encode = Encrypt.aesEncode(SOURCE, secret);
        System.out.println(encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.aesDecode(encode, secret);
        System.out.println(decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void desCheck() {
        String secret = U.uuid();

        String key = "12345678";
        String abc = Encrypt.desEncode("abc", key);
        System.out.println(abc);
        String dec = Encrypt.desDecode(abc, key);
        System.out.println(dec);

        String encode = Encrypt.desEncode(SOURCE, secret);
        System.out.println("des: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.desDecode(encode, secret);
        System.out.println("des: " + decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void desCbc() {
        String secret = U.uuid().substring(0, 8);
        String encode = Encrypt.desCbcEncode(SOURCE, secret);
        System.out.println("des/cbc: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.desCbcDecode(encode, secret);
        System.out.println("des/cbc: " + decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void rsaCheck() {
        for (Integer size : Arrays.asList(512, 1024, 2048)) {
            KeyPair pair = Encrypt.genericRsaKeyPair(size);
            String publicKey = Encrypt.rsaPublicKeyToStr(pair.getPublic());
            String privateKey = Encrypt.rsaPrivateKeyToStr(pair.getPrivate());
            System.out.println("密码长度是 " + size + " 时的公钥长度 " + publicKey.length() + " : " + publicKey);
            System.out.println("密码长度是 " + size + " 时的私钥长度 " + privateKey.length() + " : " + privateKey);

            String encode = Encrypt.rsaClientEncode(publicKey, SOURCE);
            System.out.println("密码长度是 " + size + " 时拿公钥加密后长度 " + encode.length() + " : " + encode);
            String data = Encrypt.rsaServerDecode(privateKey, encode);
            System.out.println("密码长度是 " + size + " 时拿私钥解密后(" + data + ")与原文" + (data.equals(SOURCE) ? "一致" : "不一致"));

            String sign = Encrypt.rsaServerSign(privateKey, SOURCE);
            System.out.println("密码长度是 " + size + " 时拿私钥生成验签数据长度 " + sign.length() + " : " + sign);
            boolean verify = Encrypt.rsaClientVerify(publicKey, SOURCE, sign);
            System.out.println("密码长度是 " + size + " 时拿公钥验签: " + (verify ? "成功" : "失败"));
            System.out.println("\n-------------\n");
        }
    }

    @Test
    public void rsaReqRes() {
        KeyPair pair = Encrypt.genericRsaKeyPair(512);
        String publicKey = Encrypt.rsaPublicKeyToStr(pair.getPublic());
        String privateKey = Encrypt.rsaPrivateKeyToStr(pair.getPrivate());
        System.out.println("任何地方都知道的公钥: " + publicKey);
        System.out.println("只有服务端知道的私钥: " + privateKey);

        System.out.println("要发送的源数据是(长度 " + SOURCE.length() + "): (" + SOURCE + ")");

        System.out.println("-----");

        Map<String, String> map = Encrypt.rsaClientEncodeWithValueAes(publicKey, SOURCE);
        String sendData = JsonUtil.toJson(map);
        System.out.println("客户端通过公钥 处理(aes)要发送的数据后: (" + sendData + ")");

        String decode = Encrypt.rsaServerDecodeWithValueAes(privateKey, map.get("k"), map.get("v"));
        System.out.println("服务端通过私钥 处理(aes)发过来的数据后: (" + decode + ")");
        System.out.print("源数据长度 " + decode.length() + ", 发送的数据长度 " + sendData.length());
        System.out.println(", 增长: " + new BigDecimal(sendData.length()).divide(new BigDecimal(decode.length()), 2, RoundingMode.DOWN) + " 倍");

        System.out.println("-----");

        map = Encrypt.rsaClientEncodeWithValueDes(publicKey, SOURCE);
        sendData = JsonUtil.toJson(map);
        System.out.println("客户端通过公钥 处理(des)要发送的数据后: (" + sendData + ")");

        decode = Encrypt.rsaServerDecodeWithValueDes(privateKey, map.get("k"), map.get("v"));
        System.out.println("服务端通过私钥 处理(des)发过来的数据后: (" + decode + ")");
        System.out.print("源数据长度 " + decode.length() + ", 发送的数据长度 " + sendData.length());
        System.out.println(", 增长: " + new BigDecimal(sendData.length()).divide(new BigDecimal(decode.length()), 2, RoundingMode.DOWN) + " 倍");

        System.out.println("-----");

        String sign = Encrypt.rsaServerSign(privateKey, SOURCE);
        System.out.println("服务端通过私钥 生成签名(长度 " + sign.length() + "): (" + sign + ")");

        boolean verify = Encrypt.rsaClientVerify(publicKey, SOURCE, sign);
        System.out.println("客户端通过公钥 验签: (" + verify + ")");

        System.out.println("-----");
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
        String secret = U.uuid();
        String encode = Encrypt.rc4Encode(SOURCE, secret);
        System.out.println(encode);

        String decode = Encrypt.rc4Decode(encode, secret);
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
