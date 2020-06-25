package com.baichuan.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * @author kun
 * @date 2020-06-12 9:44
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(-125 & 127);


    }
    public static void clientTest() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                        super.channelRead(ctx, msg);
                                    }
                                });
                    }
                });

        ChannelFuture connect = bootstrap.connect("google.com", 80);
        connect.addListener(future -> {
            if (connect.isSuccess()) {
                System.out.println("连接成功");
                String msg = "阿斯蒂芬";
                URI uri = new URI("http://www.google.com");
                DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                        uri.toASCIIString(), Unpooled.wrappedBuffer(msg.getBytes(StandardCharsets.UTF_8)));

                // 构建http请求
                request.headers().set(HttpHeaderNames.HOST, "google.com");
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
                ChannelFuture aaa = connect.channel().writeAndFlush(request);
                aaa.addListener(future1 -> {
                    if (aaa.isSuccess()) {
                        System.out.println("发送数据成功");
                    } else {
                        System.out.println("发送数据失败");
                    }
                });
            } else {
                System.out.println("连接失败");
            }
        });
    }
}
