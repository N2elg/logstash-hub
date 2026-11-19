package org.n2elg.log.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class LogDiskTransmitterTest {

    private static final String LOGGER_NAME = "persistence-test";
    private final RecordingAppender output = new RecordingAppender(LOGGER_NAME);
    private final LogDiskTransmitter transmitter = new LogDiskTransmitter(LoggerFactory.getLogger(LOGGER_NAME), 2, 0);

    @AfterEach
    void close() {
        transmitter.stop();
        output.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void detectsBatchLineEnding(String separator) {
        assertEquals(separator, transmitter.getLineSeparator("{}" + separator));
    }

    @Test
    void defaultsToSystemSeparatorForASingleEvent() {
        assertEquals(System.lineSeparator(), transmitter.getLineSeparator("{}"));
    }

    @Test
    void writesFormattedMessageAndStructuredContext() throws InterruptedException {
        transmitter.start();
        transmitter.transmit("""
                {"@timestamp":"2026-01-01T00:00:00Z","level":"INFO","source_host":"host","serviceName":"api",\
                 "transactionId":"tx","PID":"42","thread_name":"main","caller_method_name":"save",\
                 "logger_name":"Service","message":"done","extra":"ignored"}""");

        assertTrue(output.awaitFirstEvent(), "The running writer should emit the event before shutdown");
        assertEquals(List.of("[t:main][m:save] Service  :  done"), messages());
        assertEquals(Map.of("TS", "2026-01-01T00:00:00Z", "LL", "INFO", "HN", "host", "SN", "api",
                "TX_ID", "tx", "PID", "42"), output.events().getFirst().getContextData().toMap());
    }

    @Test
    void appendsMultilineStackTraceWithPlatformLineEndings() {
        transmitter.transmit("""
                {"thread_name":"main","logger_name":"Service","message":"failed","stackTrace":"error\\r\\nat save"}
                """.stripTrailing() + "\r\n");
        transmitter.start();
        transmitter.stop();

        String newline = System.lineSeparator();
        assertEquals(List.of("[t:main][m:?] Service  :  failed" + newline + "error" + newline + "at save"), messages());
    }

    @Test
    void drainsBatchesWithDifferentSeparatorsAndSkipsMalformedAndBlankLines() {
        transmitter.transmit(String.join("\r", event("one"), "not-json", "", event("two"), ""));
        transmitter.transmit(String.join("\n", event("three"), event("four"), ""));
        transmitter.start();
        transmitter.stop();

        assertEquals(List.of("one", "two", "three", "four"), transactionIds());
    }

    @Test
    void fullQueueKeepsPreviouslyAcceptedEvents() {
        transmitter.transmit(event("first"));
        transmitter.transmit(event("second"));
        transmitter.transmit(event("overflow"));
        transmitter.start();
        transmitter.stop();

        assertEquals(List.of("first", "second"), transactionIds());
    }

    private String event(String id) {
        return "{\"transactionId\":\"" + id + "\"}";
    }

    private List<String> transactionIds() {
        return output.events().stream().map(e -> e.getContextData().<String>getValue("TX_ID")).toList();
    }

    private List<String> messages() {
        return output.events().stream().map(LogEvent::getMessage).map(message -> message.getFormattedMessage()).toList();
    }
}
