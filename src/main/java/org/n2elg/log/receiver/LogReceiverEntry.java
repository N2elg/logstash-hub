package org.n2elg.log.receiver;

import java.io.IOException;
import java.net.URISyntaxException;

import org.n2elg.log.EntryPoint;
import org.n2elg.log.MsgDispatcher;
import org.n2elg.log.configuration.LogRecieverConfiguration;
import org.n2elg.log.persistence.ConsolidationLogDispatcher;
import org.n2elg.log.persistence.DispatcherStrategy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
import io.netty.util.NettyRuntime;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogReceiverEntry implements EntryPoint {
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

    @Override
    public void start() throws InterruptedException, URISyntaxException, IOException {
        log.info("Start log receiver with port:{}, ioThread:{}, socketBacklog:{}, IO type:{}", port, 2, wokerCount,
                socketBacklog, useIOUring ? "IOUring" : useEpoll ? "Epoll" : "Nio");
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(getChannel()).childHandler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("log-decoder", new LogMsgStickHandler(freeMsgAsyncExecutor));
                    ch.pipeline().addLast("log-dispatcher",
                            new LogMsgDispatcherHandler(sseLogDispatcher, consolDispatcher));
                    ch.pipeline().addLast("exception-handler", new LogReceiverExceptionHandler());
                }
            }).option(ChannelOption.SO_BACKLOG, socketBacklog)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_SNDBUF, socketSendBuff)
                    .childOption(ChannelOption.SO_RCVBUF, socketReceiveBuff)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            f = b.bind(port).sync();
        } catch (Exception e) {
            log.error("Fail to start log receiver due to: ", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            } else {
                stop();
            }
        }
    }

    private IoHandlerFactory getHandlerFactory() {
        return useIOUring ? IoUringIoHandler.newFactory()
                : useEpoll ? EpollIoHandler.newFactory() : NioIoHandler.newFactory();
    }

    private Class<? extends ServerChannel> getChannel() {
        return useIOUring ? IoUringServerSocketChannel.class
                : useEpoll ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
    }

    @Override
    @SuppressWarnings("java:S2142")
    public void stop() {
        f.channel().close();
        try {
            workerGroup.shutdownGracefully().sync();
            bossGroup.shutdownGracefully().sync();
        } catch (Exception e) {
            log.error("Unexpected exceptions happen when shut down logstash hub.", e);
        }
        consolDispatcher.shutdown();
        freeMsgAsyncExecutor.shutdown();
    }
}
