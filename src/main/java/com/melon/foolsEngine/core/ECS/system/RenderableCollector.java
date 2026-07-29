package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderableComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class RenderableCollector extends ClientSystem {

    private final SparseSet<TransformComponent> transforms;
    private final SparseSet<RenderableComponent> renderables;

    {
        requiredComponents.add(TransformComponent.class);
        requiredComponents.add(RenderableComponent.class);
    }

    public RenderableCollector(FoolsEngine engine) {
        super(engine);
        transforms = getSparseSet(TransformComponent.class);
        renderables = getSparseSet(RenderableComponent.class);
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            TransformComponent t = transforms.getComponent(e);
            RenderableComponent r = renderables.getComponent(e);
            if (t == null || r == null) continue;
            scene.submit(new RenderCommand(r.mesh, r.material, t.getMatrix()));
        }
    }
}
