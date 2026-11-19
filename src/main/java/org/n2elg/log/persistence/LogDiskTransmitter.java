package org.n2elg.log.persistence;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.ThreadContext;
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
        while (!this.consumer.isInterrupted()) {
            String batchJson = logStreamQueue.poll();
            try {
                if (batchJson == null) {
                    LOGGER.debug("There is no log event in queue");
                    TimeUnit.MILLISECONDS.sleep(INNER_THREAD_SLEEP_MILLIS);
                } else {
                    splitAndWriteLogs(batchJson);
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Thread {} will quit because of {}", Thread.currentThread().getName(), e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.warn("Unexpected error happens, skip this log event", e);
            }
        }
        LOGGER.debug("Before quit this dispatcher, clean up spoooled logs in {}", this.writerName);
        logStreamQueue.drain(this::splitAndWriteLogs);
        LOGGER.info("Spooled logs have been written to files, thread quits");
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

    private void splitAndWriteLogs(String batchString) {
        String lineSeparator = getLineSeparator(batchString);
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

    @SuppressWarnings("java:S2629")
    private void persistToDisk(String json, String lineSeparator) {
        LogstashLogEvent event = this.jsonReader.readValue(json);
        // Timestamp
        ThreadContext.put("TS", event.getTimestamp());
        // Log Level
        ThreadContext.put("LL", event.getLevel());
        // Host Name
        ThreadContext.put("HN", event.getSourceHost());
        // Service Name
        ThreadContext.put("SN", event.getServiceName());
        // Transaction ID
        ThreadContext.put("TX_ID", event.getTransactionId());
        ThreadContext.put("PID", event.getPID());
        /*
         * String.formart or String.join String.concat in some way is super slower than
         * StringBuilder. Although there are many articles introduce that there is no
         * difference with using operator "+" or StringBuilder (JVM will optimize it).
         * To 100% ensure there is no performanace degradation, I still choose
         * StringBuilder.
         * Output LOG_PATTERN = "[] [] %s  :%s"
         */
        StringBuilder sb = new StringBuilder(4096);
        sb.append("[t:").append(event.getThreadName()).append("][m:");
        sb.append(event.getCallerMethodName());
        sb.append("] ").append(event.getLoggerName()).append("  :  ");
        sb.append(event.getMessage());
        if (event.getStackTrace() != null && !event.getStackTrace().isEmpty()) {
            sb.append(SYSTEM_LINE_SEPARATOR);
            if (SYSTEM_LINE_SEPARATOR.equals(lineSeparator)) {
                sb.append(event.getStackTrace());
            } else {
                sb.append(event.getStackTrace().replace(lineSeparator, SYSTEM_LINE_SEPARATOR));
            }
        }
        logWriter.info(sb.toString());
        ThreadContext.clearMap();
    }

    /**
     * Windows/Linux/MacOS line separator
     */
    protected String getLineSeparator(String message) {
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '\r') {
                return message.startsWith("\n", i + 1) ? "\r\n" : "\r";
            }
            if (c == '\n') {
                return "\n";
            }
        }
        return SYSTEM_LINE_SEPARATOR;
    }
}
