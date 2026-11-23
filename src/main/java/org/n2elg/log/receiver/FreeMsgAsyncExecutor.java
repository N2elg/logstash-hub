package org.n2elg.log.receiver;

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.jctools.queues.MpscArrayQueue;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FreeMsgAsyncExecutor {
    private static final long INNER_THREAD_SLEEP_MILLIS = 20L;
    private final MpscArrayQueue<Object> logStreamQueue;
    private final Thread t;

    public FreeMsgAsyncExecutor(int maxConcurrentCount) {
        logStreamQueue = new MpscArrayQueue<>(maxConcurrentCount);
        t = new Thread(getRunnable(), "FreeMsgAysncExecturor");
        t.setDaemon(true);
        t.start();
    }

    public void shutdown() {
        if (t.isAlive()) {
            log.debug("Stop thread FreeMsgAysncExecturor");
            t.interrupt();
        }
    }

    public void free(Object msg) {
        boolean result = logStreamQueue.offer(msg);
        if (!result) {
            log.warn("FreeMsgAsyncExecutor's queue is full, free message in Netty threads");
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    private Runnable getRunnable() {
        return () -> {
            while (!t.isInterrupted()) {
                ByteBuf in = (ByteBuf) logStreamQueue.poll();
                if (in == null) {
                    log.debug("There is no log events in queue.");
                    try {
                        TimeUnit.MILLISECONDS.sleep(INNER_THREAD_SLEEP_MILLIS);
                    } catch (InterruptedException e) {
                        log.warn("Thread {} will quit because of interruption", Thread.currentThread().getName());
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.debug("Message {} would be free.", in);
                    ReferenceCountUtil.safeRelease(in);
                }
            }
        };
    }
}
