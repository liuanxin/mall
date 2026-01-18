package com.github.common.encrypt;

import com.github.common.util.U;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

public final class RsaEncrypt {

    /*
    // pnpm i jsencrypt crypto-js
    import JSEncrypt from 'jsencrypt';
    import CryptoJS from 'crypto-js';

    // 使用 crypto-js 计算 SHA256
    const sha256 = (str) => {
        return CryptoJS.SHA256(str).toString();
    }

    // 生成密钥对(公钥 私钥)
    const rsaGenerateKey = (keyLen = 512) => {
        const crypt = new JSEncrypt({ default_key_size: keyLen });
        return { priKey: crypt.getPrivateKey(), pubKey: crypt.getPublicKey() };
    }

    const rsaPub = (pubKey) => {
        const pub = new JSEncrypt();
        pub.setPublicKey(pubKey);
        return pub;
    }
    const rsaPri = (priKey) => {
        const pri = new JSEncrypt();
        pri.setPrivateKey(priKey);
        return pri;
    }

    // 使用公钥加密
    export const rsaEncrypt = (pubKey, data) => {
        return rsaPub(pubKey).encrypt(data);
    }

    // 使用私钥解密(通常用在服务端, 当前不在客户端用)
    const rsaDecrypt = (priKey, encryptData) => {
        return rsaPri(priKey).decrypt(encryptData);
    }

    // 使用私钥生成签名(通常用在服务端, 当前不在客户端用)
    const rsaSign = (priKey, data) => {
        return rsaPri(priKey).sign(data, sha256, 'sha256');
    }

    // 使用公钥验证签名, 正确则返回 true
    export const rsaVerify = (pubKey, data, sign) => {
        return rsaPub(pubKey).verify(data, sign, sha256);
    }

    function rsaExample() {
        // 1. 初始化并生成密钥对
        const pair = rsaGenerateKey();
        const publicKey = pair.pubKey;
        const privateKey = pair.priKey;

        console.log('--- RSA ---');
        console.log('公钥:', publicKey);
        console.log('私钥:', privateKey);

        const originalData = 'Hello RSA 中文 2026!';
        console.log('原文:', originalData);

        // 公钥加密
        let start = Date.now();
        const encrypted = rsaEncrypt(publicKey, originalData);
        console.log(`公钥加密后: ${encrypted}, 耗时:(${Date.now() - start})`);
        // 私钥解密
        start = Date.now();
        const decrypted = rsaDecrypt(privateKey, encrypted);
        console.log(`私钥解密后: ${decrypted}, 耗时:(${Date.now() - start})`);

        // 私钥签名
        start = Date.now();
        const sign = rsaSign(privateKey, originalData);
        console.log(`私钥生成签名: ${sign}, 耗时:(${Date.now() - start})`);
        // 公钥校验
        start = Date.now();
        const v = rsaVerify(publicKey, originalData, sign);
        console.log(`公钥验证签名: ${v}, 耗时:(${Date.now() - start})`);
        console.log('--- RSA ---');
    }
    */

    /** 加密数据的长度不能超过 53 */
    private static final String RSA = "RSA";
    /** 用来做数据验签时的算法 */
    private static final String RSA_SIGN = "SHA256withRSA";

