package org.n2elg.log.persistence;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

class RecordingAppender extends AbstractAppender implements AutoCloseable {

    private final ConcurrentLinkedQueue<LogEvent> events = new ConcurrentLinkedQueue<>();
    private final CountDownLatch firstEvent = new CountDownLatch(1);
    private final Logger logger;

    RecordingAppender(String loggerName) {
        super("capture-" + loggerName, null, null, false, Property.EMPTY_ARRAY);
        logger = (Logger) LogManager.getLogger(loggerName);
        start();
        logger.addAppender(this);
    }

    @Override
    public void append(LogEvent event) {
        events.add(event.toImmutable());
        firstEvent.countDown();
    }

    boolean awaitFirstEvent() throws InterruptedException {
        return firstEvent.await(5, TimeUnit.SECONDS);
    }

    List<LogEvent> events() {
        return List.copyOf(events);
    }

    @Override
    public void close() {
        logger.removeAppender(this);
        stop();
    }
}
