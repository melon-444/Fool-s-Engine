package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderPassComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderPassCollector extends ClientSystem {

    private final SparseSet<RenderPassComponent> passComps;

    {
        requiredComponents.add(RenderPassComponent.class);
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    public RenderPassCollector(FoolsEngine engine) {
        super(engine);
        passComps = getSparseSet(RenderPassComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        List<RenderPassComponent> userPasses = new ArrayList<>();
        for (int e : entities) {
            RenderPassComponent pc = passComps.getComponent(e);
            if (pc != null) userPasses.add(pc);
        }
        userPasses.sort(Comparator.comparingInt(p -> p.order));

        for (RenderPassComponent pc : userPasses) {
            scene.submitPass(pc.pass);
        }
    }
}