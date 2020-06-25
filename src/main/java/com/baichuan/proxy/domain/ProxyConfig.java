package com.baichuan.proxy.domain;

import io.netty.handler.ssl.SslContext;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

/**
 * @author kun
 * @date 2020-06-12 16:22
 */
@Getter
@Builder
public class ProxyConfig {
    /**
     * CA证书发布者名称
     */
    private String issuer;

    /**
     * CA私钥用于给动态生成的网站SSL证书签证
     */
    private PrivateKey caPriKey;

    /**
     * 随机公私钥用于网站SSL证书动态创建
     */
    private PrivateKey privateKey;

    private PublicKey publicKey;

    /**
     * CA证书有效时段(server证书有效期超出CA证书的，在手机上会提示证书不安全)
     */
    private Date caNotBefore;

    private Date caNotAfter;

    private SslContext clientSslCtx;
}
