package org.n2elg.log.receiver;

import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogMsgStickHandler extends ChannelInboundHandlerAdapter {

    private static final ConcurrentHashMap<Integer, String> halfPackets = new ConcurrentHashMap<>();
    private static final char JSON_START_FLAG = '{';
    private static final char JSON_END_FLAG = '}';
    private static final char LINE_FEED = '\n';
    private static final char CARRIAGE_RETURN = '\r';
    private final FreeMsgAsyncExecutor freeMsgAsyncExecutor;

    /**
     * This decoder is to resolve the problem of TCP sticky packet or half packet
     * which may cause by Nagle's algorithm (used as default in TCP layer) or
     * Sliding Window or TCP Congestion Control
     * 
     * Nagle's Algorithm:
     * is a means of improving the efficiency of TCP/IP networks
     * by reducing the number of packets that need to be sent over the network. It
     * was defined by John Nagle while working for Ford Aerospace. It was published
     * in 1984 as a Request for Comments (RFC) with title Congestion Control in
     * IP/TCP Internetworks in RFC 896.
     * 
     * The RFC describes what Nagle calls the "small-packet problem", where an
     * application repeatedly emits data in small chunks, frequently only 1 byte in
     * size. Since TCP packets have a 40-byte header (20 bytes for TCP, 20 bytes for
     * IPv4), this results in a 41-byte packet for 1 byte of useful information, a
     * huge overhead. This situation often occurs in Telnet sessions, where most
     * keypresses generate a single byte of data that is transmitted immediately.
     * Worse, over slow links, many such packets can be in transit at the same time,
     * potentially leading to congestion collapse.
     * 
     * Nagle's algorithm works by combining a number of small outgoing messages and
     * sending them all at once. Specifically, as long as there is a sent packet for
     * which the sender has received no acknowledgment, the sender should keep
     * buffering its output until it has a full packet's worth of output, thus
     * allowing output to be sent all at once.
     */

    public LogMsgStickHandler(FreeMsgAsyncExecutor freeMsgAsyncExecutor) {
        this.freeMsgAsyncExecutor = freeMsgAsyncExecutor;
    }

}
