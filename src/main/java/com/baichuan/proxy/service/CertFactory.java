package com.baichuan.proxy.service;

import com.baichuan.proxy.domain.ProxyConfig;
import com.baichuan.proxy.exception.GenCertException;
import com.baichuan.proxy.utils.CertUtils;
import com.baichuan.proxy.utils.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.X509V3CertificateGenerator;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

import static javax.management.timer.Timer.ONE_DAY;

/**
 * @author kun
 * @date 2020-06-07 20:42
 */
@Slf4j
public class CertFactory {
    public static final PrivateKey PRIVATE_KEY;
    public static final PublicKey PUBLIC_KEY;

    public static X509Certificate certificate;

    private volatile static KeyPair keyPair;

    public static final Map<String, X509Certificate> CACHE;

    static {
        //注册bouncycastle
        Security.addProvider(new BouncyCastleProvider());
        //生成ssl证书公钥和私钥
        KeyPairGenerator caKeyPairGen = null;
        KeyPair keyPair = null;
        try {
            caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
            caKeyPairGen.initialize(2048, new SecureRandom());
            keyPair = caKeyPairGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        PRIVATE_KEY = keyPair.getPrivate();
        PUBLIC_KEY = keyPair.getPublic();
        CACHE = new HashMap<>();
    }

    public static void main(String[] args) throws Exception {
        //X509Certificate localhost = genCert(PUBLIC_KEY, PRIVATE_KEY, "localhost");
    }

    public static KeyPair getKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException {
        if (keyPair == null) {
            synchronized (CertUtils.class) {
                if (keyPair == null) {
                    KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
                    caKeyPairGen.initialize(2048, new SecureRandom());
                    keyPair = caKeyPairGen.generateKeyPair();
                }
            }
        }
        return keyPair;
    }

    public static X509Certificate getCert() {
        if (certificate == null) {
            try {
                certificate = genCert(PUBLIC_KEY, PRIVATE_KEY, "localhost");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return certificate;
    }

    //通过CA私钥动态签发ssl证书
    public static X509Certificate genCert(PublicKey serverPubKey, PrivateKey caPriKey, String host) throws Exception {
        X509V3CertificateGenerator v3CertGen = new X509V3CertificateGenerator();
        String issuer = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyeeRoot";
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host;
        v3CertGen.reset();
        v3CertGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        v3CertGen.setIssuerDN(new X509Principal(issuer));
        v3CertGen.setNotBefore(new Date(System.currentTimeMillis() - 10 * ONE_DAY));
        v3CertGen.setNotAfter(new Date(System.currentTimeMillis() + 3650 * ONE_DAY));
        v3CertGen.setSubjectDN(new X509Principal(subject));
        v3CertGen.setPublicKey(serverPubKey);
        //SHA256 Chrome需要此哈希算法否则会出现不安全提示
        v3CertGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
        //SAN扩展 Chrome需要此扩展否则会出现不安全提示
        GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, host));
        v3CertGen.addExtension(X509Extensions.SubjectAlternativeName, false, (ASN1Encodable) subjectAltName);
        return v3CertGen.generateX509Certificate(caPriKey);
    }

    public static X509Certificate loadCert() throws CertificateException, IOException {
        return CertUtils.load(PathUtils.ROOT_PATH + "/cert/baichuan.crt");
    }

    public static PrivateKey loadPriKey() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        return CertUtils.loadPriKey(PathUtils.ROOT_PATH + "/cert/baichuan.der");
    }


    public static X509Certificate getCert(String host, ProxyConfig config) {
        if (StringUtils.isBlank(host)) {
            throw new IllegalArgumentException("host or port must not be null");
        }

        host = host.trim();
        //Map<Integer, X509Certificate> portCertMap = CACHE.computeIfAbsent(host, s -> new HashMap<>(16));
        X509Certificate certificate = CACHE.get(host);

        if (certificate == null) {
            try {
                certificate = genCert(config.getIssuer(), config.getCaPriKey(), config.getCaNotBefore(), config.getCaNotAfter(), config.getPublicKey(), host);
            } catch (Exception e) {
                log.error("证书生成失败");
                throw new GenCertException(e);
            }
            CACHE.put(host, certificate);
        }
        return certificate;
    }

    /**
     * 动态生成服务器证书,并进行CA签授
     *
     * @param issuer 颁发机构
     */
    public static X509Certificate genCert(String issuer, PrivateKey caPriKey, Date caNotBefore,
                                          Date caNotAfter, PublicKey serverPubKey,
                                          String... hosts) throws Exception {
        /* String issuer = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=ProxyeeRoot";
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + host;*/
        //根据CA证书subject来动态生成目标服务器证书的issuer和subject
        String subject = "C=CN, ST=GD, L=SZ, O=lee, OU=study, CN=" + hosts[0];
        //doc from https://www.cryptoworkshop.com/guide/
        JcaX509v3CertificateBuilder jv3Builder = new JcaX509v3CertificateBuilder(new X500Name(issuer),
                //issue#3 修复ElementaryOS上证书不安全问题(serialNumber为1时证书会提示不安全)，避免serialNumber冲突，采用时间戳+4位随机数生成
                BigInteger.valueOf(System.currentTimeMillis() + (long) (RandomUtils.nextInt(0, 10000)) + 1000),
                caNotBefore,
                caNotAfter,
                new X500Name(subject),
                serverPubKey);
        //SAN扩展证书支持的域名，否则浏览器提示证书不安全
        GeneralName[] generalNames = new GeneralName[hosts.length];
        for (int i = 0; i < hosts.length; i++) {
            generalNames[i] = new GeneralName(GeneralName.dNSName, hosts[i]);
        }
        GeneralNames subjectAltName = new GeneralNames(generalNames);
        jv3Builder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);
        //SHA256 用SHA1浏览器可能会提示证书不安全
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPriKey);
        return new JcaX509CertificateConverter().getCertificate(jv3Builder.build(signer));
    }
}
