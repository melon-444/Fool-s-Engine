package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderableComp;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComp;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class RenderableCollector extends ClientSystem {

    private final SparseSet<TransformComp> transforms;
    private final SparseSet<RenderableComp> renderables;

    {
        requiredComponents.add(TransformComp.class);
        requiredComponents.add(RenderableComp.class);
    }

    public RenderableCollector(FoolsEngine engine) {
        super(engine);
        transforms = getSparseSet(TransformComp.class);
        renderables = getSparseSet(RenderableComp.class);
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            TransformComp t = transforms.getComponent(e);
            RenderableComp r = renderables.getComponent(e);
            if (t == null || r == null) continue;
            scene.submit(new RenderCommand(r.mesh, r.material, t.getMatrix()));
        }
    }
}
