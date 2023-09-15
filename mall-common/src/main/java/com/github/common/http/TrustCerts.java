package com.github.common.http;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

class TrustCerts {

    /**
     * java --help 可见 「-D<name>=<value> set a system property」
     *
     * 设置了 -Dignore-https-ssl=1 将在请求 https 时忽略证书
     */
    static final boolean IGNORE_SSL = U.toBool(System.getProperty("ignore-https-ssl"));

    /** 请求 https 时无视 ssl 证书 */
    static final X509TrustManager TRUST_MANAGER;
    static final SSLContext IGNORE_SSL_CONTEXT;
    static final SSLSocketFactory IGNORE_SSL_FACTORY;

    static {
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext sc;
        SSLSocketFactory sf;
        try {
            sc = SSLContext.getInstance("SSLv3");
            sc.init(null, new TrustManager[] { tm }, null);
            sf = sc.getSocketFactory();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("设置忽略 ssl 证书异常", e);
            }
            sc = null;
            sf = null;
        }
        TRUST_MANAGER = tm;
        IGNORE_SSL_CONTEXT = sc;
        IGNORE_SSL_FACTORY = sf;
    }


    /**
     * <pre>
     * 见: https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html
     *
     * 生成 jks 文件
     * keytool -genkey -alias xxx-jks \
     *   -dname "CN=Tom Cat, OU=Java, O=Oracle, L=SZ, ST=GD, C=CN" \
     *   -keyalg RSA -keystore file.jks -validity 3600 \
     *   -storepass 123456 -keypass abcdef
     *
     * 转换成 pkcs12 标准格式
     * keytool -importkeystore -srcalias xxx-jks -destalias xxx-new-jks \
     *   -srckeystore file.jks -srcstoretype jks -destkeystore file-new.jks -deststoretype pkcs12 \
     *   -srcstorepass 123456 -srckeypass abcdef -deststorepass 123456 -destkeypass abcdef
     *
     *
     * storepass 用来访问密钥库, keypass 用来访问密钥库中具体的密钥. 通常建议保持一致
     * 如果 storepass 和 keypass 是一致的, -importkeystore 时则不需要 -destkeypass 选项, 下同
     *
     *
     * 使用旧的 jks 转换成 pfx 格式(IIS)
     * keytool -importkeystore -srcalias xxx-jks -destalias xxx-pfx \
     *   -srckeystore file.jks -srcstoretype jks -destkeystore file.pfx -deststoretype pkcs12 \
     *   -srcstorepass 123456 -srckeypass abcdef -deststorepass 123456 -destkeypass abcdef
     *
     * 使用新的 jks 转换成 pfx 格式
     * keytool -importkeystore -srcalias xxx-new-jks -destalias xxx-new-pfx \
     *   -srckeystore file-new.jks -srcstoretype pkcs12 -destkeystore file-new.pfx -deststoretype pkcs12 \
     *   -srcstorepass 123456 -srckeypass abcdef -deststorepass 123456 -destkeypass abcdef
     *
     *
     * 将 jks 导出成 crt 和 key 文件(apache 及 nginx), 提取时用 openssl 命令基于 pkcs12 标准格式的文件操作即可
     * 见: https://www.openssl.org/docs/man1.0.2/man1/pkcs12.html
     *
     * 提取 key: openssl pkcs12 -in file-new.jks -nocerts -nodes -out file.key -passin pass:123456
     * 提取 crt: openssl pkcs12 -in file-new.jks -nokeys -out file.crt -passin pass:123456
     * 将 crt 转换成 pem 格式: openssl x509 -in file.crt -out file.pem
     *
     * 基于 crt/pem 和 key 生成 jks: openssl pkcs12 -export -in file.(crt|pem) -inkey file.key -out file.jks -passout pass:123456
     * </pre>
     */
    static SSLContext createFileVerifySSL(String jksFile, String storePass, String keyPass) {
        try (InputStream keyStoreStream = new FileInputStream(jksFile)) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, storePass.toCharArray());
            return SSLContexts.custom().loadKeyMaterial(keyStore, keyPass.toCharArray()).build();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("设置本地 ssl 证书异常", e);
            }
            return null;
        }
    }
}
