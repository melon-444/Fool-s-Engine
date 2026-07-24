package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderPassComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Collects {@link RenderPassComponent} entities into an ordered pass list,
 * consumed by the renderer to configure per-pass GL state (depth, blend, etc.).
 *
 * <p>Multiple RenderPassComponent entities on different entities define the pipeline.
 * This collector sorts them by {@link RenderPassComponent#order} and exposes the list.</p>
 */
public class RenderPassCollector extends ClientSystem {

    private final SparseSet<RenderPassComponent> passes;
    private final List<RenderPassComponent> sortedPasses = new ArrayList<>();
    private boolean dirty = true;

    {
        requiredComponents.add(RenderPassComponent.class);
    }

    public RenderPassCollector(FoolsEngine engine) {
        super(engine);
        passes = getSparseSet(RenderPassComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        // TODO: wire into GLRenderFrame — pass sortedPasses to renderer for per-pass state setup
        // TODO: handle pass-specific cameras (e.g. shadow pass uses shadow cam)
        if (!dirty) return;

        sortedPasses.clear();
        List<RenderPassComponent> list = new ArrayList<>();
        for (int e : entities) {
            RenderPassComponent p = passes.getComponent(e);
            if (p != null) list.add(p);
        }
        list.sort(Comparator.comparingInt(p -> p.order));
        sortedPasses.addAll(list);
        dirty = false;
    }

    public List<RenderPassComponent> getSortedPasses() {
        return sortedPasses;
    }
}
