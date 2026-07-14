package org.n2elg.log.configuration;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.dataformat.toml.TomlMapper;

public class ConfigurationLoader {

    private static final String CONFIGURATION_FILE_NAME = "application.toml";
    private static final TomlMapper TOML_MAPPER = new TomlMapper();

    public ConfigurationLoader() {
    }

    public LogstashHubDelegatedConfiguration load() throws IOException {
        Path path = Paths.get(CONFIGURATION_FILE_NAME);
        InputStream tomlInputStream;
        String currentTime = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalDateTime.now());
        if (Files.exists(path)) {
            System.err.println(currentTime + " [main] INFO - Find outside configuration");
            tomlInputStream = Files.newInputStream(path);
        } else {
            System.err.println(currentTime + " [main] INFO - Could not find outside configuration, use default configuration");
            tomlInputStream = this.getClass().getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME);
            if (tomlInputStream == null) {
                throw new IOException("Default configuration resource not found: " + CONFIGURATION_FILE_NAME);
            }
        }

        try (InputStream inputStream = tomlInputStream) {
            return TOML_MAPPER.readValue(inputStream, LogstashHubDelegatedConfiguration.class);
        }
    }
}
