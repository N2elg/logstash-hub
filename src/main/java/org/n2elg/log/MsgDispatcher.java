package org.n2elg.log;

import io.netty.channel.ChannelId;

public interface MsgDispatcher {

    void transfer(String message, ChannelId channelId);

    void shutdown();
}
