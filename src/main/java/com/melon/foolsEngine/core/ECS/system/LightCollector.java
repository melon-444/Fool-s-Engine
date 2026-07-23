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

import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LightCollector extends ClientSystem<RenderScene> {

    private final SparseSet<com.melon.foolsEngine.core.ECS.basicComponents.Light> ecsLights;
    private final Map<Integer, Light> activeLights = new HashMap<>();

    {
        requiredComponents.add(com.melon.foolsEngine.core.ECS.basicComponents.Light.class);
    }

    public LightCollector(FoolsEngine engine) {
        super(engine);
        ecsLights = getSparseSet(com.melon.foolsEngine.core.ECS.basicComponents.Light.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        LightEnvironment env = scene.getLighting();
        if (env == null) {
            return;
        }

        Set<Integer> currentEntities = new HashSet<>(entities);
        Set<Integer> toRemove = new HashSet<>(activeLights.keySet());
        toRemove.removeAll(currentEntities);

        for (int eid : toRemove) {
            Light removed = activeLights.remove(eid);
            if (removed != null) {
                env.remove(removed);
            }
        }

        for (int eid : currentEntities) {
            if (activeLights.containsKey(eid)) {
                continue;
            }

            com.melon.foolsEngine.core.ECS.basicComponents.Light ecsLight = ecsLights.getComponent(eid);
            if (ecsLight == null) {
                continue;
            }

            Light apiLight;
            switch (ecsLight.lightType) {
                case PARALLEL -> apiLight = Light.directional(ecsLight.color, ecsLight.direction);
                case POINT -> apiLight = Light.point(ecsLight.color, ecsLight.position);
                case SPOT -> apiLight = Light.spot(ecsLight.color, ecsLight.direction, ecsLight.position,
                        ecsLight.innerTheta, ecsLight.outerTheta);
                default -> {
                    continue;
                }
            }

            env.add(apiLight);
            activeLights.put(eid, apiLight);
        }
    }
}
