package org.n2elg.log.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class LogstashHubDelegatedConfigurationTest {

    @Test
    void gettersReturnValuesSetBySetters() {
        LogRecieverConfiguration receiver = new LogRecieverConfiguration();
        SSEConfiguration sseServer = new SSEConfiguration();
        Log4j2Configuration log = new Log4j2Configuration();

        LogstashHubDelegatedConfiguration configuration = new LogstashHubDelegatedConfiguration();
        configuration.setReceiver(receiver);
        configuration.setSseServer(sseServer);
        configuration.setLog(log);

        assertSame(receiver, configuration.getReceiver());
        assertSame(sseServer, configuration.getSseServer());
        assertSame(log, configuration.getLog());
    }

    @Test
    void equalsAndHashCodeAreConsistentForSameValues() {
        LogstashHubDelegatedConfiguration first = buildConfiguration();
        LogstashHubDelegatedConfiguration second = buildConfiguration();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        LogstashHubDelegatedConfiguration first = buildConfiguration();
        LogstashHubDelegatedConfiguration second = buildConfiguration();
        second.getSseServer().setPort(1);

        assertNotEquals(first, second);
    }

    private LogstashHubDelegatedConfiguration buildConfiguration() {
        LogRecieverConfiguration receiver = new LogRecieverConfiguration();
        receiver.setPort(38787);
        receiver.setSocketBacklog(1024);
        receiver.setSocketSendBuffSize(2097152);
        receiver.setSocketReceiveBuffSize(2097152);
        receiver.setDispatcherStrategy("RANDOM");
        receiver.setFreeMsgAsyncQueueSize(2097152);
        receiver.setLogDispatcherQueueSize(65536);

        SSEConfiguration sseServer = new SSEConfiguration();
        sseServer.setPort(30081);
        sseServer.setMaxConcurrentLogStreams(65536);

        Log4j2Configuration log = new Log4j2Configuration();
        log.setLogPath("/var/logs/logstash-hub/");
        log.setMaxSingleLogSize("2 GB");
        log.setMaxAccumulatedLogSize("20 GB");
        log.setMaxLastModified("P60D");
        log.setMaxCountOfCompressedPerHour(16);

        LogstashHubDelegatedConfiguration configuration = new LogstashHubDelegatedConfiguration();
        configuration.setReceiver(receiver);
        configuration.setSseServer(sseServer);
        configuration.setLog(log);
        return configuration;
    }
}
