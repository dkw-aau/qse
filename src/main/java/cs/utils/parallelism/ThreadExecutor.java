package cs.utils.parallelism;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class ThreadExecutor {
    public static <T, R> R execute(int parallelism, String forkJoinWorkerThreadName, T source, Function<T, R> parallelStream) {
        return execute(parallelism, forkJoinWorkerThreadName, source, 0, null, parallelStream);
    }
    
    public static <T, R> R execute(int parallelism, String forkJoinWorkerThreadName, T source, long timeout, TimeUnit unit, Function<T, R> parallelStream) {
        if (timeout < 0)
            throw new IllegalArgumentException("Invalid timeout " + timeout);
        // see java.util.concurrent.Executors.newWorkStealingPool(int parallelism)
        ExecutorService threadPool = new ForkJoinPool(parallelism, new NamedForkJoinWorkerThreadFactory(forkJoinWorkerThreadName), null, true);
        Future<R> future = threadPool.submit(() -> parallelStream.apply(source));
        try {
            return timeout == 0 ? future.get() : future.get(timeout, unit);
        } catch (ExecutionException e) {
            future.cancel(true);
            threadPool.shutdownNow();
            Throwable cause = e.getCause();
            if (cause instanceof Error)
                throw new ExecutionError((Error) cause);
            throw new UncheckedExecutionException(cause);
        } catch (TimeoutException e) {
            future.cancel(true);
            threadPool.shutdownNow();
            throw new UncheckedTimeoutException(e);
        } catch (Throwable t) {
            future.cancel(true);
            threadPool.shutdownNow();
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException(t);
        } finally {
            threadPool.shutdown();
        }
    }
    
    public static <T> void execute(int parallelism, String forkJoinWorkerThreadName, T source, Consumer<T> parallelStream) {
        execute(parallelism, forkJoinWorkerThreadName, source, 0, null, parallelStream);
    }
    
    public static <T> void execute(int parallelism, String forkJoinWorkerThreadName, T source, long timeout, TimeUnit unit, Consumer<T> parallelStream) {
        if (timeout < 0)
            throw new IllegalArgumentException("Invalid timeout " + timeout);
        // see java.util.concurrent.Executors.newWorkStealingPool(int parallelism)
        ExecutorService threadPool = new ForkJoinPool(parallelism, new NamedForkJoinWorkerThreadFactory(forkJoinWorkerThreadName), null, true);
        CompletableFuture<Void> future = null;
        try {
            Runnable task = () -> parallelStream.accept(source);
            if (timeout == 0) {
                future = CompletableFuture.runAsync(task, threadPool);
                future.get();
                threadPool.shutdown();
            } else {
                threadPool.execute(task);
                threadPool.shutdown();
                if (!threadPool.awaitTermination(timeout, unit))
                    throw new TimeoutException("Timed out after: " + Duration.of(timeout, unit.toChronoUnit()));
            }
        } catch (TimeoutException e) {
            threadPool.shutdownNow();
            throw new UncheckedTimeoutException(e);
        } catch (ExecutionException e) {
            future.cancel(true);
            threadPool.shutdownNow();
            Throwable cause = e.getCause();
            if (cause instanceof Error)
                throw new ExecutionError((Error) cause);
            throw new UncheckedExecutionException(cause);
        } catch (Throwable t) {
            threadPool.shutdownNow();
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException(t);
        }
    }
}
