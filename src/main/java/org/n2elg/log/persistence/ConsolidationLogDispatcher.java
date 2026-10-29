package org.n2elg.log.persistence;

import java.util.Random;

import org.n2elg.log.MsgDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelId;

public class ConsolidationLogDispatcher implements MsgDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsolidationLogDispatcher.class);
    private final LogTransmitter[] transmitters;
    private final DispatcherStrategy strategy;
    private final Random r;
    private volatile int counter;

    public ConsolidationLogDispatcher(int threadCount, int maxConcurrentlog, DispatcherStrategy strategy) {
        this.transmitters = new LogTransmitter[threadCount];
        for (int i = 0; i < threadCount; i++) {
            this.transmitters[i] = new LogTransmitter();
        }
        this.strategy = strategy;
        this.r = new Random();
        this.counter = 0;
    }

    @Override
    public void transfer(String message, ChannelId channelId) {
        int i = 0;
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'transfer'");
    }

    @Override
    public void shutdown() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'shutdown'");
    }

}