    /**
     * <pre>生成 rsa 的密钥对, 建议只使用 512  1024  2048 即可, 过大会非常占用 cpu 资源
     *
     * 长度是 512 时生成的公钥长度是 128 私钥长度是 460
     * 长度是 1024 时生成的公钥长度是 216 私钥长度是 848, 建议使用
     * 长度是 2048 时生成的公钥长度是 392 私钥长度是 1624
     * 长度是 4096 时生成的公钥长度是 736 私钥长度是 3168, 不建议
     * 长度是 8192 时生成的公钥长度是 1416 私钥长度是 6240, 强烈不建议</pre>
     *
     * @param keyLength 长度不能小于 512
     */
    public static KeyPair genericRsaKeyPair(int keyLength) {
        // 生成 rsa 密钥对时的最小长度
        int keyMinLen = 512;
        if (keyLength < keyMinLen) {
            throw new RuntimeException(String.format("rsa 生成密钥对时长度不能小于 %s", keyMinLen));
        }
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            keyPairGenerator.initialize(keyLength);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 rsa 生成 %s 位的密钥对时异常", keyLength), e);
        }
    }

    public static String publicKeyToRsaStr(PublicKey key) {
        return new String(Encrypt.base64Encode(key.getEncoded()), StandardCharsets.UTF_8);
    }
    public static String privateKeyToRsaStr(PrivateKey key) {
        return new String(Encrypt.base64Encode(key.getEncoded()), StandardCharsets.UTF_8);
    }

    private static PublicKey rsaStrToPublicKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Encrypt.base64Decode(str.getBytes(StandardCharsets.UTF_8));
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(keyBytes));
    }
    private static PrivateKey rsaStrToPrivateKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Encrypt.base64Decode(str.getBytes(StandardCharsets.UTF_8));
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据传递 中的客户端操作: 使用 rsa 的公钥加密原文, 生成密文</pre>
     *
     * @see #rsaEncodeWithValueAes
     * @see #rsaEncodeWithValueDes
     * @param publicKey 公钥
     * @param source 原文, 长度不能超过 53 (ecc 没有此限制). 因此通常的做法是: {
     *     客户端: 生成一个随机数(0), 使用这个随机数做 aes 或 des 加密(1), 将随机数用 rsa 公钥加密(2),
     *     服务端: 用 rsa 私钥解密(2)得到随机数(0)再用 aes 或 des 解密(1)
     * }
     * @return 密文
     */
    public static String rsaEncode(String publicKey, String source) {
        // 使用 rsa 加密时数据的最大长度
        int maxDateLen = 53;
        if (U.isBlank(publicKey) || source == null || source.length() > maxDateLen) {
            throw new RuntimeException(String.format("用 rsa 基于公钥(%s)加密(%s)时数据不能为空或长度不能超过 %s",
                    publicKey, source, maxDateLen));
        }
        try {
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.ENCRYPT_MODE, rsaStrToPublicKey(publicKey));
            byte[] encodeBytes = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));
            // 用 base64 编码, 跟 rsaServerDecode 中的 xxx 对应
            return new String(Encrypt.base64Encode(encodeBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 rsa 基于公钥(%s)加密(%s)时异常", publicKey, source), e);
        }
    }
    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据传递 中的服务端操作: 使用 rsa 的私钥解密密文, 得到原文</pre>
     *
     * @see #rsaDecodeWithValueAes
     * @see #rsaDecodeWithValueDes
     * @param privateKey 私钥
     * @param encryptData 密文
     * @return 原文
     */
    public static String rsaDecode(String privateKey, String encryptData) {
        if (U.isBlank(privateKey) || U.isBlank(encryptData)) {
            throw new RuntimeException(String.format("用 rsa 基于私钥(%s)解密(%s)时数据不能为空", privateKey, encryptData));
        }
        try {
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.DECRYPT_MODE, rsaStrToPrivateKey(privateKey));
            // 用 base64 解码, 跟 rsaClientEncode 中的 xxx 对应
            byte[] decodeBytes = cipher.doFinal(Encrypt.base64Decode(encryptData.getBytes(StandardCharsets.UTF_8)));
            return new String(decodeBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 rsa 基于私钥(%s)解密(%s)时异常", privateKey, encryptData), e);
        }
    }

    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据验签 中的服务端操作: 使用 rsa 的私钥加密原文, 生成签名</pre>
     *
     * @param privateKey 私钥
     * @param source 原文
     * @return 签名数据
     */
    public static String rsaSign(String privateKey, String source) {
        if (U.isBlank(privateKey) || U.isBlank(source)) {
            throw new RuntimeException(String.format("用 rsa 基于私钥(%s)生成验签时数据(%s)不能为空", privateKey, source));
        }
        try {
            Signature privateSign = Signature.getInstance(RSA_SIGN);
            privateSign.initSign(rsaStrToPrivateKey(privateKey));
            privateSign.update(source.getBytes(StandardCharsets.UTF_8));
            // 将结果用 base64 编码, 跟 rsaClientVerify 中的 yyy 对应
            return new String(Encrypt.base64Encode(privateSign.sign()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 rsa 基于私钥(%s)给(%s)生成验签时异常", privateKey, source), e);
        }
    }
    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据验签 中的客户端操作: 使用 rsa 的公钥验签原文</pre>
     *
     * @param publicKey 公钥
     * @param source 原文
     * @param signData 签名
     * @return true 表示验签成功
     */
    public static boolean rsaVerify(String publicKey, String source, String signData) {
        if (U.isBlank(publicKey) || U.isBlank(source) || U.isBlank(signData)) {
            // throw new RuntimeException(String.format("用 rsa 基于公钥(%s)验签(%s)时(%s)不能为空", publicKey, source, signData));
            return false;
        }
        try {
            Signature publicSign = Signature.getInstance(RSA_SIGN);
            publicSign.initVerify(rsaStrToPublicKey(publicKey));
            publicSign.update(source.getBytes(StandardCharsets.UTF_8));
            // 将结果用 base64 编码, 跟 rsaServerSign 中的 yyy 对应
            return publicSign.verify(Encrypt.base64Decode(signData.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // throw new RuntimeException(String.format("用 rsa 基于公钥(%s)验签(%s)时(%s)异常", publicKey, source, signData), e);
            return false;
        }
    }


    /**
     * <pre>
     * rsa 在加密时, 原文长度超过 53 会有错误(因此当前方法有强制限制长度), 因此通常的做法是:
     *   客户端: 生成一个随机数(0), 使用这个随机数做 aes 或 des 加密(1), 将随机数用 rsa 公钥加密(2),
     *   服务端: 用 rsa 私钥解密(2)得到随机数(0)再用 aes 或 des 解密(1)
     *
     * 客户端操作: 有 公钥(pub) 和 要发送的数据(data)
     * 1. 生成 16 位的随机数(key)
     * 2. 用 rsa 基于 pub 加密 key 生成一个加密数据(1)
     * 3. 用 aes 基于 key 加密 data 生成加密数据(2)
     *
     * 返回 { "k" : (1), "v" : (2) }
     * </pre>
     */
    public static Map<String, String> rsaEncodeWithValueAes(String publicKey, String data) {
        // 随机数, 用来做 aes 的密钥, 长度 16 位. 用 rsa 基于公钥加密这个值, 用 aes 基于这个值加密数据
        String key = U.uuid();
        return Map.of("k", rsaEncode(publicKey, key), "v", Encrypt.aesEncode(data, key));
    }
    /**
     * <pre>
     * 服务端操作: 有 私钥(pri) 和 客户端传过来的 k 和 v
     * 1. 用 rsa 基于 pri 解密 k 得到一个值(key)
     * 2. 用 aes 基于 key 解密 v 得到 data
     *
     * 返回 data
     * </pre>
     */
    public static String rsaDecodeWithValueAes(String privateKey, String k, String v) {
        // 通过 rsa 解出 aes 的密钥, 再用密钥解出数据
        String key = rsaDecode(privateKey, k);
        return Encrypt.aesDecode(v, key);
    }

    /**
     * <pre>
     * rsa 在加密时, 原文长度超过 53 会有错误(因此当前方法有强制限制长度), 因此通常的做法是:
     *   客户端: 生成一个随机数(0), 使用这个随机数做 aes 或 des 加密(1), 将随机数用 rsa 公钥加密(2),
     *   服务端: 用 rsa 私钥解密(2)得到随机数(0)再用 aes 或 des 解密(1)
     *
     * 客户端操作: 有 公钥(pub) 和 要发送的数据(data)
     * 1. 生成 16 位的随机数(key)
     * 2. 用 rsa 基于 pub 加密 key 生成一个加密数据(1)
     * 3. 用 des 基于 key 加密 data 生成加密数据(2)
     *
     * 返回 { "k" : (1), "v" : (2) }
     * </pre>
     */
    public static Map<String, String> rsaEncodeWithValueDes(String publicKey, String data) {
        // 随机数, 用来做 aes 的密钥, 长度 8 位. 数据用这个来加密, 用 rsa 私钥加密这个值也传过去
        String key = U.uuid();
        return Map.of("k", rsaEncode(publicKey, key), "v", Encrypt.desEncode(data, key));
    }

    /**
     * <pre>
     * 服务端操作: 有 私钥(pri) 和 客户端传过来的 k 和 v
     * 1. 用 rsa 基于 pri 解密 k 得到一个值(key)
     * 2. 用 des 基于 key 解密 v 得到 data
     *
     * 返回 data
     * </pre>
     */
    public static String rsaDecodeWithValueDes(String privateKey, String k, String v) {
        // 通过 rsa 解出 aes 的密钥, 再用密钥解出数据
        String key = rsaDecode(privateKey, k);
        return Encrypt.desDecode(v, key);
    }
}
