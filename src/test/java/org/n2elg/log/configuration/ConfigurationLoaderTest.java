package org.n2elg.log.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigurationLoaderTest {

    private static final Path OUTSIDE_CONFIGURATION_PATH = Paths.get("application.toml");

    private final ConfigurationLoader configurationLoader = new ConfigurationLoader();

    @BeforeEach
    @AfterEach
    void removeOutsideConfigurationFile() throws IOException {
        Files.deleteIfExists(OUTSIDE_CONFIGURATION_PATH);
    }

    @Test
    void loadsDefaultConfigurationFromClasspathWhenNoOutsideFileExists() throws IOException {
        LogstashHubDelegatedConfiguration configuration = configurationLoader.load();

        assertEquals(38787, configuration.getReceiver().getPort());
        assertEquals(1024, configuration.getReceiver().getSocketBacklog());
        assertEquals(2097152, configuration.getReceiver().getSocketSendBuffSize());
        assertEquals(2097152, configuration.getReceiver().getSocketReceiveBuffSize());
        assertEquals("RANDOM", configuration.getReceiver().getDispatcherStrategy());
        assertEquals(2097152, configuration.getReceiver().getFreeMsgAsyncQueueSize());
        assertEquals(65536, configuration.getReceiver().getLogDispatcherQueueSize());

        assertEquals(30081, configuration.getSseServer().getPort());
        assertEquals(65536, configuration.getSseServer().getMaxConcurrentLogStreams());

        assertEquals("/var/logs/logstash-hub/", configuration.getLog().getLogPath());
        assertEquals("2 GB", configuration.getLog().getMaxSingleLogSize());
        assertEquals("20 GB", configuration.getLog().getMaxAccumulatedLogSize());
        assertEquals("P60D", configuration.getLog().getMaxLastModified());
        assertEquals(16, configuration.getLog().getMaxCountOfCompressedPerHour());
    }

    @Test
    void loadsOutsideConfigurationWhenPresent() throws IOException {
        String outsideConfiguration = """
            [receiver]
            port = 12345
            socketBacklog = 1
            socketSendBuffSize = 1024
            socketReceiveBuffSize = 1024
            dispatcherStrategy = "HASH"
            freeMsgAsyncQueueSize = 1024
            logDispatcherQueueSize = 1024

            [sseServer]
            port = 54321
            maxConcurrentLogStreams = 10

            [log]
            logPath = "/tmp/logs/"
            maxSingleLogSize = "1 GB"
            maxAccumulatedLogSize = "5 GB"
            maxLastModified = "P30D"
            maxCountOfCompressedPerHour = 4
            """;
        Files.writeString(OUTSIDE_CONFIGURATION_PATH, outsideConfiguration, StandardCharsets.UTF_8);

        LogstashHubDelegatedConfiguration configuration = configurationLoader.load();

        assertEquals(12345, configuration.getReceiver().getPort());
        assertEquals("HASH", configuration.getReceiver().getDispatcherStrategy());
        assertEquals(54321, configuration.getSseServer().getPort());
        assertEquals(10, configuration.getSseServer().getMaxConcurrentLogStreams());
        assertEquals("/tmp/logs/", configuration.getLog().getLogPath());
        assertEquals(4, configuration.getLog().getMaxCountOfCompressedPerHour());
    }

    @Test
    void throwsWhenOutsideConfigurationIsMalformed() throws IOException {
        Files.writeString(OUTSIDE_CONFIGURATION_PATH, "this is not valid toml [[[", StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, configurationLoader::load);
    }
}
