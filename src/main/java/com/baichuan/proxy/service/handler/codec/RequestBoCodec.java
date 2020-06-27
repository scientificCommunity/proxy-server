package com.baichuan.proxy.service.handler.codec;

import com.baichuan.proxy.domain.RequestBO;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.List;

/**
 * @author kun
 * @date 2020-06-26 17:13
 */
@Slf4j
public class RequestBoCodec extends ByteToMessageCodec<RequestBO> {
    @Override
    protected void encode(ChannelHandlerContext ctx, RequestBO msg, ByteBuf out) throws Exception {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int i = in.readableBytes();
        if (i <= 0) {
            return;
        }
        String s = in.toString(Charset.defaultCharset());
        try {
            //获取requestBo长度
            String substring = s.substring(0, s.indexOf("{"));

            int a = Integer.parseInt(substring);
            byte[] b1 = new byte[s.indexOf("{")];
            byte[] b2 = new byte[a];
            in.readBytes(b1);
            in.readBytes(b2);
            String string = new String(b2, Charset.defaultCharset());
            out.add(string);
        } catch (NumberFormatException e) {
            log.warn("转换失败，s:{}", s);
        } finally {
            int i1 = in.readableBytes();
            byte[] b3 = new byte[i1];
            in.readBytes(b3);
            out.add(Unpooled.wrappedBuffer(b3));
        }

    }
}
