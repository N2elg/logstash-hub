package org.n2elg.log.persistence;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.shaded.org.jctools.queues.MpscArrayQueue;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.blackbird.BlackbirdModule;

public class LogDiskTransmitter implements LogTransmitter, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogDiskTransmitter.class);
    private static final String SYSTEM_LINE_SEPARATOR = System.lineSeparator();
    private static final long INNER_THREAD_SLEEP_MILLIS = 20L;
    private static final char JSON_END_FLAG = '}';
    private static final int MAX_RETRY_TIME = 20;
    private final ObjectReader jsonReader;
    private final Logger logWriter;
    private MpscArrayQueue<String> logStreamQueue;
    private final String writerName;
    private final Thread consumer;

    public LogDiskTransmitter(Logger logWriter, int maxConcurrentLog, int index) {
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(new BlackbirdModule())
                .build();
        this.jsonReader = mapper.readerFor(new TypeReference<LogstashLogEvent>() {
        });
        this.logWriter = logWriter;
        this.logStreamQueue = new MpscArrayQueue<>(maxConcurrentLog);
        this.writerName = "DiskWriter-" + (index + 1);
        this.consumer = new Thread(this, this.writerName);
        this.consumer.setDaemon(false);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    @Override
    public void transmit(String log) {
        boolean result = logStreamQueue.offer(log);
        if (!result) {
            LOGGER.warn("{}'s queue is full, skip this log event", this.writerName);
        }
    }

    @Override
    public void start() {
        this.consumer.start();
        LOGGER.info("{} has been created and started up", this.writerName);
    }

    @Override
    public void stop() {
        if (this.consumer.isAlive()) {
            this.consumer.interrupt();
        }
        for (int i = 0; i < MAX_RETRY_TIME; i++) {
            if (this.consumer.isAlive()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(INNER_THREAD_SLEEP_MILLIS);
                } catch (InterruptedException e) {
                    LOGGER.warn("Sleeping in interrupted when waiting for close writer {}", this.writerName);
                }
            } else {
                break;
            }
        }
    }

    private void splitAndWriteLogs(String batchString, String lineSeparator) {
        String[] jsonStrings = batchString.split(lineSeparator);
        LOGGER.debug("{} logs are received", jsonStrings.length);
        Arrays.stream(jsonStrings).forEach(jsonString -> {
            if (!jsonString.isEmpty()) {
                try {
                    persistToDisk(jsonString, lineSeparator);
                } catch (JacksonException e) {
                    LOGGER.warn("Fail to deserialize this log event {} with line separator 0x{}", jsonString,
                            HexFormat.of().formatHex(lineSeparator.getBytes()), e);
                }
            }
        });
    }

    private void persistToDisk(String json, String lineSeparator) {

    }
}
