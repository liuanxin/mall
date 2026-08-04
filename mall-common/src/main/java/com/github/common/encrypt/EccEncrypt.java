package com.github.common.encrypt;

import com.github.common.util.LogUtil;
import com.github.common.util.Obj;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.*;
import java.util.Base64;
import java.util.HexFormat;

/**
 * <pre>
 * ECC 加密工具类.
 *
 * 在需要客户端加密服务端解密的场景时, 客户端会在最开始请求服务端获取到配置的公钥并保存到本地缓存,
 * 后续在需要加密的接口上使用公钥加密, 服务端用配置的私钥解密.
 *
 * 在需要服务端加密客户端解密的场景时, 客户端在请求前生成临时密钥对,
 * 将临时公钥传递到服务端, 服务端使用公钥加密后返回数据, 客户端用临时私钥解密.
 * </pre>
 */
public final class EccEncrypt {

    /*
    // ecc.js

    // 采用 ECC + AES 混合加密模式
    // 依赖安装: npm install @noble/curves aes-js

    import { p256 } from '@noble/curves/nist.js'
    import aesjs from 'aes-js'

    // true 表示打印日志, 线上跑时一定要改成 false
    const NOT_ONLINE = true

    // 十六进制转换成字节数组
    export const hexToBytes = (hex) => {
        if (!hex) return new Uint8Array(0)
        const bytes = new Uint8Array(hex.length / 2)
        for (let i = 0, j = 0; i < hex.length; i += 2) {
            bytes[j++] = parseInt(hex.substring(i, i + 2), 16)
        }
        return bytes
    }

    // 生成随机数
    export const randomBytes = (length) => {
        const bytes = new Uint8Array(length)
        if (globalThis.crypto && globalThis.crypto.getRandomValues) {
            globalThis.crypto.getRandomValues(bytes)
        } else {
            if (NOT_ONLINE) {
                console.error("Crypto 环境未准备好! 需要在微信层面处理一下? 见下面日志\n" +
                    "// 微信小游戏环境补丁: 在游戏初始化(比如 Loading 脚本)时跑一次这段代码\n" +
                    "if (typeof wx !== 'undefined' && wx.getRandomValues) {\n" +
                    "    if (typeof globalThis !== 'undefined' && !globalThis.crypto) {\n" +
                    "        // @ts-ignore\n" +
                    "        globalThis.crypto = {\n" +
                    "            getRandomValues: (arr) => {\n" +
                    "                const buffer = wx.getRandomValues({ length: arr.length });\n" +
                    "                arr.set(new Uint8Array(buffer));\n" +
                    "                return arr;\n" +
                    "            }\n" +
                    "        };\n" +
                    "    }\n" +
                    "}")
            }
            // 兜底逻辑：填充伪随机
            for (let i = 0; i < length; i++) {
                bytes[i] = Math.floor(Math.random() * 256);
            }
        }
        return bytes
    }

    // 强制补零，确保 Hex 长度对齐
    const forceHex = (bytes) => {
        return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('')
    }

    // 替换 new TextEncoder().encode
    const strToU8 = (str) => {
        const bytes = []
        for (let i = 0; i < str.length; i++) {
            let code = str.charCodeAt(i)
            if (code < 0x80) {
                bytes.push(code)
            } else if (code < 0x800) {
                bytes.push(0xc0 | (code >> 6), 0x80 | (code & 0x3f))
            } else if (code >= 0xd800 && code <= 0xdbff) {
                if (i + 1 < str.length) {
                    const next = str.charCodeAt(++i)
                    const cp = 0x10000 + ((code - 0xd800) << 10) + (next - 0xdc00)
                    bytes.push(0xf0 | (cp >> 18), 0x80 | ((cp >> 12) & 0x3f), 0x80 | ((cp >> 6) & 0x3f), 0x80 | (cp & 0x3f))
                }
            } else {
                bytes.push(0xe0 | (code >> 12), 0x80 | ((code >> 6) & 0x3f), 0x80 | (code & 0x3f))
            }
        }
        return new Uint8Array(bytes)
    }
    // 替换 new TextDecoder().decode
    const u8ToStr = (bytes) => {
        let str = ''
        for (let i = 0; i < bytes.length; i++) {
            let b1 = bytes[i]
            if (b1 < 0x80) {
                str += String.fromCharCode(b1)
            } else if (b1 < 0xe0) {
                let b2 = bytes[++i]
                str += String.fromCharCode(((b1 & 0x1f) << 6) | (b2 & 0x3f))
            } else if (b1 < 0xf0) {
                let b2 = bytes[++i]
                let b3 = bytes[++i]
                str += String.fromCharCode(((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f))
            } else {
                let b2 = bytes[++i]
                let b3 = bytes[++i]
                let b4 = bytes[++i]
                let cp = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f)
                if (cp <= 0xffff) {
                    str += String.fromCharCode(cp)
                } else {
                    cp -= 0x10000
                    str += String.fromCharCode((cp >> 10) | 0xd800, (cp & 0x3ff) | 0xdc00)
                }
            }
        }
        return str
    }

    const CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    // Base64 编码(不依赖 btoa)
    const bytesToBase64 = (bytes) => {
        let base64 = ""
        for (let i = 0; i < bytes.length; i += 3) {
            const chunk = ((bytes[i] << 16) | (bytes[i + 1] << 8) | (bytes[i + 2] || 0)) >>> 0
            base64 += CHARS[(chunk >> 18) & 63] + CHARS[(chunk >> 12) & 63] + CHARS[(chunk >> 6) & 63] + CHARS[chunk & 63]
        }
        const p = bytes.length % 3
        if (p === 1) {
            return base64.slice(0, -2) + "=="
        } else if (p === 2) {
            return base64.slice(0, -1) + "="
        } else {
            return base64
        }
    }
    // Base64 解码(不依赖 atob)
    const base64ToBytes = (base64) => {
        const lookup = new Uint8Array(256)
        for (let i = 0; i < CHARS.length; i++) {
            lookup[CHARS.charCodeAt(i)] = i
        }

        const buffer = base64.replace(/=/g, "")
        const n = buffer.length
        const bytes = new Uint8Array((n * 3) / 4)
        for (let i = 0, j = 0; i < n; i += 4) {
            const w1 = lookup[buffer.charCodeAt(i)]
            const w2 = lookup[buffer.charCodeAt(i + 1)]
            const w3 = lookup[buffer.charCodeAt(i + 2)]
            const w4 = lookup[buffer.charCodeAt(i + 3)]
            bytes[j++] = (w1 << 2) | (w2 >> 4)
            if (j < bytes.length) {
                bytes[j++] = ((w2 & 15) << 4) | (w3 >> 2)
            }
            if (j < bytes.length) {
                bytes[j++] = ((w3 & 3) << 6) | w4
            }
        }
        return bytes
    }

    // 生成密钥对
    export const eccGenerateKey = () => {
        const privateKey = randomBytes(32)
        return {
            priKey: forceHex(privateKey),
            pubKey: forceHex(p256.getPublicKey(privateKey, false))
        }
    }

    // 公钥加密
    export const eccEncrypt = (pubKeyHex, strData) => {
        const ephemPrivateKey = randomBytes(32)
        const ephemPubKey = p256.getPublicKey(ephemPrivateKey, false)
        const sharedSecret = p256.getSharedSecret(ephemPrivateKey, hexToBytes(pubKeyHex))
        const aesKey = sharedSecret.slice(1, 17)
        const ivBytes = randomBytes(16)
        const textBytes = strToU8(strData)
        const paddedBytes = aesjs.padding.pkcs7.pad(textBytes)
        const aesCbc = new aesjs.ModeOfOperation.cbc(aesKey, ivBytes)
        const encryptedBytes = aesCbc.encrypt(paddedBytes)
        const ciphertextBase64 = bytesToBase64(encryptedBytes)
        const result = forceHex(ephemPubKey) + forceHex(ivBytes) + ciphertextBase64
        if (NOT_ONLINE) {
            console.log(`ECC 公钥(${pubKeyHex})加密(${strData}) -> (${result})`)
        }
        return result
    }
    // 私钥解密
    export const eccDecrypt = (priKeyHex, encryptData) => {
        if (!encryptData || encryptData.length < 162) {
            return null
        }
        const ephemPubKeyHex = encryptData.substring(0, 130)
        const ivHex = encryptData.substring(130, 162)
        const ciphertextBase64 = encryptData.substring(162)
        const sharedSecret = p256.getSharedSecret(hexToBytes(priKeyHex), hexToBytes(ephemPubKeyHex))
        const aesKey = sharedSecret.slice(1, 17)
        const iv = hexToBytes(ivHex)
        const ciphertextBytes = base64ToBytes(ciphertextBase64)
        const aesCbc = new aesjs.ModeOfOperation.cbc(aesKey, iv)
        const decryptedBytes = aesCbc.decrypt(ciphertextBytes)
        const unpaddedBytes = aesjs.padding.pkcs7.strip(decryptedBytes)
        const result = u8ToStr(unpaddedBytes)
        if (NOT_ONLINE) {
            console.log(`ECC 私钥(${priKeyHex})解密(${encryptData}) -> (${result})`)
        }
        return result
    }

    // 用私钥生成签名
    export const eccSign = (priKeyHex, data) => {
        const msgBytes = strToU8(data)
        const sigBytes = p256.sign(msgBytes, hexToBytes(priKeyHex))
        // sigBytes 是 Uint8Array(64), 前 32 位是 R, 后 32 位是 S
        const rBytes = sigBytes.slice(0, 32)
        const sBytes = sigBytes.slice(32, 64)

        const toDerPart = (bytes) => {
            let hex = forceHex(bytes)
            hex = hex.replace(/^0+/, '')
            if (hex === '' || parseInt(hex.substring(0, 2), 16) >= 0x80) {
                hex = '00' + hex
            }
            if (hex.length % 2 !== 0) {
                hex = '0' + hex
            }
            return hex
        }
        const rH = toDerPart(rBytes)
        const sH = toDerPart(sBytes)
        const rL = (rH.length / 2).toString(16).padStart(2, '0')
        const sL = (sH.length / 2).toString(16).padStart(2, '0')
        const totalL = (rH.length / 2 + sH.length / 2 + 4).toString(16).padStart(2, '0')
        const signHex = `30${totalL}02${rL}${rH}02${sL}${sH}`
        if (NOT_ONLINE) {
            console.log(`ECC 私钥(${priKeyHex})给数据(${data})生成签名(${signHex})`)
        }
        return signHex
    }
    // 用公钥验签
    export const eccVerify = (pubKeyHex, data, signHex) => {
        let signBytes = hexToBytes(signHex)
        if (signBytes[0] === 0x30) {
            const raw = new Uint8Array(64)
            const extract = (start) => {
                let len = signBytes[start + 1]
                let val = signBytes.slice(start + 2, start + 2 + len)
                if (val[0] === 0x00 && val.length > 32) val = val.slice(1)
                const out = new Uint8Array(32)
                out.set(val, 32 - val.length)
                return { out, next: start + 2 + len }
            }
            const rPart = extract(2)
            const sPart = extract(rPart.next)
            raw.set(rPart.out, 0)
            raw.set(sPart.out, 32)
            signBytes = raw
        }
        try {
            const verify = p256.verify(signBytes, strToU8(data), hexToBytes(pubKeyHex))
            if (NOT_ONLINE) {
                console.log(`ECC 公钥(${pubKeyHex})给数据(${data})验签(${signHex}) -> (${verify})`)
            }
            return verify
        } catch (e) {
            if (NOT_ONLINE) {
                console.log(`ECC 公钥(${pubKeyHex})给数据(${data})验签(${signHex})异常.`, e)
            }
            return false
        }
    }

    // 下面是 ECC 示例

    export const testEcc = () => {
        // 1. 初始化并生成密钥对
        const pair = eccGenerateKey()
        const publicKey = pair.pubKey
        const privateKey = pair.priKey

        console.log('--- ECC ---')
        console.log('公钥:', publicKey)
        console.log('私钥:', privateKey)

        const originalData = '{"key":"Hello ECC 中文 2026!"}'
        console.log('原文:', originalData)

        // 公钥加密
        let start = Date.now()
        const encrypted = eccEncrypt(publicKey, originalData)
        console.log(`公钥加密后: ${encrypted}, 耗时:(${Date.now() - start})`)
        // 私钥解密
        start = Date.now()
        const decrypted = eccDecrypt(privateKey, encrypted)
        console.log(`私钥解密后: ${decrypted}, 耗时:(${Date.now() - start})`)

        // 私钥签名
        start = Date.now()
        const sign = eccSign(privateKey, originalData)
        console.log(`私钥生成签名: ${sign}, 耗时:(${Date.now() - start})`)
        // 公钥校验
        start = Date.now()
        const v = eccVerify(publicKey, originalData, sign)
        console.log(`公钥验证签名: ${v}, 耗时:(${Date.now() - start})`)
        console.log('--- ECC ---')
    }
    */

