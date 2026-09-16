package org.n2elg.log;

import java.io.IOException;
import java.net.URISyntaxException;

public interface EntryPoint {

    void start() throws InterruptedException, URISyntaxException, IOException;

    void stop();
}
