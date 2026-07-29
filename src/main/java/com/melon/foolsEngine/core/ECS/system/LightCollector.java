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
package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.core.ECS.basicComponents.LightComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.LightAddedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.LightRemovedEvent;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LightCollector extends ClientSystem {

    private final SparseSet<LightComponent> ecsLights;
    private final Map<Integer, Light> activeLights = new HashMap<>();

    {
        requiredComponents.add(LightComponent.class);
    }

    public LightCollector(FoolsEngine engine) {
        super(engine);
        ecsLights = getSparseSet(LightComponent.class);
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        LightEnvironment env = scene.getLighting();
        if (env == null) return;

        Camera mainCamera = scene.getCamera();

        Set<Integer> current = new HashSet<>(entities);
        Set<Integer> toRemove = new HashSet<>(activeLights.keySet());

        if (env.getLights().isEmpty() && !activeLights.isEmpty()) {
            activeLights.clear();
            return;
        }

        toRemove.removeAll(current);
        EventBus bus = EventBus.get("SystemBus");
        for (int eid : toRemove) {
            Light removed = activeLights.remove(eid);
            if (removed != null) {
                env.remove(removed);
                if (bus != null) bus.emit(new LightRemovedEvent(eid, removed));
            }
        }

        for (int eid : current) {
            if (activeLights.containsKey(eid)) continue;

            LightComponent lc = ecsLights.getComponent(eid);
            if (lc == null) continue;

            Light apiLight = switch (lc.lightType) {
                case PARALLEL -> Light.directional(lc.color, lc.direction, lc.intensity);
                case POINT -> Light.point(lc.color, lc.position, lc.intensity);
                case SPOT -> Light.spot(lc.color, lc.direction, lc.position,
                        lc.innerTheta, lc.outerTheta, lc.intensity);
            };

            if (lc.castsShadow && mainCamera != null) {
                Camera camCopy = new Camera(
                        new Matrix4f(mainCamera.view),
                        new Matrix4f(mainCamera.projection));
                apiLight = switch (lc.lightType) {
                    case PARALLEL -> env.enableDirLightShadow(apiLight, camCopy);
                    case SPOT -> env.enableSpotLightShadow(apiLight, lc.shadowNear);
                    default -> apiLight;
                };
            }

            env.add(apiLight);
            activeLights.put(eid, apiLight);

            if (bus != null) bus.emit(new LightAddedEvent(eid, apiLight));
        }
    }
}
