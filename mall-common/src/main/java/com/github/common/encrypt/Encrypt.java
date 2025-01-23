package com.github.common.encrypt;

import com.github.common.encrypt.jwt.JWTExpiredException;
import com.github.common.encrypt.jwt.JWTSigner;
import com.github.common.encrypt.jwt.JWTVerifier;
import com.github.common.exception.ForbiddenException;
import com.github.common.util.U;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** AES、DES、DES/CBC/PKCS5Padding、jwt 加密解密, base64 编码解码, md5、sha-1、sha-224、sha-256、sha-384、sha-512 加密算法 */
public final class Encrypt {

    private static final String AES = "AES";
    private static final String DES = "DES";
    /** 密钥只能是 8 位 */
    private static final String DES_CBC_PKCS5PADDING = "DES/CBC/PKCS5Padding";

    /** 加密数据的长度不能超过 53 */
    private static final String RSA = "RSA";
    /** 生成 rsa 密钥对时的最小长度 */
    private static final int RSA_KEY_MIN_LEN = 512;
    /**
     * <pre>使用 rsa 加密时数据的最大长度. 因此通常的做法是: {
     *     客户端: 生成一个随机数(0), 使用这个随机数做 aes 或 des 加密(1), 将随机数用 rsa 公钥加密(2),
     *     服务端: 用 rsa 私钥解密(2)得到随机数(0)再用 aes 或 des 解密(1)
     * }</pre>
     */
    private static final int RSA_DATA_MAX_LEN = 53;
    /** 用来做数据验签时的算法 */
    private static final String RSA_SIGN = "SHA256withRSA";

    private static final String JWT_SECRET_KEY = "*W0$%Te#nr&y^pOt";
    private static final JWTSigner JWT_SIGNER = new JWTSigner(JWT_SECRET_KEY);
    private static final JWTVerifier JWT_VERIFIER = new JWTVerifier(JWT_SECRET_KEY);

    private static final boolean SUPPORT_ECC;
    static {
        boolean hasEcc = false;
        try {
            Class<?> clazz = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Security.addProvider((Provider) clazz.getDeclaredConstructor().newInstance());
            hasEcc = true;
        } catch (Exception ignore) {}
        SUPPORT_ECC = hasEcc;
    }

