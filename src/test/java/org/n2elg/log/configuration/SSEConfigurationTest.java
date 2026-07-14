package org.n2elg.log.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SSEConfigurationTest {

    @Test
    void gettersReturnValuesSetBySetters() {
        SSEConfiguration configuration = buildConfiguration();

        assertEquals(30081, configuration.getPort());
        assertEquals(65536, configuration.getMaxConcurrentLogStreams());
    }

    @Test
    void equalsAndHashCodeAreConsistentForSameValues() {
        SSEConfiguration first = buildConfiguration();
        SSEConfiguration second = buildConfiguration();

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentValues() {
        SSEConfiguration first = buildConfiguration();
        SSEConfiguration second = buildConfiguration();
        second.setMaxConcurrentLogStreams(1);

        assertNotEquals(first, second);
    }

    private SSEConfiguration buildConfiguration() {
        SSEConfiguration configuration = new SSEConfiguration();
        configuration.setPort(30081);
        configuration.setMaxConcurrentLogStreams(65536);
        return configuration;
    }
}
