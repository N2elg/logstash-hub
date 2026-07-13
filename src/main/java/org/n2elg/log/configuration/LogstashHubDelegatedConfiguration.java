package org.n2elg.log.configuration;

import lombok.Data;

@Data
public class LogstashHubDelegatedConfiguration {
    private SSEConfiguration sseServer;
    private LogRecieverConfiguration receiver;
    private Log4j2Configuration log;
}
