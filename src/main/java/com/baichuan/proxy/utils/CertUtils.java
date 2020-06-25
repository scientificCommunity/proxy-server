package com.baichuan.proxy.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author kun
 * @date 2020-06-12 17:51
 */
public class CertUtils {

    private static volatile KeyFactory keyFactory;

    private static KeyFactory getKeyFactory() throws NoSuchAlgorithmException {
        if (keyFactory == null) {
            synchronized (CertUtils.class) {
                if (keyFactory == null) {
                    keyFactory = KeyFactory.getInstance("RSA");
                }
            }
        }
        return keyFactory;
    }

    public static X509Certificate load(String path) throws CertificateException, IOException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(Files.newInputStream(Paths.get(path)));
    }

    public static PrivateKey loadPriKey(String path) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        return loadPriKey(Files.readAllBytes(Paths.get(path)));
    }

    /**
     * 读取ssl证书使用者信息
     */
    public static String getSubject(X509Certificate certificate) {
        //读出来顺序是反的需要反转下
        List<String> tempList = Arrays.asList(certificate.getIssuerDN().toString().split(", "));
        return IntStream.rangeClosed(0, tempList.size() - 1)
                .mapToObj(i -> tempList.get(tempList.size() - i - 1)).collect(Collectors.joining(", "));
    }

    public static PrivateKey loadPriKey(byte[] bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);
        return getKeyFactory().generatePrivate(privateKeySpec);
    }
}
