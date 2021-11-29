package cs.utils.parallelism;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    private AtomicInteger counter = new AtomicInteger(0);
    private final String name;
    private final boolean daemon;

    public NamedForkJoinWorkerThreadFactory(String name, boolean daemon) {
        this.name = name;
        this.daemon = daemon;
    }

    public NamedForkJoinWorkerThreadFactory(String name) {
        this(name, false);
    }

    @Override
    public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread t = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        t.setName(name + counter.incrementAndGet());
        t.setDaemon(daemon);
        return t;
    }
}
