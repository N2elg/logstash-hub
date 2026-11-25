module org.n2elg.log {
    requires java.naming;

    requires io.netty.buffer;
    requires io.netty.common;
    requires io.netty.transport;
    requires io.netty.transport.classes.epoll;
    requires io.netty.transport.classes.io_uring;
    requires io.netty.transport.epoll.linux.x86_64;
    requires io.netty.transport.io_uring.linux.x86_64;
    requires org.jctools.core;

    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j.slf4j.impl;
    requires com.lmax.disruptor;

    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.toml;
    requires tools.jackson.module.blackbird;

    requires static lombok;

    opens org.n2elg.log.configuration to tools.jackson.databind;
    opens org.n2elg.log.persistence to tools.jackson.databind;

}
