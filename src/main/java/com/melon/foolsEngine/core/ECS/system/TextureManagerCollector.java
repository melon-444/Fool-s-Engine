package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.TextureManagerComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class TextureManagerCollector extends ClientSystem {

    private final SparseSet<TextureManagerComponent> textureMnrs;

    {
        requiredComponents.add(TextureManagerComponent.class);
    }

    public TextureManagerCollector(FoolsEngine engine) {
        super(engine);
        textureMnrs = getSparseSet(TextureManagerComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            TextureManagerComponent tm = textureMnrs.getComponent(e);
            if (tm != null) {
                scene.setTextureManager(tm.manager);
                return;
            }
        }
    }
}
