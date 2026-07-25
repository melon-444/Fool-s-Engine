package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.core.ECS.basicComponents.LightComp;
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

    private final SparseSet<LightComp> ecsLights;
    private final Map<Integer, Light> activeLights = new HashMap<>();

    {
        requiredComponents.add(LightComp.class);
    }

    public LightCollector(FoolsEngine engine) {
        super(engine);
        ecsLights = getSparseSet(LightComp.class);
    }

    @Override
    public int priority() {
        return 2;
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

            LightComp lc = ecsLights.getComponent(eid);
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
