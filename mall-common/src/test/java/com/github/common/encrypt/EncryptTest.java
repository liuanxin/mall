package com.github.common.encrypt;

import com.github.common.date.Dates;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.U;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyPair;
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
        System.out.println("===== RSA =====");
        int size = 512;
//        for (Integer size : Arrays.asList(512, 1024, 2048)) {
        KeyPair pair = RsaEncrypt.genericRsaKeyPair(size);
        String publicKey = RsaEncrypt.publicKeyToRsaStr(pair.getPublic());
        String privateKey = RsaEncrypt.privateKeyToRsaStr(pair.getPrivate());
//        String publicKey = ("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALJHy2oCwceK4J9JHIarwaq5jtomCftG\n" +
//                "lLwdzXtawm7/mWARFvHlO2XM0d4JobAWH2PRYzTqio1O+T1is7KJ4DECAwEAAQ==").replace("\n", "");
//        String privateKey = ("MIIBPAIBAAJBALJHy2oCwceK4J9JHIarwaq5jtomCftGlLwdzXtawm7/mWARFvHl\n" +
//                "O2XM0d4JobAWH2PRYzTqio1O+T1is7KJ4DECAwEAAQJAMKnaQ4CnJnGpKLGLQNNn\n" +
//                "VNO7w544gUdd2A+GhFJc2nEjxIT19w8vSSa334LixiGUTL+Pa9/RNKS2xhFcIxu2\n" +
//                "AQIhAOCs/uAhypAvEUqd/ykDby5Flh+ck6v0atb+lsoGSNPhAiEAyyLh+xv9jm4+\n" +
//                "J2jfQ5UT5UdVStghynSv0rJPCWR5llECIQCgM3pBQpb3HDiOJf5stiAutDuJKtI5\n" +
//                "CDyuNDY8syJ2wQIhALi78Gc8/UoaV8vfQ6tiV8WbKaX3CEPl+j/SiK4yAaEBAiEA\n" +
//                "wi+fXMOjHQBBTN/ik6iVfGKsLu0iu2G3pD24usSw4tA=").replace("\n", "");

        System.out.println("公钥: 「" + publicKey + "」");
        System.out.println("私钥: 「" + privateKey + "」");

        String source = "Hello RSA 中文 2026!";
        String encode = RsaEncrypt.rsaEncode(publicKey, source);
//        String encode = "IW7+TNhdvRqCJreA02T8lv3Od5JC85MyopGcGdG2CAoW7aTOzsqV1fACe1NwWt2liLnRhk8o96V9RKfrVli6ww==";
        System.out.println("用公钥加密: 「" + encode + "」");
        long start = System.currentTimeMillis();
        String data = RsaEncrypt.rsaDecode(privateKey, encode);
        System.out.println("测试的源串: 「" + source + "」, 耗时:(" + (System.currentTimeMillis() - start) + ")");
        System.out.println("用私钥解密: 「" + data + "」, 与原文" + (data.equals(source) ? "一致" : "不一致"));

        String sign = RsaEncrypt.rsaSign(privateKey, source);