    public static KeyPair generateEccKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            // 曲线名 secp256r1 [NIST P-256,X9.62 prime256v1] (1.2.840.10045.3.1.7)
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("用 ECC 生成密钥对时异常", e);
        }
    }

    public static String publicKeyToEccStr(PublicKey key) {
        ECPoint point = ((ECPublicKey) key).getW();
        return "04" + toFixedHex(point.getAffineX()) + toFixedHex(point.getAffineY());
    }
    public static String privateKeyToEccStr(PrivateKey key) {
        return toFixedHex(((ECPrivateKey) key).getS());
    }

    // 辅助：Hex 转 PublicKey
    private static PublicKey eccStrToPublicKey(String publicKey) throws Exception {
        byte[] bytes = HexFormat.of().parseHex(publicKey);
        if (bytes.length != 65 || bytes[0] != 0x04) {
            throw new IllegalArgumentException("ECC 公钥格式错误");
        }
        BigInteger x = new BigInteger(1, bytes, 1, 32);
        BigInteger y = new BigInteger(1, bytes, 33, 32);
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(new ECPoint(x, y), getEccParameterSpec());
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePublic(pubSpec);
    }

    // 辅助：Hex 转 PrivateKey
    private static PrivateKey eccStrPrivateKey(String privateKey) throws Exception {
        ECPrivateKeySpec priSpec = new ECPrivateKeySpec(new BigInteger(privateKey, 16), getEccParameterSpec());
        KeyFactory kf = KeyFactory.getInstance("EC");
        return kf.generatePrivate(priSpec);
    }

    private static ECParameterSpec getEccParameterSpec() throws Exception {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        return parameters.getParameterSpec(ECParameterSpec.class);
    }

    private static String toFixedHex(BigInteger value) {
        return String.format("%064x", value);
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
        if (Obj.isBlank(publicKey) || source == null) {
            LogUtil.ROOT_LOG.error("用 ECC 基于公钥({})加密({})时数据有误", publicKey, source);
            throw new RuntimeException(String.format("加密时数据(%s)有误", source));
        }
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = keyGen.generateKeyPair();

            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(kp.getPrivate());
            ka.doPhase(eccStrToPublicKey(publicKey), true);
            byte[] sharedSecret = ka.generateSecret();

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));

            String ephemPub = publicKeyToEccStr(kp.getPublic());
            String ivHex = HexFormat.of().formatHex(iv);
            String ciphertext = Base64.getEncoder().encodeToString(cipherBytes);
            return ephemPub + ivHex + ciphertext;
        } catch (Exception e) {
            LogUtil.ROOT_LOG.error("用 ECC 基于公钥({})加密({})时异常", publicKey, source);
            throw new RuntimeException(String.format("加密(%s)时异常", source), e);
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
        if (Obj.isBlank(privateKey) || Obj.isBlank(encryptData) || encryptData.length() < 162) {
            LogUtil.ROOT_LOG.error("用 ECC 基于私钥({})解密({})时数据有误",
                    Obj.foggyValue(privateKey, 12, 4), encryptData);
            throw new RuntimeException(String.format("解密(%s)时数据有误", encryptData));
        }

        try {
            String ephemPub = encryptData.substring(0, 130);
            String ivHex = encryptData.substring(130, 162);
            String ciphertext = encryptData.substring(162);

            KeyAgreement ka = KeyAgreement.getInstance("ECDH");
            ka.init(eccStrPrivateKey(privateKey));
            ka.doPhase(eccStrToPublicKey(ephemPub), true);
            byte[] sharedSecret = ka.generateSecret();

            SecretKeySpec aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(HexFormat.of().parseHex(ivHex)));
            return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogUtil.ROOT_LOG.error("用 ECC 基于私钥({})解密({})时异常",
                    Obj.foggyValue(privateKey, 12, 4), encryptData);
            throw new RuntimeException(String.format("解密(%s)时异常", encryptData), e);
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
        if (Obj.isBlank(privateKey) || source == null) {
            LogUtil.ROOT_LOG.error("用 ECC 基于私钥({})生成验签时数据({})有误",
                    Obj.foggyValue(privateKey, 12, 4), source);
            throw new RuntimeException("生成验签时数据有误");
        }
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(eccStrPrivateKey(privateKey));
            s.update(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(s.sign());
        } catch (Exception e) {
            LogUtil.ROOT_LOG.error("用 ECC 基于私钥({})生成验签时({})异常",
                    Obj.foggyValue(privateKey, 12, 4), source);
            throw new RuntimeException("生成验签时异常", e);
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
        if (Obj.isBlank(publicKey) || Obj.isBlank(source) || Obj.isBlank(sign)) {
            return false;
        }
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(eccStrToPublicKey(publicKey));
            s.update(source.getBytes(StandardCharsets.UTF_8));
            return s.verify(HexFormat.of().parseHex(sign));
        } catch (Exception e) {
            return false;
        }
    }
}
