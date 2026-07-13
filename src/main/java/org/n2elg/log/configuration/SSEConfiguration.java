package org.n2elg.log.configuration;

import lombok.Data;

@Data
public class SSEConfiguration {
    private int port;
    private int maxConcurrentLogStreams;
}