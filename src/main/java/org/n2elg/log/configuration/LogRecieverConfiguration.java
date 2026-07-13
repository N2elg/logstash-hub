package org.n2elg.log.configuration;

import lombok.Data;

@Data
public class LogRecieverConfiguration {
    private int port;
    private int socketBacklog;
    private int socketSendBuffSize;
    private int socketReceiveBuffSize;
    private String dispatcherStrategy;
    private int freeMsgAsyncQueueSize;
    private int logDispatcherQueueSize;
}
