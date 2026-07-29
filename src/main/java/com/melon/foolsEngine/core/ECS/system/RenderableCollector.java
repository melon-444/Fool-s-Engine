// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderableComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.RevZFrustumIntersection;
import com.melon.foolsEngine.util.SparseSet;
import com.melon.foolsEngine.util.logger.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderableCollector extends ClientSystem {

    private final SparseSet<TransformComponent> transforms;
    private final SparseSet<RenderableComponent> renderables;
    private final RevZFrustumIntersection frustum = new RevZFrustumIntersection();
    private final Vector3f worldMin = new Vector3f();
    private final Vector3f worldMax = new Vector3f();
    private final Matrix4f vp = new Matrix4f();
    private final Logger log = new Logger("FrustumCull");

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
        Camera camera = scene.getCamera();
        boolean hasFrustum = false;
        if (camera != null) {
            camera.vp().get(vp);
            frustum.setVp(vp);
            hasFrustum = true;
        }

        int submitted = 0, skipped = 0;

        for (int e : entities) {
            TransformComponent t = transforms.getComponent(e);
            RenderableComponent r = renderables.getComponent(e);
            if (t == null || r == null) continue;

            Mesh mesh = r.mesh;
            float[] aabb = mesh.getAABB();
            if (aabb != null && hasFrustum) {
                worldMin.set(aabb[0], aabb[1], aabb[2]);
                worldMax.set(aabb[3], aabb[4], aabb[5]);
                t.getMatrix().transformAab(worldMin, worldMax, worldMin, worldMax);
                if (frustum.testAab(worldMin.x, worldMin.y, worldMin.z,
                                     worldMax.x, worldMax.y, worldMax.z) == RevZFrustumIntersection.OUTSIDE) {
                    skipped++;
                    continue;
                }
            }

            scene.submit(new RenderCommand(mesh, r.material, t.getMatrix()));
            submitted++;
        }

        if (hasFrustum && skipped > 0) {
            log.trace("cull: %d/%d submitted, %d skipped", submitted, entities.size(), skipped);
        }
    }
}
