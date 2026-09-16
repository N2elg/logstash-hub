package org.n2elg.log.persistence;

public enum DispatcherStrategy {
    ROUND_ROBIN("RoundRobin"),
    HASH("Hash"),
    RANDOM("Random");

    private final String value;

    DispatcherStrategy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
