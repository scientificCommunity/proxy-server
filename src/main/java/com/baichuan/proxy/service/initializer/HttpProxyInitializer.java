package com.baichuan.proxy.service.initializer;

import com.baichuan.proxy.domain.ProxyConfig;
import com.baichuan.proxy.domain.RequestBO;
import com.baichuan.proxy.service.ForwardHandler;
import com.baichuan.proxy.service.listener.ForwardListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kun
 * @date 2020-06-19 9:27
 */
@Slf4j
public class HttpProxyInitializer extends ChannelInitializer<Channel> {

    private final RequestBO requestBO;

    private final ForwardListener listener;

    private final ProxyConfig proxyConfig;

    public HttpProxyInitializer(RequestBO requestBO, ForwardListener listener, ProxyConfig proxyConfig) {
        this.requestBO = requestBO;
        this.listener = listener;
        this.proxyConfig = proxyConfig;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline()
                .addLast(new ForwardHandler(listener));
        if (requestBO.getSupportSsl()) {
            ch.pipeline()
                    .addFirst(new HttpClientCodec())
                    .addFirst(proxyConfig.getClientSslCtx().newHandler(ch.alloc(), requestBO.getHost(), requestBO.getPort()));
        }

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
        listener.channelUnregistered();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("连接断开");
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("=======================================读取服务端返回数据异常=======================================");
        ctx.channel().close();
        listener.exceptionCaught(cause);
        super.exceptionCaught(ctx, cause);
    }
}
