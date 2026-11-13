package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.ui.UiDrawList.WorldAnchor;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.UiComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Vector3f;

public final class UiCollector extends ClientSystem {
    private final SparseSet<UiComponent> components;
    private final SparseSet<TransformComponent> transforms;
    private final Vector3f anchor = new Vector3f();

    public UiCollector(FoolsEngine engine) {
        super(engine);
        engine.componentManager.registerComponent(UiComponent.class);
        requiredComponents.add(UiComponent.class);
        components = getSparseSet(UiComponent.class);
        transforms = getSparseSet(TransformComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int entity : entities.stream().sorted().toList()) {
            UiComponent ui = components.getComponent(entity);
            if (ui == null || !ui.visible) {
                continue;
            }
            WorldAnchor worldAnchor = null;
            if (ui.followTransform) {
                TransformComponent transform = transforms.getComponent(entity);
                if (transform == null) {
                    continue;
                }
                anchor.set(ui.offsetX, ui.offsetY, ui.offsetZ);
                transform.getMatrix().transformPosition(anchor);
                worldAnchor = new WorldAnchor(anchor.x, anchor.y, anchor.z);
            }
            for (var quad : ui.getQuads()) {
                scene.ui().submit(worldAnchor == null ? quad : quad.at(worldAnchor));
            }
        }
    }
}
