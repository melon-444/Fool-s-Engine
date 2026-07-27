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
import com.melon.foolsEngine.core.ECS.system.System;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@InstanceBusSubscriber
public class SystemScheduler {

    private static final long FIXED_DT_NS = 16_666_667L;
    private static final float FIXED_DT_S = FIXED_DT_NS * 1e-9f;
    private static final int MAX_FRAME_CATCHUP = 5;

    private record ServerEntry(ServerSystem<?> system, Object ctx) {
    }

    private final List<ServerEntry> serverEntries = new ArrayList<>();
    private final List<ClientSystem> clientSystems = new ArrayList<>();
    private final RenderFrame frame;
    private final GraphicsContext ctx;
    private final boolean headless;

    private Runnable additionalRenderTask;

    private RenderScene sceneFront;
    private RenderScene sceneBack;

    private long accumulatorNs;
    private long lastFrameNs = java.lang.System.nanoTime();

    public SystemScheduler(SystemManager systemManager) {
        this(null, null,systemManager);
    }

    public SystemScheduler(RenderFrame frame, GraphicsContext ctx,SystemManager systemManager) {
        this.frame = frame;
        this.ctx = ctx;
        this.headless = (frame == null || ctx == null);

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

    public void scheduleSystem(SystemManager systemManager) {
        for(var system: systemManager.getRegisteredSystems().values()) {
            if (system instanceof ClientSystem clientSystem)
                registerClient(clientSystem);
            if (system instanceof ServerSystem<?> serverSystem)
                registerServer(serverSystem,serverSystem.getContext());
        }
    }

    public boolean isHeadless() {
        return headless;
    }

    private <Context> void registerServer(ServerSystem<?> system, Context ctx) {
        if(!serverEntries.stream().anyMatch(serverEntry -> serverEntry.system == system))
            serverEntries.add(new ServerEntry(system, ctx));
        serverEntries.sort(Comparator.comparingInt(entry -> entry.system.priority()));
    }

    private void registerClient(ClientSystem system) {
        if(!clientSystems.contains(system))
            clientSystems.add(system);
        clientSystems.sort(Comparator.comparingInt(System::priority));
    }

    public void additionalRenderTask(Runnable task) {
        this.additionalRenderTask = task;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void update() {
        EventBus systemBus = EventBus.get("SystemBus");
        if (systemBus != null) systemBus.process();
        long now = java.lang.System.nanoTime();
        long elapsed = now - lastFrameNs;
        lastFrameNs = now;

        accumulatorNs += Math.min(elapsed, FIXED_DT_NS * MAX_FRAME_CATCHUP);

        while (accumulatorNs >= FIXED_DT_NS) {
            for (ServerEntry entry : serverEntries) {
                ((ServerSystem) entry.system).update(FIXED_DT_S, entry.ctx);
            }
            accumulatorNs -= FIXED_DT_NS;
        }

        if (headless) return;

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

    public RenderScene getScene() {
        return sceneBack;
    }
}
