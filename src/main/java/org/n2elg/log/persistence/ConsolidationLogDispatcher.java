package org.n2elg.log.persistence;

import java.util.Arrays;
import java.util.Random;

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
        Logger logWriter = LoggerFactory.getLogger("org.n2elg.log.logstash.writer");
        this.transmitters = new LogTransmitter[threadCount];
        for (int i = 0; i < threadCount; i++) {
            this.transmitters[i] = new LogDiskTransmitter(logWriter, maxConcurrentlog, i);
            this.transmitters[i].start();
        }
        this.strategy = strategy;
        this.r = new Random();
        this.counter = 0;
    }

    @Override
    @SuppressWarnings("java:S3078")
    public void transfer(String message, ChannelId channelId) {
        int writerIndex = 0;
        switch (strategy) {
            case HASH:
                // Avoid negative integer
                writerIndex = (channelId.hashCode() & Integer.MAX_VALUE) % transmitters.length;
                break;
            case RANDOM:
                writerIndex = r.nextInt(transmitters.length);
                break;
            case ROUND_ROBIN:
                // AtomicInteger has poor performance in multi-threads scenario
                this.counter++;
                writerIndex = (this.counter & Integer.MAX_VALUE) % transmitters.length;
                break;
            default:
                break;
        }
        LogTransmitter transmitter = this.transmitters[writerIndex];
        LOGGER.debug("Json list {} is sent to thread {}", message, writerIndex);
        transmitter.transmit(message);
    }

    @Override
    public void shutdown() {
        Arrays.stream(transmitters).forEach(LogTransmitter::stop);
    }
}