//        String sign = "h/tjDAlu8yHTUOO8e6CQAASKF5Xp01aSOM92dFvQv/i4fIb+MH24Uz+f1+GlVCQOcYHni2FKGGaez/1RMr8y2g==";
        System.out.println("用私钥生成验签: 「" + sign + "」");
        start = System.currentTimeMillis();
        boolean verify = RsaEncrypt.rsaVerify(publicKey, source, sign);
        System.out.println("用公钥验证签名: 「" + (verify ? "成功" : "失败") + "」, 耗时:(" + (System.currentTimeMillis() - start) + ")");
        System.out.println("===== RSA =====");
        /*
        ===== RSA =====
        公钥: 「MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJjkNzrx2wgcDgbuC0X42ZCUYYQUqS8KMF6ok40juG4T57A0vWo5NM9b6QW7cRWzSQ74zUUIBEkfwDa8I76qVE8CAwEAAQ==」
        私钥: 「MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEAmOQ3OvHbCBwOBu4LRfjZkJRhhBSpLwowXqiTjSO4bhPnsDS9ajk0z1vpBbtxFbNJDvjNRQgESR/ANrwjvqpUTwIDAQABAkBIiJCJNt9uR/0BnaIchpQU6sgepLyk0+Uhq0EhBu7CzZBEUCEShy0/a9DxOLuDsdBTdKeny7LAVsKVp18CXntpAiEA8TrYA1prsnBmzaBioPC2VVcsu7Pn0wjuQb+eDJq92gkCIQCiQLZYEPpn238muajBv5QQ6rG9gTT8glOqLMiTyxsxlwIgH347LVTksosSIM5Lkg9a/pE++dJm9Zo44MSPcb3SA2ECIF4KheV7SbeiiAsI9t/9SzOW5BgDaJOmchmjRUosIYHXAiA539xttInG1gEBgctn1u1/G4sdwAWi4Sio7p5J2CfTcg==」
        用公钥加密: 「APckl9vRc91ZAZ1uahFbNave5LirJbMFQ8elYVgWtAmpM64VXDWae5jDbZQtjUr7tYsmZeLl8edvXi5URb2Z9w==」
        测试的源串: 「Hello RSA 中文 2026!」
        用私钥解密: 「Hello RSA 中文 2026!」, 与原文一致
        用私钥生成验签: 「Tx0TBUSKE9z1NE7SpbsEUy76/ewKEXNU5WTs3JpIGQ2UC8o15nrDiIhOcZdb7h7i8gxVcBXdkJ+cs0JHKXX/JQ==」
        用公钥验证签名: 「成功」
        ===== RSA =====
        */
