package org.n2elg.log.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogstashLogEvent {

    @JsonProperty("@timestamp")
    private String timestamp;
    @JsonProperty("@version")
    private String version;
    private String message;
    @JsonProperty("logger_name")
    private String loggerName;
    @JsonProperty("thread_name")
    private String threadName;
    private String level;
    @JsonProperty("source_host")
    private String sourceHost;
    private String PID = "";
    private String serviceName = "unknown-service";
    @JsonProperty("caller_method_name")
    private String callerMethodName = "?";
    @JsonProperty("caller_class_name")
    private String callerClassName;
    @JsonProperty("caller_line_number")
    private String callerLineNumber;
    @JsonProperty("caller_file_name")
    private String callerFileName;
    private String transactionId;
    private String stackTrace;
}