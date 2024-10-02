package work.orchestrator.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String baseName;
    private final AtomicInteger threadCount = new AtomicInteger(0);

    public NamedThreadFactory(String baseName) {
        this.baseName = baseName;
    }

    @Override
    public Thread newThread(Runnable r) {
        // Create a new thread and give it a name based on the base name and a unique number
        Thread thread = new Thread(r);
        thread.setName(baseName + "-" + threadCount.incrementAndGet());
        return thread;
    }
}