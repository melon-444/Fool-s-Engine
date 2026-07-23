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

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.core.ECS.system.ClientSystem;
import com.melon.foolsEngine.core.ECS.system.ServerSystem;
import com.melon.foolsEngine.core.ECS.system.System;
import com.melon.foolsEngine.core.FoolsEngine;

import java.util.ArrayList;
import java.util.List;

public class SystemScheduler {

    private final List<ServerSystem<?>> serverSystems = new ArrayList<>();
    private final List<ClientSystem<?>> clientSystems = new ArrayList<>();
    private final RenderFrame frame;
    private final RenderScene scene = new RenderScene();
    private final FoolsEngine instance;

    private long lastFrameNs = java.lang.System.nanoTime();

    public SystemScheduler(FoolsEngine engine) {
        this.instance = engine;
        this.frame = engine.frame;
        scene.setBackGroundColor(0.1f, 0.1f, 0.12f, 1.0f);
        syncSystems();
    }

    private void syncSystems() {
        clientSystems.clear();
        serverSystems.clear();
        for (System<?> system : instance.systemManager.getRegisteredSystems().values()) {
            if (system instanceof ClientSystem<?> cs) {
                clientSystems.add(cs);
            } else if (system instanceof ServerSystem<?> ss) {
                serverSystems.add(ss);
            }
        }
    }

    public void checkRegisteredSystem() {
        syncSystems();
    }

    //TODO:find a way to deliver context to server systems
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void update() {
        long now = java.lang.System.nanoTime();
        float dt = (now - lastFrameNs) * 1e-9f;
        lastFrameNs = now;

        for (ServerSystem ss : serverSystems) {
            ss.update(dt, null);
        }

        scene.clear();
        for (ClientSystem cs : clientSystems) {
            cs.update(dt, scene);
        }

        frame.render(scene);
    }

    public RenderScene getScene() {
        return scene;
    }
}
