package com.baichuan.proxy.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.baichuan.proxy.constants.RequestTimeStatus;
import com.baichuan.proxy.domain.ProxyConfig;
import com.baichuan.proxy.domain.RequestBO;
import com.baichuan.proxy.service.handler.codec.RequestBoCodec;
import com.baichuan.proxy.service.initializer.HttpProxyInitializer;
import com.baichuan.proxy.service.initializer.TunnelProxyInitializer;
import com.baichuan.proxy.service.listener.ForwardListener;
import com.baichuan.proxy.utils.RequestUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;

/**
 * @author kun
 * @date 2020-06-11 17:34
 */
@Slf4j
public class ServerHandlerNew extends ChannelInboundHandlerAdapter {
    private final EventLoopGroup eventExecutors;

    private ChannelFuture remoteServerCf;

    private static final String CONNECT_METHOD = "connect";

    private static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(200, "Connection established");

    private RequestBO requestBO;

    private boolean proxyConnected2Server;

    private int status;

    private final ProxyConfig proxyConfig;

    public ServerHandlerNew(EventLoopGroup eventExecutors, ProxyConfig proxyConfig) {
        this.eventExecutors = eventExecutors;
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof String && status == RequestTimeStatus.FIRST) {
            status = RequestTimeStatus.NOT_FIRST;
            try {
                this.requestBO = JSON.parseObject((String) msg, RequestBO.class);
            } catch (JSONException e) {
                log.warn("=========================JSON转换异常，msg：{}==================================", msg);
            }
            ctx.channel().pipeline().remove(RequestBoCodec.class);
            return;
        }
        boolean isHttp = false;
        if (msg instanceof HttpRequest) {
            isHttp = true;
        } else if (msg instanceof HttpContent) {
            HttpContent httpContent = ((HttpContent) msg).duplicate();

            int i = httpContent.content().readableBytes();
            if (i > 0) {
                byte[] bytes = new byte[i];
                httpContent.content().readBytes(bytes);
                String s = new String(bytes);
                System.out.println(s);
            }
            isHttp = true;
        }

        forward(new ForwardListener(ctx), msg, isHttp);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("=======================================读取客户端消息异常================================");
        ctx.channel().close();
        super.exceptionCaught(ctx, cause);
    }

    public void forward(ForwardListener listener, Object clientMsg, boolean isHttp) {
        if (remoteServerCf == null) {
            if (isHttp && !(clientMsg instanceof HttpRequest)) {
                return;
            }

            ChannelInitializer<Channel> initializer = isHttp ? new HttpProxyInitializer(requestBO, listener, proxyConfig)
                    : new TunnelProxyInitializer(listener);

            Bootstrap proxy2ServerEd = new Bootstrap();
            proxy2ServerEd.group(eventExecutors)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                    .handler(initializer);
            remoteServerCf = proxy2ServerEd.connect(this.requestBO.getHost(), this.requestBO.getPort());

            remoteServerCf.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    future.channel().writeAndFlush(clientMsg);
                    synchronized (unSendReqs) {
                        unSendReqs.forEach(o -> future.channel().writeAndFlush(o));
                        unSendReqs.clear();
                    }
                    proxyConnected2Server = true;
                } else {
                    log.error("proxy2server connection failed,host:{},port:{}", this.requestBO.getHost(), this.requestBO.getPort(), future.cause());
                    unSendReqs.forEach(ReferenceCountUtil::release);
                    unSendReqs.clear();
                    future.channel().close();
                }
            });
        } else {
            synchronized (unSendReqs) {
                if (proxyConnected2Server) {
                    remoteServerCf.channel().writeAndFlush(clientMsg);
                } else {
                    unSendReqs.add(clientMsg);
                }
            }
        }
    }

    /**
     * 顺序存取
     */
    private final List<Object> unSendReqs = new LinkedList<>();
}