    /** 使用 base64 编码 */
    public static String base64Encode(String src) {
        return new String(base64Encode(src.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
    public static byte[] base64Encode(byte[] src) {
        return Base64.getEncoder().encode(src);
    }
    /** 使用 base64 解码 */
    public static String base64Decode(String src) {
        return new String(base64Decode(src.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
    public static byte[] base64Decode(byte[] src) {
        return Base64.getDecoder().decode(src);
    }


    /** 使用 aes 加密 */
    public static String aesEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException(String.format("空无需使用 %s 加密", AES));
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), AES));
            return binary2Hex(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 加密(%s)密钥(%s)时异常", AES, data, secretKey), e);
        }
    }
    /** 使用 aes 解密 */
    public static String aesDecode(String data, String secretKey) {
        if (U.isBlank(data)) {
            throw new RuntimeException(String.format("空无需使用 %s 解密", AES));
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), AES));
            return new String(cipher.doFinal(hex2Binary(data)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 解密(%s)密钥(%s)时异常", AES, data, secretKey), e);
        }
    }


    /** 使用 des 加密 */
    public static String desEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException(String.format("空无需使用 %s 加密", DES));
        }
        try {
            DESKeySpec desKey = new DESKeySpec(secretKey.getBytes(StandardCharsets.UTF_8));
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.ENCRYPT_MODE, secretkey, new SecureRandom());
            return binary2Hex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 加密(%s)密钥(%s)时异常", DES, data, secretKey), e);
        }
    }
    /** 使用 des 解密 */
    public static String desDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException(String.format("空无需使用 %s 解密", DES));
        }
        try {
            DESKeySpec desKey = new DESKeySpec(secretKey.getBytes(StandardCharsets.UTF_8));
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.DECRYPT_MODE, secretkey, new SecureRandom());
            return new String(cipher.doFinal(hex2Binary(data)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 解密(%s)密钥(%s)时异常", DES, data, secretKey), e);
        }
    }


    /** 使用 DES/CBC/PKCS5Padding 加密 */
    public static String desCbcEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException(String.format("空无需使用 %s 加密", DES_CBC_PKCS5PADDING));
        }
        if (secretKey.length() != 8) {
            throw new RuntimeException(String.format("%s 加密时, 密钥必须是 %s 位", DES_CBC_PKCS5PADDING, 8));
        }
        try {
            byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            DESKeySpec desKey = new DESKeySpec(secretKeyBytes);
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES_CBC_PKCS5PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretkey, new IvParameterSpec(secretKeyBytes));
            return binary2Hex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 加密(%s)密钥(%s)时异常", DES_CBC_PKCS5PADDING, data, secretKey), e);
        }
    }
    /** 使用 DES/CBC/PKCS5Padding 解密 */
    public static String desCbcDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException(String.format("空无需使用 %s 解密", DES_CBC_PKCS5PADDING));
        }
        if (secretKey.length() != 8) {
            throw new RuntimeException(String.format("%s 解密时, 密钥必须是 %s 位", DES_CBC_PKCS5PADDING, 8));
        }
        try {
            byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            DESKeySpec desKey = new DESKeySpec(secretKeyBytes);
            SecretKey key = SecretKeyFactory.getInstance(DES).generateSecret(desKey);
            Cipher cipher = Cipher.getInstance(DES_CBC_PKCS5PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(secretKeyBytes));
            return new String(cipher.doFinal(hex2Binary(data)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 解密(%s)密钥(%s)时异常", DES_CBC_PKCS5PADDING, data, secretKey), e);
        }
    }


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
        if (keyLength < RSA_KEY_MIN_LEN) {
            throw new RuntimeException(String.format("%s 生成密钥对时长度不能小于 %s", RSA, RSA_KEY_MIN_LEN));
        }
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            keyPairGenerator.initialize(keyLength);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 生成 %s 位的密钥对时异常", RSA, keyLength), e);
        }
    }

    public static KeyPair genericEccKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "SunEC"); // secp256r1 [NIST P-256,X9.62 prime256v1] (1.2.840.10045.3.1.7)
            keyGen.initialize(new ECGenParameterSpec("secp384r1"));
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("用 ECC 生成密钥对时异常", e);
        }
    }

    public static String publicKeyToStr(PublicKey key) {
        return new String(base64Encode(key.getEncoded()), StandardCharsets.UTF_8);
    }
    public static String privateKeyToStr(PrivateKey key) {
        return new String(base64Encode(key.getEncoded()), StandardCharsets.UTF_8);
    }

    private static PublicKey getRsaPublicKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = base64Decode(str.getBytes(StandardCharsets.UTF_8));
        return KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(keyBytes));
    }
    private static PrivateKey getRsaPrivateKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = base64Decode(str.getBytes(StandardCharsets.UTF_8));
        return KeyFactory.getInstance(RSA).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private static PublicKey getEccPublicKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = base64Decode(str.getBytes(StandardCharsets.UTF_8));
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(keyBytes));
    }
    private static PrivateKey getEccPrivateKey(String str) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = base64Decode(str.getBytes(StandardCharsets.UTF_8));
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
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
     * @see #rsaClientEncodeWithValueAes
     * @see #rsaClientEncodeWithValueDes
     * @param publicKey 公钥
     * @param source 原文, 长度不能超过 53. 因此通常的做法是: {
     *     客户端: 生成一个随机数(0), 使用这个随机数做 aes 或 des 加密(1), 将随机数用 rsa 公钥加密(2),
     *     服务端: 用 rsa 私钥解密(2)得到随机数(0)再用 aes 或 des 解密(1)
     * }
     * @return 密文
     */
    public static String rsaClientEncode(String publicKey, String source) {
        if (U.isBlank(publicKey) || source == null || source.length() > RSA_DATA_MAX_LEN) {
            throw new RuntimeException(String.format("用 %s 基于公钥(%s)加密(%s)时数据不能为空或长度不能超过 %s",
                    RSA, publicKey, source, RSA_DATA_MAX_LEN));
        }
        try {
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.ENCRYPT_MODE, getRsaPublicKey(publicKey));
            byte[] encodeBytes = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));
            // 用 base64 编码, 跟 rsaServerDecode 中的 xxx 对应
            return new String(base64Encode(encodeBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 基于公钥(%s)加密(%s)时异常", RSA, publicKey, source), e);
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
     * @see #rsaServerDecodeWithValueAes
     * @see #rsaServerDecodeWithValueDes
     * @param privateKey 私钥
     * @param encryptData 密文
     * @return 原文
     */
    public static String rsaServerDecode(String privateKey, String encryptData) {
        if (U.isBlank(privateKey) || U.isBlank(encryptData)) {
            throw new RuntimeException(String.format("用 %s 基于私钥(%s)解密(%s)时数据不能为空", RSA, privateKey, encryptData));
        }
        try {
            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.DECRYPT_MODE, getRsaPrivateKey(privateKey));
            // 用 base64 解码, 跟 rsaClientEncode 中的 xxx 对应
            byte[] decodeBytes = cipher.doFinal(base64Decode(encryptData.getBytes(StandardCharsets.UTF_8)));
            return new String(decodeBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 基于私钥(%s)解密(%s)时异常", RSA, privateKey, encryptData), e);
        }
    }

    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据传递 中的客户端操作: 使用 ecc 的公钥加密原文, 生成密文</pre>
     *
     * @param publicKey 公钥
     * @param source 原文, rsa 有长度不能超过 53 的限制, ecc 没有
     * @return 密文
     */
    public static String eccClientEncode(String publicKey, String source) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        try {
            Cipher cipher = Cipher.getInstance("ECIES", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, getEccPublicKey(publicKey));
            byte[] encodeBytes = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));
            return new String(base64Encode(encodeBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 ECC 基于公钥(%s)加密(%s)时异常", publicKey, source), e);
        }
    }
    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据传递 中的服务端操作: 使用 ecc 的私钥解密密文, 得到原文</pre>
     *
     * @param privateKey 私钥
     * @param encryptData 密文
     * @return 原文
     */
    public static String eccServerDecode(String privateKey, String encryptData) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        if (U.isBlank(privateKey) || U.isBlank(encryptData)) {
            throw new RuntimeException(String.format("用 ECC 基于私钥(%s)解密(%s)时数据不能为空", privateKey, encryptData));
        }
        try {
            Cipher cipher = Cipher.getInstance("ECIES", "BC");
            cipher.init(Cipher.DECRYPT_MODE, getEccPrivateKey(privateKey));
            byte[] decodeBytes = cipher.doFinal(base64Decode(encryptData.getBytes(StandardCharsets.UTF_8)));
            return new String(decodeBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 ECC 基于私钥(%s)解密(%s)时异常", privateKey, encryptData), e);
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
    public static String rsaServerSign(String privateKey, String source) {
        if (U.isBlank(privateKey) || U.isBlank(source)) {
            throw new RuntimeException(String.format("用 %s 基于私钥(%s)生成验签时数据(%s)不能为空", RSA, privateKey, source));
        }
        try {
            Signature privateSign = Signature.getInstance(RSA_SIGN);
            privateSign.initSign(getRsaPrivateKey(privateKey));
            privateSign.update(source.getBytes(StandardCharsets.UTF_8));
            // 将结果用 base64 编码, 跟 rsaClientVerify 中的 yyy 对应
            return new String(base64Encode(privateSign.sign()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 基于私钥(%s)给(%s)生成验签时异常", RSA, privateKey, source), e);
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
    public static boolean rsaClientVerify(String publicKey, String source, String signData) {
        if (U.isBlank(publicKey) || U.isBlank(source) || U.isBlank(signData)) {
            // throw new RuntimeException(String.format("用 %s 基于公钥(%s)验签(%s)时(%s)不能为空", RSA, publicKey, source, signData));
            return false;
        }
        try {
            Signature publicSign = Signature.getInstance(RSA_SIGN);
            publicSign.initVerify(getRsaPublicKey(publicKey));
            publicSign.update(source.getBytes(StandardCharsets.UTF_8));
            // 将结果用 base64 编码, 跟 rsaServerSign 中的 yyy 对应
            return publicSign.verify(base64Decode(signData.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // throw new RuntimeException(String.format("用 %s 基于公钥(%s)验签(%s)时(%s)异常", RSA, publicKey, source, signData), e);
            return false;
        }
    }

    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据验签 中的服务端操作: 使用 ecc 的私钥加密原文, 生成签名</pre>
     *
     * @param privateKey 私钥
     * @param source 原文
     * @return 签名数据
     */
    public static String eccServerSign(String privateKey, String source) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        if (U.isBlank(privateKey) || U.isBlank(source)) {
            throw new RuntimeException(String.format("用 ECC 基于私钥(%s)生成验签时数据(%s)不能为空", privateKey, source));
        }
        try {
            Signature privateSign = Signature.getInstance("SHA1withECDSA");
            privateSign.initSign(getEccPrivateKey(privateKey));
            privateSign.update(source.getBytes(StandardCharsets.UTF_8));
            return new String(base64Encode(privateSign.sign()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 ECC 基于私钥(%s)给(%s)生成验签时异常", privateKey, source), e);
        }
    }
    /**
     * <pre>
     * 服务端持有私钥, 公钥公开.
     *
     * 用于数据传递: 客户端用公钥加密原文后发给服务端, 服务端拿到数据后用私钥解密得到原文
     * 用于数据验签: 服务端用私钥加密数据后公开, 客户端用公钥签验确定数据确实是服务端发的
     *
     * 当前方法 用于数据验签 中的客户端操作: 使用 ecc 的公钥验签原文</pre>
     *
     * @param publicKey 公钥
     * @param source 原文
     * @param signData 签名
     * @return true 表示验签成功
     */
    public static boolean eccClientVerify(String publicKey, String source, String signData) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        if (U.isBlank(publicKey) || U.isBlank(source) || U.isBlank(signData)) {
            // throw new RuntimeException(String.format("用 ECC 基于公钥(%s)验签(%s)时(%s)不能为空", publicKey, source, signData));
            return false;
        }
        try {
            Signature publicSign = Signature.getInstance("SHA1withECDSA");
            publicSign.initVerify(getEccPublicKey(publicKey));
            publicSign.update(source.getBytes(StandardCharsets.UTF_8));
            return publicSign.verify(base64Decode(signData.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // throw new RuntimeException(String.format("用 ECC 基于公钥(%s)验签(%s)时(%s)异常", publicKey, source, signData), e);
            return false;
        }
    }

    /**
     * <pre>
     * 客户端操作: 有 公钥(pub) 和 要发送的数据(data)
     * 1. 生成 16 位的随机数(key)
     * 2. 用 rsa 基于 pub 加密 key 生成一个加密数据(1)
     * 3. 用 aes 基于 key 加密 data 生成加密数据(2)
     *
     * 返回 { "k" : (1), "v" : (2) }
     * </pre>
     */
    public static Map<String, String> rsaClientEncodeWithValueAes(String publicKey, String data) {
        // 随机数, 用来做 aes 的密钥, 长度 16 位. 用 rsa 基于公钥加密这个值, 用 aes 基于这个值加密数据
        String key = U.uuid();
        return Map.of("k", rsaClientEncode(publicKey, key), "v", aesEncode(data, key));
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
    public static String rsaServerDecodeWithValueAes(String privateKey, String k, String v) {
        // 通过 rsa 解出 aes 的密钥, 再用密钥解出数据
        String key = rsaServerDecode(privateKey, k);
        return aesDecode(v, key);
    }

    /**
     * <pre>
     * ecc 没有原文度不能超过 53 的限制, 可以不需要
     *
     * 客户端操作: 有 公钥(pub) 和 要发送的数据(data)
     * 1. 生成 16 位的随机数(key)
     * 2. 用 ecc 基于 pub 加密 key 生成一个加密数据(1)
     * 3. 用 aes 基于 key 加密 data 生成加密数据(2)
     *
     * 返回 { "k" : (1), "v" : (2) }
     * </pre>
     */
    public static Map<String, String> eccClientEncodeWithValueAes(String publicKey, String data) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        // 随机数, 用来做 aes 的密钥, 长度 16 位. 用 ecc 基于公钥加密这个值, 用 aes 基于这个值加密数据
        String key = U.uuid();
        return Map.of("k", eccClientEncode(publicKey, key), "v", aesEncode(data, key));
    }
    /**
     * <pre>
     * ecc 没有原文度不能超过 53 的限制, 可以不需要
     *
     * 服务端操作: 有 私钥(pri) 和 客户端传过来的 k 和 v
     * 1. 用 ecc 基于 pri 解密 k 得到一个值(key)
     * 2. 用 aes 基于 key 解密 v 得到 data
     *
     * 返回 data
     * </pre>
     */
    public static String eccServerDecodeWithValueAes(String privateKey, String k, String v) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        // 通过 ecc 解出 aes 的密钥, 再用密钥解出数据
        String key = eccServerDecode(privateKey, k);
        return aesDecode(v, key);
    }

    /**
     * <pre>
     * 客户端操作: 有 公钥(pub) 和 要发送的数据(data)
     * 1. 生成 16 位的随机数(key)
     * 2. 用 rsa 基于 pub 加密 key 生成一个加密数据(1)
     * 3. 用 des 基于 key 加密 data 生成加密数据(2)
     *
     * 返回 { "k" : (1), "v" : (2) }
     * </pre>
     */
    public static Map<String, String> rsaClientEncodeWithValueDes(String publicKey, String data) {
        // 随机数, 用来做 aes 的密钥, 长度 8 位. 数据用这个来加密, 用 rsa 私钥加密这个值也传过去
        String key = U.uuid();
        return Map.of("k", rsaClientEncode(publicKey, key), "v", desEncode(data, key));
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
    public static String rsaServerDecodeWithValueDes(String privateKey, String k, String v) {
        // 通过 rsa 解出 aes 的密钥, 再用密钥解出数据
        String key = rsaServerDecode(privateKey, k);
        return desDecode(v, key);
    }

    /**
     * <pre>
     * ecc 没有原文度不能超过 53 的限制, 可以不需要
     *
     * 客户端操作: 有 公钥(pub) 和 要发送的数据(data)
     * 1. 生成 16 位的随机数(key)
     * 2. 用 ecc 基于 pub 加密 key 生成一个加密数据(1)
     * 3. 用 des 基于 key 加密 data 生成加密数据(2)
     *
     * 返回 { "k" : (1), "v" : (2) }
     * </pre>
     */
    public static Map<String, String> eccClientEncodeWithValueDes(String publicKey, String data) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        // 随机数, 用来做 aes 的密钥, 长度 8 位. 数据用这个来加密, 用 ecc 私钥加密这个值也传过去
        String key = U.uuid();
        return Map.of("k", eccClientEncode(publicKey, key), "v", desEncode(data, key));
    }
    /**
     * <pre>
     * ecc 没有原文度不能超过 53 的限制, 可以不需要
     *
     * 服务端操作: 有 私钥(pri) 和 客户端传过来的 k 和 v
     * 1. 用 ecc 基于 pri 解密 k 得到一个值(key)
     * 2. 用 des 基于 key 解密 v 得到 data
     *
     * 返回 data
     * </pre>
     */
    public static String eccServerDecodeWithValueDes(String privateKey, String k, String v) {
        if (!SUPPORT_ECC) {
            throw new RuntimeException("不支持 ECC 算法, 缺少对应的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on");
        }
        // 通过 ecc 解出 aes 的密钥, 再用密钥解出数据
        String key = eccServerDecode(privateKey, k);
        return desDecode(v, key);
    }


    /**
     * bcrypt 慢加密
     *
     * @param password 原密码
     * @return 加密后的密码
     */
    public static String bcryptEncode(String password) {
        return BCrypt.encrypt(password, BCrypt.genSalt());
    }
    /**
     * 验证密码是否相同
     *
     * @param password 原密码
     * @param encryptPass 加密后的密码. 60 位
     * @return 如果加密后相同, 则返回 true
     */
    public static boolean checkBcrypt(String password, String encryptPass) {
        if (encryptPass == null || encryptPass.length() == 0) {
            return false;
        }

        try {
            return encryptPass.equals(BCrypt.encrypt(password, encryptPass));
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 验证密码是否不相同
     *
     * @param password 原密码
     * @param encryptPass 加密后的密码. 60 位
     * @return 如果加密后不相同, 则返回 true
     */
    public static boolean checkNotBcrypt(String password, String encryptPass) {
        return !checkBcrypt(password, encryptPass);
    }


    /** 基于 secret 使用 jwt 将 map 进行编码并使用 aes 加密 */
    public static String jwtEncode(String secret, Map<String, Object> map) {
        return new JWTSigner(secret).sign(map);
    }
    /** 基于 secret 将 map 设置过期时间且进行 jwt 编码并使用 aes 加密 */
    public static String jwtEncode(String secret, Map<String, Object> map, long time, TimeUnit unit) {
        map.put(JWTVerifier.EXP, System.currentTimeMillis() + unit.toMillis(time));
        return new JWTSigner(secret).sign(map);
    }
    /** 使用 aes 解密并基于 secret 解码 jwt 及验证过期和数据完整性, 解码异常 或 数据已过期 或 验证失败 则抛出未登录异常 */
    public static Map<String, Object> jwtDecode(String secret, String data) {
        if (U.isBlank(data)) {
            throw new ForbiddenException("数据有误, 请重新登录");
        }

        try {
            return new JWTVerifier(secret).verify(data);
        } catch (JWTExpiredException e) {
            throw new ForbiddenException("登录已过期, 请重新登录", e);
        } catch (Exception e) {
            throw new ForbiddenException(String.format("数据(%s)验证失败, 请重新登录", data), e);
        }
    }

    /** 使用 jwt 将 map 进行编码并使用 aes 加密 */
    public static String jwtEncode(Map<String, Object> map) {
        return JWT_SIGNER.sign(map);
    }
    /** 将 map 设置过期时间且进行 jwt 编码并使用 aes 加密 */
    public static String jwtEncode(Map<String, Object> map, long time, TimeUnit unit) {
        map.put(JWTVerifier.EXP, System.currentTimeMillis() + unit.toMillis(time));
        return JWT_SIGNER.sign(map);
    }
    /** 使用 aes 解密并解码 jwt 及验证过期和数据完整性, 解码异常 或 数据已过期 或 验证失败 则抛出未登录异常 */
    public static Map<String, Object> jwtDecode(String data) {
        if (U.isBlank(data)) {
            throw new ForbiddenException("数据有误, 请重新登录");
        }

        try {
            return JWT_VERIFIER.verify(data);
        } catch (JWTExpiredException e) {
            throw new ForbiddenException("登录已过期, 请重新登录", e);
        } catch (Exception e) {
            throw new ForbiddenException(String.format("数据(%s)验证失败, 请重新登录", data), e);
        }
    }


    /**
     * 生成 google 动态令牌
     *
     * @param secret 密钥. 当 secret 中包含非 base32 的字符(字母 a-z 及数字 2-7, 字母不区分大小写)时会进行去除
     * @return 生成的 google 验证码(6 位数字), 每过 30 秒变化
     */
    public static String getGoogleAuthenticatorCode(String secret) {
        return getGoogleAuthenticatorCode(secret, true);
    }

    /**
     * 生成 google 动态令牌
     *
     * @param secret 密钥
     * @param handleSecretError true: 当 secret 中包含非 base32 的字符(字母 a-z 及数字 2-7, 字母不区分大小写)时去除, false: 抛出异常
     * @return 生成的 google 验证码(6 位数字), 每过 30 秒变化
     */
    public static String getGoogleAuthenticatorCode(String secret, boolean handleSecretError) {
        return getGoogleAuthenticatorCode(secret, handleSecretError, 0);
    }

    /**
     * 生成 google 动态令牌
     *
     * @param secret 密钥. 由 base32 字母表组成: 字母 a-z 及数字 2-7, 字母不区分大小写
     * @param handleSecretError true: 如果 secret 中包含非 base32 的字符则去除, 否则将会抛出异常
     * @param offsetSecond 偏移时间(-29 到 29 之间), 正数则往前偏移指定秒, 负数则往后偏移指定秒
     * @return 生成的 google 验证码(6 位数字), 每过 30 秒变化
     */
    public static String getGoogleAuthenticatorCode(String secret, boolean handleSecretError, int offsetSecond) {
        return getGoogleAuthenticatorCode(secret, handleSecretError, System.currentTimeMillis(), offsetSecond);
    }

    /**
     * 生成 google 动态令牌
     *
     * @param secret 密钥. 由 base32 字母表组成: 字母 a-z 及数字 2-7(字母不区分大小写, 26 个字母 + 6 个数字, 共 32 位), 数字 0 1 8 被跳过(它们与字母 O I B 相似)
     * @param handleSecretError true: 如果 secret 中包含非 base32 的字符则去除, 否则将会抛出异常
     * @param ms 时间戳
     * @param offsetSecond 偏移时间(-29 到 29 之间), 正数则往前偏移指定秒, 负数则往后偏移指定秒
     * @return 生成的 google 验证码(6 位数字), 每过 30 秒变化
     */
    public static String getGoogleAuthenticatorCode(String secret, boolean handleSecretError, long ms, int offsetSecond) {
        return GoogleAuthenticator.getCode(secret, handleSecretError, ms, offsetSecond);
    }


    /** 使用 rc4 加密 */
    public static String rc4Encode(String input, String key) {
        return base64Encode(rc4(input, key));
    }
    /** 使用 rc4 解密 */
    public static String rc4Decode(String input, String key) {
        return rc4(base64Decode(input), key);
    }
    /** 使用 rc4 加解密, 如果是密文调用此方法将返回明文 */
    private static String rc4(String input, String key) {
        int[] iS = new int[256];
        byte[] iK = new byte[256];

        for (int i = 0; i < 256; i++) {
            iS[i] = i;
        }
        for (short i = 0; i < 256; i++) {
            iK[i] = (byte) key.charAt((i % key.length()));
        }

        int j = 0;
        for (int i = 0; i < 255; i++) {
            j = (j + iS[i] + iK[i]) % 256;
            int temp = iS[i];
            iS[i] = iS[j];
            iS[j] = temp;
        }

        int i = 0;
        j = 0;
        char[] iInputChar = input.toCharArray();
        char[] iOutputChar = new char[iInputChar.length];
        for (short x = 0; x < iInputChar.length; x++) {
            i = (i + 1) % 256;
            j = (j + iS[i]) % 256;
            int temp = iS[i];
            iS[i] = iS[j];
            iS[j] = temp;
            int t = (iS[i] + (iS[j] % 256)) % 256;
            int iY = iS[t];
            char iCY = (char) iY;
            iOutputChar[x] = (char) (iInputChar[x] ^ iCY);
        }
        return new String(iOutputChar);
    }


    /** 生成 md5 值(16 位) */
    public static String to16Md5(String src) {
        return to16Md5(src, StandardCharsets.UTF_8);
    }
    /** 生成 md5 值(16 位), 指定 src 的字符集 */
    public static String to16Md5(String src, Charset charset) {
        return toMd5(src, charset).substring(8, 24);
    }
    /** 生成 md5 值(32 位) */
    public static String toMd5(String src) {
        return toMd5(src, StandardCharsets.UTF_8);
    }
    /** 生成 md5 值(32 位), 指定 src 的字符集 */
    public static String toMd5(String src, Charset charset) {
        return toHash(src, charset, "md5");
    }
    /** 生成 sha-1 值(40 位) */
    public static String toSha1(String src) {
        return toSha1(src, StandardCharsets.UTF_8);
    }
    /** 生成 sha-1 值(40 位), 指定 src 的字符集 */
    public static String toSha1(String src, Charset charset) {
        return toHash(src, charset, "sha-1");
    }
    /** 生成 sha-224 值(56 位) */
    public static String toSha224(String src) {
        return toSha224(src, StandardCharsets.UTF_8);
    }
    /** 生成 sha-224 值(56 位), 指定 src 的字符集 */
    public static String toSha224(String src, Charset charset) {
        return toHash(src, charset, "sha-224");
    }
    /** 生成 sha-256 值(64 位) */
    public static String toSha256(String src) {
        return toSha256(src, StandardCharsets.UTF_8);
    }
    /** 生成 sha-256 值(64 位), 指定 src 的字符集 */
    public static String toSha256(String src, Charset charset) {
        return toHash(src, charset, "sha-256");
    }
    /** 生成 sha-384 值(96 位) */
    public static String toSha384(String src) {
        return toSha384(src, StandardCharsets.UTF_8);
    }
    /** 生成 sha-384 值(96 位), 指定 src 的字符集 */
    public static String toSha384(String src, Charset charset) {
        return toHash(src, charset, "sha-384");
    }
    /** 生成 sha-512 值(128 位) */
    public static String toSha512(String src) {
        return toSha512(src, StandardCharsets.UTF_8);
    }
    /** 生成 sha-512 值(128 位), 指定 src 的字符集 */
    public static String toSha512(String src, Charset charset) {
        return toHash(src, charset, "sha-512");
    }
    private static String toHash(String src, Charset charset, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(src.getBytes(charset));
            return binary2Hex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(String.format("无法给(%s)生成 %s 值", src, algorithm), e);
        }
    }


    /** 生成文件的 md5 值(32 位) */
    public static String toMd5File(String file) {
        return toHashFile(file, "md5");
    }
    /** 生成文件的 sha-1 值(40 位) */
    public static String toSha1File(String file) {
        return toHashFile(file, "sha-1");
    }
    /** 生成文件的 sha-224 值(56 位) */
    public static String toSha224File(String file) {
        return toHashFile(file, "sha-224");
    }
    /** 生成文件的 sha-256 值(64 位) */
    public static String toSha256File(String file) {
        return toHashFile(file, "sha-256");
    }
    /** 生成文件的 sha-384 值(96 位) */
    public static String toSha384File(String file) {
        return toHashFile(file, "sha-384");
    }
    /** 生成文件的 sha-512 值(128 位) */
    public static String toSha512File(String file) {
        return toHashFile(file, "sha-512");
    }
    private static String toHashFile(String file, String algorithm) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            int len, count = 1024;
            byte[] buffer = new byte[count];
            while ((len = in.read(buffer, 0, count)) != -1) {
                md.update(buffer, 0, len);
            }
            return binary2Hex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(String.format("无法生成文件(%s)的 %s 值", file, algorithm), e);
        }
    }


    /** 基于密钥生成 hmac-md5 值(32 位) */
    public static String toHmacMd5(String src, String secret) {
        return toHmacHash(src, "HmacMD5", secret);
    }
    /** 基于密钥生成 hmac-sha-1 值(40 位) */
    public static String toHmacSha1(String src, String secret) {
        return toHmacHash(src, "HmacSHA1", secret);
    }
    /** 基于密钥生成 hmac-sha-224 值(56 位) */
    public static String toHmacSha224(String src, String secret) {
        return toHmacHash(src, "HmacSHA224", secret);
    }
    /** 基于密钥生成 hmac-sha-256 值(64 位) */
    public static String toHmacSha256(String src, String secret) {
        return toHmacHash(src, "HmacSHA256", secret);
    }
    /** 基于密钥生成 hmac-sha-384 值(96 位) */
    public static String toHmacSha384(String src, String secret) {
        return toHmacHash(src, "HmacSHA384", secret);
    }
    /** 基于密钥生成 hmac-sha-512 值(128 位) */
    public static String toHmacSha512(String src, String secret) {
        return toHmacHash(src, "HmacSHA512", secret);
    }
    private static String toHmacHash(String src, String algorithm, String secret) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(), algorithm));
            return binary2Hex(mac.doFinal(src.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("无法基于(%s)给(%s)生成 %s 值", secret, src, algorithm), e);
        }
    }


    /** 二进制 转换成 十六进制字符串 */
    public static String binary2Hex(byte[] bytes) {
        StringBuilder sbd = new StringBuilder();
        for (byte b : bytes) {
            sbd.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sbd.toString();
    }
    /** 十六进制字符串 转换成 二进制 */
    public static byte[] hex2Binary(String data) {
        byte[] bt = data.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[bt.length / 2];
        for (int n = 0; n < bt.length; n += 2) {
            String item = new String(bt, n, 2);
            bytes[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        return bytes;
    }
}
