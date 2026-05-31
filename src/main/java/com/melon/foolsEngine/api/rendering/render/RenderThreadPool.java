package com.melon.foolsEngine.api.rendering.render;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RenderThreadPool {

    private final ExecutorService renderMain;
    private final ExecutorService workers;
    private final AtomicInteger workerCounter = new AtomicInteger(0);
    private volatile boolean running = true;

    public RenderThreadPool() {
        renderMain = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "RenderMain");
            t.setDaemon(true);
            return t;
        });
        workers = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "RenderWorker-" + workerCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    public void executeOnMain(Runnable task) {
        if (running) {
            renderMain.execute(task);
        }
    }

    public <T> Future<T> submitToWorker(Callable<T> task) {
        if (running) {
            return workers.submit(task);
        }
        return null;
    }

    public Future<?> submitToWorker(Runnable task) {
        if (running) {
            return workers.submit(task);
        }
        return null;
    }

    public void shutdown() {
        running = false;
        renderMain.shutdown();
        workers.shutdown();
    }

    public boolean awaitShutdown(long timeout, TimeUnit unit) throws InterruptedException {
        boolean mainDone = renderMain.awaitTermination(timeout, unit);
        boolean workersDone = workers.awaitTermination(timeout, unit);
        return mainDone && workersDone;
    }

    public int activeWorkerCount() {
        return ((ThreadPoolExecutor) workers).getActiveCount();
    }
}
