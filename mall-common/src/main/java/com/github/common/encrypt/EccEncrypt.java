package com.github.common.encrypt;

import com.github.common.util.U;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public final class EccEncrypt {

    private static final String ECC = "EC";
    /** 用来做数据验签时的算法 */
    private static final String ECC_SIGN = "SHA256withECDSA";

    // 支持 ECC 算法的依赖. 见: https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyPair genericEccKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ECC, "BC");
            // 曲线名
            // secp256r1 [NIST P-256,X9.62 prime256v1] (1.2.840.10045.3.1.7)
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("用 ECC 生成密钥对时异常", e);
        }
    }

    public static KeyPair generateEccKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("用 ECC 生成密钥对时异常", e);
        }
    }

    public static String publicKeyToEccStr(PublicKey key) {
        return Encrypt.binary2Hex(((BCECPublicKey) key).getQ().getEncoded(false));
    }
    public static String privateKeyToEccStr(PrivateKey key) {
        return ((ECPrivateKey) key).getS().toString(16);
    }

    // 辅助：Hex 转 PublicKey
    private static PublicKey eccStrToPublicKey(String publicKey) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC", "BC");
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(spec.getCurve().decodePoint(Hex.decode(publicKey)), spec);
        return kf.generatePublic(pubSpec);
    }

    // 辅助：Hex 转 PrivateKey
    private static PrivateKey eccStrPrivateKey(String privateKey) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC", "BC");
        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256r1");
        ECPrivateKeySpec priSpec = new ECPrivateKeySpec(new BigInteger(privateKey, 16), spec);
        return kf.generatePrivate(priSpec);
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
    public static String eccEncode(String publicKey, String source) {
        if (U.isBlank(publicKey) || source == null) {
            throw new RuntimeException(String.format("用 ECC 基于公钥(%s)加密(%s)时数据不能为空", publicKey, source));
        }
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = keyGen.generateKeyPair();

            KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
            ka.init(kp.getPrivate());
            ka.doPhase(eccStrToPublicKey(publicKey), true);
            byte[] sharedSecret = ka.generateSecret();

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));

            String ephemPub = Hex.toHexString(((BCECPublicKey) kp.getPublic()).getQ().getEncoded(false));
            String ivHex = Hex.toHexString(iv);
            String ciphertext = Base64.getEncoder().encodeToString(cipherBytes);
            return ephemPub + ivHex + ciphertext;
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
    public static String eccDecode(String privateKey, String encryptData) {
        if (U.isBlank(privateKey) || U.isBlank(encryptData)) {
            throw new RuntimeException(String.format("用 ECC 基于私钥(%s)解密(%s)时数据不能为空", privateKey, encryptData));
        }
        if (encryptData.length() < 162) {
            throw new RuntimeException(String.format("用 ECC 基于解密(%s)时长度不足", encryptData));
        }

        try {
            String ephemPub = encryptData.substring(0, 130);
            String ivHex = encryptData.substring(130, 162);
            String ciphertext = encryptData.substring(162);

            KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
            ka.init(eccStrPrivateKey(privateKey));
            ka.doPhase(eccStrToPublicKey(ephemPub), true);
            byte[] sharedSecret = ka.generateSecret();

            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(Hex.decode(ivHex)));
            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
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
     * 当前方法 用于数据验签 中的服务端操作: 使用 ecc 的私钥加密原文, 生成签名</pre>
     *
     * @param privateKey 私钥
     * @param source 原文
     * @return 签名数据
     */
    public static String eccSign(String privateKey, String source) {
        if (U.isBlank(privateKey) || source == null) {
            throw new RuntimeException(String.format("用 ECC 基于私钥(%s)生成验签时数据(%s)不能为空", privateKey, source));
        }
        try {
            Signature s = Signature.getInstance("SHA256withECDSA", "BC");
            s.initSign(eccStrPrivateKey(privateKey));
            s.update(source.getBytes(StandardCharsets.UTF_8));
            return Hex.toHexString(s.sign());
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
     * @param sign 签名
     * @return true 表示验签成功
     */
    public static boolean eccVerify(String publicKey, String source, String sign) {
        if (U.isBlank(publicKey) || U.isBlank(source) || U.isBlank(sign)) {
            return false;
        }
        try {
            Signature s = Signature.getInstance("SHA256withECDSA", "BC");
            s.initVerify(eccStrToPublicKey(publicKey));
            s.update(source.getBytes(StandardCharsets.UTF_8));
            return s.verify(Hex.decode(sign));
        } catch (Exception e) {
            return false;
        }
    }
}
