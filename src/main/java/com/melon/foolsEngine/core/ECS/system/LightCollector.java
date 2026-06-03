package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.core.ECS.basicComponents.Light;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class LightCollector extends System {

    private final SparseSet<Light> Lights;
    {
        requiredComponents.add(Light.class);
    }

    public LightCollector(FoolsEngine engine) {
        super(engine);
        Lights = getSparseSet(Light.class);
    }

    @Override
    public void update(long dt) {
        //TODO complete

    }
}
