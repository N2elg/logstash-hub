package org.n2elg.log.receiver;

import org.n2elg.log.MsgDispatcher;
import org.n2elg.log.configuration.LogRecieverConfiguration;
import org.n2elg.log.persistence.ConsolidationLogDispatcher;
import org.n2elg.log.persistence.DispatcherStrategy;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.util.NettyRuntime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogReceiverEntry {
    private static final int MAX_WORK_THREAD_COUNT = 16;
    private static final boolean useEpoll = Epoll.isAvailable();
    private static final boolean useIOUring = IoUring.isAvailable();
    private final int port;
    private final int socketBacklog;
    private final MsgDispatcher sseLogDispatcher;
    private final MsgDispatcher consolDispatcher;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final int wokerCount;
    ChannelFuture f;
    private final FreeMsgAsyncExecutor freeMsgAsyncExecutor;
    private final int socketSendBuff;
    private final int socketReceiveBuff;

    public LogReceiverEntry(LogRecieverConfiguration config, MsgDispatcher dispatcher) {
        this.port = config.getPort();
        this.socketBacklog = config.getSocketBacklog();
        this.sseLogDispatcher = dispatcher;
        // Hard code it to 2 to make 3-Way HandShake faster when restart logstash-hub.
        this.bossGroup = new MultiThreadIoEventLoopGroup(2, getHandlerFactory());
        this.wokerCount = Math.min(MAX_WORK_THREAD_COUNT, NettyRuntime.availableProcessors() * 2);
        this.workerGroup = new MultiThreadIoEventLoopGroup(wokerCount, getHandlerFactory());
        this.freeMsgAsyncExecutor = new FreeMsgAsyncExecutor(config.getFreeMsgAsyncQueueSize());
        this.consolDispatcher = new ConsolidationLogDispatcher(wokerCount, config.getLogDispatcherQueueSize(),
                DispatcherStrategy.valueOf(config.getDispatcherStrategy()));
        this.socketSendBuff = config.getSocketSendBuffSize();
        this.socketReceiveBuff = config.getSocketReceiveBuffSize();
    }

    private IoHandlerFactory getHandlerFactory() {
        return useIOUring ? IoUringIoHandler.newFactory()
                : useEpoll ? EpollIoHandler.newFactory() : NioIoHandler.newFactory();
    }

    private Class<? extends ServerChannel> getChannel() {
        return useIOUring ? IoUringServerSocketChannel.class
                : useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

}
