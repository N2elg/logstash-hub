package org.n2elg.log.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class Log4j2ConfigurationTest {

    @Test
    void gettersReturnValuesSetBySetters() {
        Log4j2Configuration configuration = buildConfiguration();

        assertEquals("/var/logs/logstash-hub/", configuration.getLogPath());
        assertEquals("2 GB", configuration.getMaxSingleLogSize());
        assertEquals("20 GB", configuration.getMaxAccumulatedLogSize());
        assertEquals("P60D", configuration.getMaxLastModified());
        assertEquals(16, configuration.getMaxCountOfCompressedPerHour());
    }

    @Test
    void equalsAndHashCodeAreConsistentForSameValues() {
        Log4j2Configuration first = buildConfiguration();
        Log4j2Configuration second = buildConfiguration();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        Log4j2Configuration first = buildConfiguration();
        Log4j2Configuration second = buildConfiguration();
        second.setMaxCountOfCompressedPerHour(1);

        assertNotEquals(first, second);
    }

    private Log4j2Configuration buildConfiguration() {
        Log4j2Configuration configuration = new Log4j2Configuration();
        configuration.setLogPath("/var/logs/logstash-hub/");
        configuration.setMaxSingleLogSize("2 GB");
        configuration.setMaxAccumulatedLogSize("20 GB");
        configuration.setMaxLastModified("P60D");
        configuration.setMaxCountOfCompressedPerHour(16);
        return configuration;
    }
}
