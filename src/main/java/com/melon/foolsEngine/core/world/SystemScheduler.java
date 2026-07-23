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
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.core.ECS.system.ClientSystem;
import com.melon.foolsEngine.core.ECS.system.ServerSystem;

import java.util.ArrayList;
import java.util.List;

public class SystemScheduler {

    private static final long FIXED_DT_NS = 16_666_667L;
    private static final float FIXED_DT_S = FIXED_DT_NS * 1e-9f;
    private static final int MAX_FRAME_CATCHUP = 5;

    private record ServerEntry(ServerSystem<?> system, Object ctx) {
    }

    private final List<ServerEntry> serverEntries = new ArrayList<>();
    private final List<ClientSystem<?>> clientSystems = new ArrayList<>();
    private final RenderFrame frame;
    private final GraphicsContext ctx;
    private final boolean headless;

    private RenderScene sceneFront;
    private RenderScene sceneBack;

    private long accumulatorNs;
    private long lastFrameNs = java.lang.System.nanoTime();

    public SystemScheduler(RenderFrame frame, GraphicsContext ctx) {
        this.frame = frame;
        this.ctx = ctx;
        this.headless = (frame == null || ctx == null);

        if (!headless) {
            sceneFront = new RenderScene();
            sceneBack = new RenderScene();
            sceneFront.setBackGroundColor(0.1f, 0.1f, 0.12f, 1.0f);
            sceneBack.setBackGroundColor(0.1f, 0.1f, 0.12f, 1.0f);
        }
    }

    public SystemScheduler() {
        this(null, null);
    }

    public boolean isHeadless() {
        return headless;
    }

    public <C> void registerServer(ServerSystem<C> system, C ctx) {
        serverEntries.add(new ServerEntry(system, ctx));
    }

    public void registerClient(ClientSystem<?> system) {
        clientSystems.add(system);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void update() {
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

        sceneBack.setLighting(sceneFront.getLighting());
        sceneBack.setTextureManager(sceneFront.getTextureManager());
        sceneBack.setBackGroundColor(
                sceneFront.getBgR(), sceneFront.getBgG(),
                sceneFront.getBgB(), sceneFront.getBgA());

        ctx.makeCurrent();
        frame.render(sceneFront);
        ctx.swapBuffers();
        ctx.pollEvents();
    }

    public RenderScene getScene() {
        return sceneBack;
    }
}
