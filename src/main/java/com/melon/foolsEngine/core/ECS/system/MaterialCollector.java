package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.MaterialComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

/**
 * Collects {@link MaterialComponent} entities and manages material bindings.
 *
 * <p>TODO PBR support: material parameter bindings (roughness, metallic, ao maps),
 * shader variant selection, and texture slot management.</p>
 */
public class MaterialCollector extends ClientSystem {

    private final SparseSet<MaterialComponent> materials;

    {
        requiredComponents.add(MaterialComponent.class);
    }

    public MaterialCollector(FoolsEngine engine) {
        super(engine);
        materials = getSparseSet(MaterialComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        // TODO: PBR — iterate materials, bind texture arrays, set per-material UBOs
        // TODO: material instance management (reuse vs per-entity material data)
    }
}
