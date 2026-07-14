# logstash-hub

Lightweight log receiver and SSE streaming hub.

## Build

```bash
./mvnw clean package
```

## Run

```bash
java -jar target/logstash-hub-1.0-SNAPSHOT.jar
```

The JAR manifest main class is `org.n2elg.log.LogstashHub`.

## Configuration

Configuration format is TOML.

The application loads config in this order:

1. `application.toml` in the current working directory (outside config)
2. bundled classpath resource `application.toml` (default)

### Example `application.toml`

```toml
[receiver]
port = 38787
socketBacklog = 1024
socketSendBuffSize = 2097152
socketReceiveBuffSize = 2097152
dispatcherStrategy = "RANDOM"
freeMsgAsyncQueueSize = 2097152
logDispatcherQueueSize = 65536

[sseServer]
port = 30081
maxConcurrentLogStreams = 65536

[log]
logPath = "/var/logs/logstash-hub/"
maxSingleLogSize = "2 GB"
maxAccumulatedLogSize = "20 GB"
maxLastModified = "P60D"
maxCountOfCompressedPerHour = 16
```

### Field Mapping

- `[receiver]` -> `LogRecieverConfiguration`
- `[sseServer]` -> `SSEConfiguration`
- `[log]` -> `Log4j2Configuration`
