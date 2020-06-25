package com.baichuan.proxy.service.listener;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kun
 * @date 2020-06-11 17:42
 */
@Slf4j
public class ForwardListener {
    private final ChannelHandlerContext clientCtx;

    public ForwardListener(ChannelHandlerContext clientCtx) {
        this.clientCtx = clientCtx;
    }

    public void writeAndFlush(Object msg) {
        if (!clientCtx.channel().isOpen()) {
            ReferenceCountUtil.release(msg);
            log.warn("与客户端的连接已关闭！！");
            return;
        }

        clientCtx.writeAndFlush(msg);

        if (msg instanceof HttpResponse) {
            HttpResponse httpResponse = (HttpResponse) msg;
            if (HttpHeaderValues.WEBSOCKET.toString().equals(httpResponse.headers().get(HttpHeaderNames.UPGRADE))) {
                clientCtx.pipeline().remove(HttpServerCodec.class);
            }
        }
    }

    public void channelUnregistered() {
        clientCtx.channel().close();
    }

    public void exceptionCaught(Throwable cause) {
        clientCtx.channel().close();
        cause.printStackTrace();
    }
}
