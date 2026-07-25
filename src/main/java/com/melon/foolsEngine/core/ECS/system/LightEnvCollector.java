package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.LightEnvComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class LightEnvCollector extends ClientSystem {

    private final SparseSet<LightEnvComponent> lightEnvs;

    {
        requiredComponents.add(LightEnvComponent.class);
    }

    public LightEnvCollector(FoolsEngine engine) {
        super(engine);
        lightEnvs = getSparseSet(LightEnvComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            LightEnvComponent le = lightEnvs.getComponent(e);
            if (le != null) {
                scene.setLighting(le.env);
                return;
            }
        }
    }
}
