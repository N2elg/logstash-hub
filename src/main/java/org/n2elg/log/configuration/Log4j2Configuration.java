package org.n2elg.log.configuration;

import lombok.Data;

@Data
public class Log4j2Configuration {
    private String logPath;
    private String maxSingleLogSize;
    private String maxAccumulatedLogSize;
    private String maxLastModified;
    private int maxCountOfCompressedPerHour;
}
