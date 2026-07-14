package org.n2elg.log.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class LogRecieverConfigurationTest {

    @Test
    void gettersReturnValuesSetBySetters() {
        LogRecieverConfiguration configuration = buildConfiguration();

        assertEquals(38787, configuration.getPort());
        assertEquals(1024, configuration.getSocketBacklog());
        assertEquals(2097152, configuration.getSocketSendBuffSize());
        assertEquals(2097152, configuration.getSocketReceiveBuffSize());
        assertEquals("RANDOM", configuration.getDispatcherStrategy());
        assertEquals(2097152, configuration.getFreeMsgAsyncQueueSize());
        assertEquals(65536, configuration.getLogDispatcherQueueSize());
    }

    @Test
    void equalsAndHashCodeAreConsistentForSameValues() {
        LogRecieverConfiguration first = buildConfiguration();
        LogRecieverConfiguration second = buildConfiguration();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        LogRecieverConfiguration first = buildConfiguration();
        LogRecieverConfiguration second = buildConfiguration();
        second.setPort(1);

        assertNotEquals(first, second);
    }

    private LogRecieverConfiguration buildConfiguration() {
        LogRecieverConfiguration configuration = new LogRecieverConfiguration();
        configuration.setPort(38787);
        configuration.setSocketBacklog(1024);
        configuration.setSocketSendBuffSize(2097152);
        configuration.setSocketReceiveBuffSize(2097152);
        configuration.setDispatcherStrategy("RANDOM");
        configuration.setFreeMsgAsyncQueueSize(2097152);
        configuration.setLogDispatcherQueueSize(65536);
        return configuration;
    }
}
