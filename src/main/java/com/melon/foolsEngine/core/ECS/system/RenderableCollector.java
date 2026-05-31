package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;
import com.melon.foolsEngine.core.ECS.basicComponents.Renderable;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class RenderableCollector extends System{
    private final SparseSet<Transform> transforms;
    private final SparseSet<Renderable> renderables;

    private final RenderFrame frame;

    {
        requiredComponents.add(Transform.class);
        requiredComponents.add(Renderable.class);
    }

    public RenderableCollector(FoolsEngine engine) {
        super(engine);
        transforms = getSparseSet(Transform.class);
        renderables = getSparseSet(Renderable.class);
        this.frame = engine.frame;
    }

    @Override
    public void update(long dt) {
        for (int e : entities) {
            Transform t = transforms.getComponent(e);
            Mesh meshComp = renderables.getComponent(e).mesh;
            Material material = renderables.getComponent(e).material;
            frame.submit(new RenderCommand(meshComp,material,t.getMatrix()));
        }
    }
}
