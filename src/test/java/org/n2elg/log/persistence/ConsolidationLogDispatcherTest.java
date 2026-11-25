package org.n2elg.log.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.n2elg.log.MsgDispatcher;

import io.netty.channel.ChannelId;

class ConsolidationLogDispatcherTest {

    @ParameterizedTest
    @CsvSource({ "ROUND_ROBIN, RoundRobin", "HASH, Hash", "RANDOM, Random" })
    void exposesStrategyName(DispatcherStrategy strategy, String name) {
        assertEquals(name, strategy.getValue());
    }

    @Test
    void roundRobinCyclesThroughAllThreeWriters() {
        List<String> writers = dispatch(DispatcherStrategy.ROUND_ROBIN, 0, 0, 0, 0, 0, 0);

        assertEquals(3, new HashSet<>(writers.subList(0, 3)).size());
        assertEquals(writers.subList(0, 3), writers.subList(3, 6));
    }

    @Test
    void hashRoutesByChannelAndHandlesNegativeHashes() {
        List<String> writers = dispatch(DispatcherStrategy.HASH, 0, 1, 2, -1, Integer.MIN_VALUE, -1);

        assertEquals(3, new HashSet<>(writers.subList(0, 3)).size());
        assertEquals(writers.get(3), writers.get(5));
    }

    @Test
    void randomDispatchDeliversEveryMessageExactlyOnce() {
        dispatch(DispatcherStrategy.RANDOM, 1, 2, 3, 4, 5, 6);
    }

    private List<String> dispatch(DispatcherStrategy strategy, int... hashes) {
        try (var output = new RecordingAppender("org.n2elg.log.logstash.writer")) {
            MsgDispatcher dispatcher = new ConsolidationLogDispatcher(3, 16, strategy);
            try {
                for (int i = 0; i < hashes.length; i++) {
                    dispatcher.transfer("{\"transactionId\":\"" + i + "\"}", new TestChannelId(hashes[i]));
                }
            } finally {
                dispatcher.shutdown();
            }

            List<LogEvent> events = output.events();
            assertEquals(hashes.length, events.size());
            Map<Integer, String> writers = new HashMap<>();
            for (LogEvent event : events) {
                String id = event.getContextData().getValue("TX_ID");
                writers.put(Integer.valueOf(id), event.getThreadName());
            }
            List<Integer> ids = IntStream.range(0, hashes.length).boxed().toList();
            assertEquals(new HashSet<>(ids), writers.keySet());
            return ids.stream().map(writers::get).toList();
        }
    }

    private record TestChannelId(int id) implements ChannelId {
        @Override
        public String asShortText() {
            return Integer.toString(id);
        }

        @Override
        public String asLongText() {
            return asShortText();
        }

        @Override
        public int compareTo(ChannelId other) {
            return asLongText().compareTo(other.asLongText());
        }
    }
}
