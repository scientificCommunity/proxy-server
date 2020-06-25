package com.baichuan.proxy.service;

import com.baichuan.proxy.domain.ProxyConfig;
import com.baichuan.proxy.exception.ProxyServerInitException;
import com.baichuan.proxy.utils.CertUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * @author kun
 * @date 2020-06-11 17:30
 */
public class ProxyServer {
    private ProxyConfig proxyConfig;

    public ProxyServer init() {
        try {
            X509Certificate caCert = CertFactory.loadCert();
            PrivateKey caPriKey = CertFactory.loadPriKey();

            //生产一对随机公私钥用于网站SSL证书动态创建
            KeyPair keyPair = CertFactory.getKeyPair();

            proxyConfig = ProxyConfig.builder()
                    .clientSslCtx(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build())
                    .issuer(CertUtils.getSubject(caCert))
                    .caNotBefore(caCert.getNotBefore())
                    .caNotAfter(caCert.getNotAfter())
                    .caPriKey(caPriKey)
                    .privateKey(keyPair.getPrivate())
                    .publicKey(keyPair.getPublic())
                    .build();
        } catch (Exception e) {
            throw new ProxyServerInitException(e);
        }
        return this;
    }

    public void start(int port) {
        ServerBootstrap serverEndpoint = new ServerBootstrap();
        EventLoopGroup eventExecutors = new NioEventLoopGroup();
        try {
            serverEndpoint.group(eventExecutors)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 200)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            sc.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new ServerHandler(eventExecutors, proxyConfig));
                        }
                    });

            ChannelFuture bindFuture = serverEndpoint.bind(port).syncUninterruptibly();
            bindFuture.channel().closeFuture().syncUninterruptibly();
        } finally {
            eventExecutors.shutdownGracefully();
        }
    }
}
