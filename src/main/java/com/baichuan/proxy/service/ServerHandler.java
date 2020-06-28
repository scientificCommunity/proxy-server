package com.baichuan.proxy.service;

import com.baichuan.proxy.constants.RequestTimeStatus;
import com.baichuan.proxy.domain.ProxyConfig;
import com.baichuan.proxy.domain.RequestBO;
import com.baichuan.proxy.service.initializer.HttpProxyInitializer;
import com.baichuan.proxy.service.initializer.TunnelProxyInitializer;
import com.baichuan.proxy.service.listener.ForwardListener;
import com.baichuan.proxy.utils.FileTypeCheckUtil;
import com.baichuan.proxy.utils.RequestUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * @author kun
 * @date 2020-06-11 17:34
 */
@Slf4j
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final EventLoopGroup eventExecutors;

    private ChannelFuture remoteServerCf;

    private static final String CONNECT_METHOD = "connect";

    private static final HttpResponseStatus CONNECTION_ESTABLISHED = new HttpResponseStatus(200, "Connection established");

    private RequestBO requestBO;

    private boolean proxyConnected2Server;

    private int status;

    private final ProxyConfig proxyConfig;

    public ServerHandler(EventLoopGroup eventExecutors, ProxyConfig proxyConfig) {
        this.eventExecutors = eventExecutors;
        this.proxyConfig = proxyConfig;
    }

    /*@Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {*/
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        boolean isHttp = false;
        log.debug("读取到客户端消息");
        if (msg instanceof HttpRequest) {
            isHttp = true;
            HttpRequest req = (HttpRequest) msg;

            if (status == RequestTimeStatus.FIRST) {
                status = RequestTimeStatus.NOT_FIRST;
                this.requestBO = RequestUtils.getRequestBO(req);

                log.debug("新的代理请求进入：host:{},port{}", requestBO.getHost(), requestBO.getPort());

                if (CONNECT_METHOD.equalsIgnoreCase(req.method().name())) {
                    HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, CONNECTION_ESTABLISHED);
                    ctx.writeAndFlush(resp);
                    ctx.pipeline().remove(HttpServerCodec.class);
                    ReferenceCountUtil.release(msg);
                    return;
                }
            }
        } else if (msg instanceof HttpContent) {
            HttpContent httpContent = ((HttpContent) msg).duplicate();

            int i = httpContent.content().readableBytes();
            if (i > 0) {
                byte[] bytes = new byte[i];
                httpContent.content().readBytes(bytes);
                String s = new String(bytes, Charset.defaultCharset());
                log.debug("===========================数据类型：{}，host:{}|port:{}|content:{}", FileTypeCheckUtil.getFileType(bytes), this.requestBO.getHost(), this.requestBO.getPort(), s);
            }
            isHttp = true;
        } else {
            ByteBuf buf = (ByteBuf) msg;
            //ssl hand
            if (buf.getByte(0) == 22) {
                this.requestBO.setSupportSsl(Boolean.TRUE);

                SslContext sslContext = SslContextBuilder
                        .forServer(proxyConfig.getPrivateKey(), CertFactory.getCert(requestBO.getHost(), proxyConfig))
                        .build();

                //由于可以解密明文，重新添加serverCodec
                ctx.pipeline()
                        .addFirst(new HttpServerCodec())
                        .addFirst(sslContext.newHandler(ctx.alloc()));

                //重新过一遍pipeline，处理解密后的报文
                ctx.pipeline().fireChannelRead(msg);
                return;
            }
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
