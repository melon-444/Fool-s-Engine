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
package com.melon.foolsEngine.core.world;

import com.melon.foolsEngine.api.rendering.render.GraphicsContext;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.system.ClientSystem;
import com.melon.foolsEngine.core.ECS.system.ServerSystem;
import com.melon.foolsEngine.core.annotation.InstanceBusSubscriber;
import com.melon.foolsEngine.core.annotation.SubscribeEvent;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.PostRenderEvent;
import com.melon.foolsEngine.core.events.builtInEvents.PreRenderEvent;
import com.melon.foolsEngine.core.events.builtInEvents.SystemRegisteredEvent;
import com.melon.foolsEngine.core.events.builtInEvents.SystemUnregisteredEvent;
import com.melon.foolsEngine.util.logger.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@InstanceBusSubscriber
public class SystemScheduler {

    private static final long FIXED_DT_NS = 16_666_667L;
    private static final float FIXED_DT_S = FIXED_DT_NS * 1e-9f;
    private static final int MAX_FRAME_CATCHUP = 5;
    private final Logger logger = new Logger("SysScheduler");

    private record ServerEntry(ServerSystem<?> system, Object ctx) {}
    private record ServerWave(List<ServerEntry> entries) {}

    private final List<ClientSystem> clientSystems = new ArrayList<>();
    private final List<ServerEntry> serverEntries = new ArrayList<>();
    private final AtomicInteger regSeq = new AtomicInteger();
    private final Map<Class<?>, Integer> regOrder = new ConcurrentHashMap<>();

    private List<ServerWave> serverPlan;
    private boolean serverPlanDirty;

    private final ExecutorService workerPool;
    private final RenderFrame frame;
    private final GraphicsContext ctx;
    private final boolean headless;

    private Runnable additionalRenderTask;

    private RenderScene sceneFront;
    private RenderScene sceneBack;

    private long accumulatorNs;
    private long lastFrameNs = java.lang.System.nanoTime();

    public SystemScheduler(SystemManager systemManager) {
        this(null, null, systemManager);
    }

