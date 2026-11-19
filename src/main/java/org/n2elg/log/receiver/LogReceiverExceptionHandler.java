package org.n2elg.log.receiver;

import java.net.SocketException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogReceiverExceptionHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SocketException && cause.getMessage().equals("Connection reset")) {
            log.warn("{}: {} {}", cause.getClass().getName(), ctx.channel().remoteAddress(), cause.getMessage());
        } else {
            log.error("Unknow exception happen", cause);
        }
        ReferenceCountUtil.release(cause);
    }
}
