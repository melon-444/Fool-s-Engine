// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