    public SystemScheduler(RenderFrame frame, GraphicsContext ctx, SystemManager systemManager) {
        this.frame = frame;
        this.ctx = ctx;
        this.headless = (frame == null || ctx == null);

        int workers = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        workerPool = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "FoolsEngine-Logic-" + r.hashCode() % 100);
            t.setDaemon(true);
            return t;
        });

        if (!headless) {
            sceneFront = new RenderScene();
            sceneBack = new RenderScene();
            sceneFront.setBackGroundColor(0.1f, 0.1f, 0.12f, 1.0f);
            sceneBack.setBackGroundColor(0.1f, 0.1f, 0.12f, 1.0f);
        }

        scheduleSystem(systemManager);
        EventBus.addListener(this);
    }

    @SubscribeEvent
    public void onSystemRegistered(SystemRegisteredEvent event) {
        scheduleSystem(event.systemManager);
    }

    @SubscribeEvent
    public void onSystemUnregistered(SystemUnregisteredEvent event) {
        var sc = event.systemClass;
        clientSystems.removeIf(cs -> cs.getClass() == sc);
        serverEntries.removeIf(se -> se.system.getClass() == sc);
        regOrder.remove(sc);
        serverPlanDirty = true;
    }

    private void scheduleSystem(SystemManager systemManager) {
        for (var system : systemManager.getRegisteredSystems().values()) {
            if (system instanceof ClientSystem cs)
                registerClient(cs);
            else if (system instanceof ServerSystem<?> ss)
                registerServer(ss, ss.getContext());
        }
    }

    public boolean isHeadless() {
        return headless;
    }

    private <Context> void registerServer(ServerSystem<?> system, Context ctx) {
        if (serverEntries.stream().anyMatch(se -> se.system == system)) return;
        regOrder.putIfAbsent(system.getClass(), regSeq.getAndIncrement());
        serverEntries.add(new ServerEntry(system, ctx));
        serverPlanDirty = true;
        logger.debug("new ServerSystem %s detected", system.getClass().getSimpleName());
    }

    private void registerClient(ClientSystem system) {
        if (clientSystems.contains(system)) return;
        regOrder.putIfAbsent(system.getClass(), regSeq.getAndIncrement());
        clientSystems.add(system);
        sortClientSystems();
        logger.debug("new ClientSystem %s detected", system.getClass().getSimpleName());
    }

    private void sortClientSystems() {
        clientSystems.sort(
                Comparator.comparingInt(ClientSystem::collectionOrder)
                        .thenComparingInt(cs -> regOrder.getOrDefault(cs.getClass(), Integer.MAX_VALUE)));
    }

    public void additionalRenderTask(Runnable task) {
        this.additionalRenderTask = task;
    }

    // ── ServerSystem DAG / Wave ──

    private void buildServerPlan() {
        List<ServerEntry> entries = List.copyOf(serverEntries);
        if (entries.isEmpty()) {
            serverPlan = List.of();
            serverPlanDirty = false;
            return;
        }

        int n = entries.size();
        Map<Class<?>, Integer> byClass = new HashMap<>();
        for (int i = 0; i < n; i++) byClass.put(entries.get(i).system.getClass(), i);

        int[] inDegree = new int[n];
        Map<Integer, Set<Integer>> reverse = new HashMap<>();
        for (int i = 0; i < n; i++) reverse.put(i, new HashSet<>());

        for (int i = 0; i < n; i++) {
            ServerSystem<?> s = entries.get(i).system;
            for (var depClass : s.dependencies()) {
                Integer depIdx = byClass.get(depClass);
                if (depIdx == null) {
                    throw new IllegalStateException(
                        "ServerSystem " + s.getClass().getSimpleName()
                        + " depends on unregistered system: " + depClass.getSimpleName());
                }
                if (depIdx == i) {
                    throw new IllegalStateException(
                        "ServerSystem " + s.getClass().getSimpleName()
                        + " declares a self-dependency");
                }
                if (reverse.get(depIdx).add(i)) {
                    inDegree[i]++;
                }
            }
        }

        List<ServerWave> waves = new ArrayList<>();
        boolean[] placed = new boolean[n];
        int placedCount = 0;

        while (placedCount < n) {
            List<ServerEntry> wave = new ArrayList<>();
            List<Integer> justPlaced = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (!placed[i] && inDegree[i] == 0) {
                    wave.add(entries.get(i));
                    placed[i] = true;
                    placedCount++;
                    justPlaced.add(i);
                }
            }
            if (wave.isEmpty()) {
                StringBuilder sb = new StringBuilder("Circular dependency among ServerSystems:");
                for (int i = 0; i < n; i++) {
                    if (!placed[i]) {
                        sb.append("\n  ").append(entries.get(i).system.getClass().getSimpleName())
                          .append(" remaining in-degree=").append(inDegree[i]);
                    }
                }
                throw new IllegalStateException(sb.toString());
            }
            waves.add(new ServerWave(List.copyOf(wave)));
            for (int p : justPlaced) {
                for (int dependent : reverse.get(p)) {
                    inDegree[dependent]--;
                }
            }
        }

        this.serverPlan = List.copyOf(waves);
        this.serverPlanDirty = false;
    }

    // ── main update ──
    public void update() {
        EventBus systemBus = EventBus.get("SystemBus");
        if (systemBus != null) systemBus.process();

        long now = java.lang.System.nanoTime();
        long elapsed = now - lastFrameNs;
        lastFrameNs = now;

        accumulatorNs += Math.min(elapsed, FIXED_DT_NS * MAX_FRAME_CATCHUP);

        while (accumulatorNs >= FIXED_DT_NS) {
            if (serverPlanDirty) buildServerPlan();
            runServerWaves(FIXED_DT_S);
            accumulatorNs -= FIXED_DT_NS;
        }

        if (isHeadless()) return;

        float frameDt = elapsed * 1e-9f;
        for (ClientSystem cs : clientSystems) {
            cs.update(frameDt, sceneBack);
        }

        RenderScene tmp = sceneFront;
        sceneFront = sceneBack;
        sceneBack = tmp;

        sceneBack.clear();
        sceneBack.setLighting(sceneFront.getLighting());
        sceneBack.setTextureManager(sceneFront.getTextureManager());
        sceneBack.setBackGroundColor(
                sceneFront.getBgR(), sceneFront.getBgG(),
                sceneFront.getBgB(), sceneFront.getBgA());

        ctx.makeCurrent();

        if (systemBus != null) systemBus.emitNow(new PreRenderEvent(sceneFront));

        frame.render(sceneFront);

        if (systemBus != null) systemBus.emitNow(new PostRenderEvent(sceneFront));

        if (additionalRenderTask != null)
            additionalRenderTask.run();
        ctx.swapBuffers();
        ctx.pollEvents();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void runServerWaves(float dt) {
        if (serverPlan == null || serverPlan.isEmpty()) return;

        for (ServerWave wave : serverPlan) {
            List<Future<?>> futures = new ArrayList<>();
            for (ServerEntry entry : wave.entries()) {
                ServerSystem sys = entry.system;
                futures.add(workerPool.submit(() -> {
                    sys.update(dt, entry.ctx());
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    cancelFutures(futures);
                    throw new RuntimeException("ServerSystem wave interrupted", e);
                } catch (ExecutionException e) {
                    cancelFutures(futures);
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re) throw re;
                    throw new RuntimeException("ServerSystem execution failed", cause);
                }
            }
        }
    }

    private void cancelFutures(List<Future<?>> futures) {
        for (Future<?> f : futures) {
            if (!f.isDone()) f.cancel(true);
        }
    }

    public RenderScene getScene() {
        return sceneBack;
    }

    public void shutdown() {
        workerPool.shutdown();
    }
}
