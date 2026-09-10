package com.phaselume.torana.connector.nfs.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executor providing lightweight thread execution for blocking File IO.
 * Uses Java 21 Virtual Threads when running on Java 21+, with fallback to a cached thread pool on Java 17.
 */
public class VirtualThreadExecutor implements Executor {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadExecutor.class);

    private final Executor delegate;

    public VirtualThreadExecutor() {
        this.delegate = initializeExecutor();
    }

    private Executor initializeExecutor() {
        try {
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            log.info("Initialized Loom Virtual Thread executor for NFS connector");
            return (Executor) method.invoke(null);
        } catch (Throwable t) {
            log.info("Virtual threads not available, falling back to cached thread pool: {}", t.getMessage());
            return Executors.newCachedThreadPool();
        }
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }
}
