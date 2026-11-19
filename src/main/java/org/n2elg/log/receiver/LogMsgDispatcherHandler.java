package org.n2elg.log.receiver;

import org.n2elg.log.MsgDispatcher;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogMsgDispatcherHandler extends ChannelInboundHandlerAdapter {

    private final MsgDispatcher sseDispatcher;
    private final MsgDispatcher consolDispatcher;

    public LogMsgDispatcherHandler(MsgDispatcher sseDispatcher, MsgDispatcher consolDispatcher) {
        this.sseDispatcher = sseDispatcher;
        this.consolDispatcher = consolDispatcher;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String logMsg = (String) msg;
        log.debug("Log message {} would be sent to destination", logMsg);
        try {
            sseDispatcher.transfer(logMsg, ctx.channel().id());
            consolDispatcher.transfer(logMsg, ctx.channel().id());
        } catch (Exception e) {
            log.error("Unknown exception happen", e);
        }
    }
}