//        }
    }

    @Test
    public void eccCheck() {
        System.out.println("===== ECC =====");
        KeyPair pair = EccEncrypt.genericEccKeyPair();
        String publicKey = EccEncrypt.publicKeyToEccStr(pair.getPublic());
        String privateKey = EccEncrypt.privateKeyToEccStr(pair.getPrivate());
//        String publicKey = "04b7668e1d04f9f5c821f7daef701115da26701e791b885707bed5cf5a8dab255bbb2cefc1f73bd673757d87e2a9954b780a59515dcaab091b4c66e4aab9954271";
//        String privateKey = "1b31d68ac5886a27fa7e297eb9e94d205a54a7cd4e2188333204470915e9ffb9";
        System.out.println("公钥: 「" + publicKey + "」");
        System.out.println("私钥: 「" + privateKey + "」");

        String source = "Hello ECC 中文 2026!";
        long start = System.currentTimeMillis();
        String encode = EccEncrypt.eccEncode(publicKey, source);
//        String encode = "047ece730fbf3ba79733b7773feeec4590ec6ae885f591d61ec938d337e5ac3f9e7ed4f70f6b39894d0ad3b6a2e41ce051849cd68c285f0bdbb9b24dd639ce677ce785692f19a912d3d044c28d99e04baegSlxCk4FU1nTrjpyfpC2sW3NICIiQu65YlfxLUfuIMI=";
        System.out.println("用公钥加密: 「" + encode + "」, 耗时(" + (System.currentTimeMillis() - start) + ")");
        start = System.currentTimeMillis();
        String data = EccEncrypt.eccDecode(privateKey, encode);
        System.out.println("测试的源串: 「" + source + "」");
        System.out.println("用私钥解密: 「" + data + "」, 耗时(" + (System.currentTimeMillis() - start) + "), 与原文" + (data.equals(source) ? "一致" : "不一致"));

        start = System.currentTimeMillis();
        String sign = EccEncrypt.eccSign(privateKey, source);
//        String sign = "30450221009a822cabe86d916c527f7e7865216dd79c4731ad8f37ab854b346af0a8e81560022061e36b8a2a8e89b89bf25ccf5d8db7cb5d45798f93309cbd663008fa596f56a7";
        System.out.println("用私钥生成验签: 「" + sign + "」, 耗时(" + (System.currentTimeMillis() - start) + ")");
        start = System.currentTimeMillis();
        boolean verify = EccEncrypt.eccVerify(publicKey, source, sign);
        System.out.println("用公钥验证签名: 「" + (verify ? "成功" : "失败") + "」, 耗时(" + (System.currentTimeMillis() - start) + ")");
        System.out.println("===== ECC =====");

        /*
        ===== ECC =====
        公钥: 「04fd39bd04c56dcad141d9ddeecac9e4c9ca944e0c1914df26d992d987b25e1fb804a9d2a0b1fdbffe3be61c665dbeddd5f98be8fbf793ec2709f611a6c0553202」
        私钥: 「66ce1a3136bcf9df5547237a0254b998e51af7acdcecfd3aa84369512396da45」
        用公钥加密: 「04baa8ce658d89b96418d646aae6cda358f7f20e228551d5092fed6b0695f96fe599139c6c0cc13e2300a74405f8b0dafb842a5893cf4d2167bae8e20c7cdb681dc5c77a26219afb69a8108c5ce54bdd64P5iK7HMfo1FrsG7q6yUm+nRATv+8ZigmvwpG9pasEvc=」, 耗时(35)
        测试的源串: 「Hello ECC 中文 2026!」, 耗时(7)
        用私钥解密: 「Hello ECC 中文 2026!」, 与原文一致
        用私钥生成验签: 「304402204747e69c58196d567354d2ec6e03d2d6bb9d8bbcbfe86c54c452f898ec76ad2002207aad63599ac6d3d3b4437431cbed88638a81d68fe494fc941144ff58e4573e33」, 耗时(28)
        用公钥验证签名: 「成功」, 耗时(2)
        ===== ECC =====`
        */
    }

    @Test
    public void rsaReqRes() {
//        for (Integer size : Arrays.asList(512, 1024, 2048)) {
        int size = 512;
        KeyPair pair = RsaEncrypt.genericRsaKeyPair(size);

        String publicKey = RsaEncrypt.publicKeyToRsaStr(pair.getPublic());
        String privateKey = RsaEncrypt.privateKeyToRsaStr(pair.getPrivate());
        System.out.println("任何地方都知道的公钥: " + publicKey);
        System.out.println("只有服务端知道的私钥: " + privateKey);
        System.out.println("要发送的源数据是(长度 " + SOURCE.length() + "): (" + SOURCE + ")");

        System.out.println("-----");

        Map<String, String> map = RsaEncrypt.rsaEncodeWithValueAes(publicKey, SOURCE);
        String sendData = JsonUtil.toJson(map);
        System.out.println("客户端通过公钥 处理(aes)要发送的数据后: (" + sendData + ")");

        String decode = RsaEncrypt.rsaDecodeWithValueAes(privateKey, map.get("k"), map.get("v"));
        System.out.println("服务端通过私钥 处理(aes)发过来的数据后: (" + decode + ")");
        System.out.print("源数据长度 " + decode.length() + ", 发送的数据长度 " + sendData.length());
        System.out.println(", 增长: " + new BigDecimal(sendData.length()).divide(new BigDecimal(decode.length()), 2, RoundingMode.DOWN) + " 倍");

        System.out.println("-----");

        map = RsaEncrypt.rsaEncodeWithValueDes(publicKey, SOURCE);
        sendData = JsonUtil.toJson(map);
        System.out.println("客户端通过公钥 处理(des)要发送的数据后: (" + sendData + ")");

        decode = RsaEncrypt.rsaDecodeWithValueDes(privateKey, map.get("k"), map.get("v"));
        System.out.println("服务端通过私钥 处理(des)发过来的数据后: (" + decode + ")");
        System.out.print("源数据长度 " + decode.length() + ", 发送的数据长度 " + sendData.length());
        // 512:  源数据长度 39, 发送的数据长度 199, 增长: 5.10 倍
        // 1024: 源数据长度 39, 发送的数据长度 283, 增长: 7.25 倍
        // 2048: 源数据长度 39, 发送的数据长度 455, 增长: 11.66 倍
        System.out.println(", 增长: " + new BigDecimal(sendData.length()).divide(new BigDecimal(decode.length()), 2, RoundingMode.DOWN) + " 倍");
        System.out.println("-----");
//      }
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
    public void authenticator() {
        String secret = U.uuid().replace("0", "O").replace("1", "I");
        for (int i = 0; i < 35; i++) {
            String code = Encrypt.getGoogleAuthenticatorCode(secret);
            System.out.println(Dates.nowDateTimeMs() + " : " + secret + " -> " + code);
            try {
                TimeUnit.SECONDS.sleep(1L);
            } catch (InterruptedException ignore) {
            }
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
