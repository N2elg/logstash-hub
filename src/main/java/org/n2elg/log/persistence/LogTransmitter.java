package org.n2elg.log.persistence;

public interface LogTransmitter {

    void transmit(String log);

    void start();

    void stop();
}