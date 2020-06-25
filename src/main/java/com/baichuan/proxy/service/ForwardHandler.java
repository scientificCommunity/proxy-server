package com.baichuan.proxy.service;

import com.baichuan.proxy.service.listener.ForwardListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kun
 * @date 2020-06-11 17:45
 */
@Slf4j
public class ForwardHandler extends ChannelInboundHandlerAdapter {
    private final ForwardListener listener;

    public ForwardHandler(ForwardListener listener) {
        this.listener = listener;
    }

    /*@Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        ReferenceCountUtil.retain(o);
        this.listener.writeAndFlush(o);
        ReferenceCountUtil.retain(o);
    }*/

    @Override
    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        if (o instanceof ByteBuf) {
            ByteBuf byteBuf = ((ByteBuf) o).duplicate();
            int i = byteBuf.readableBytes();
            if (i > 0) {
                byte[] bytes = new byte[i];
                String s = new String(bytes);
                System.out.println(s);
            }

        }
        this.listener.writeAndFlush(o);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn("与服务端连接断开");
        super.channelInactive(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().close();
        listener.channelUnregistered();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        listener.exceptionCaught(cause);
    }
}
