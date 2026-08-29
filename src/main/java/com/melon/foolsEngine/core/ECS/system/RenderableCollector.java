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
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderableComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.Octree;
import com.melon.foolsEngine.util.RevZFrustumIntersection;
import com.melon.foolsEngine.util.SparseSet;
import com.melon.foolsEngine.util.logger.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenderableCollector extends ClientSystem {

    private final SparseSet<TransformComponent> transforms;
    private final SparseSet<RenderableComponent> renderables;
    private final RevZFrustumIntersection frustum = new RevZFrustumIntersection();
    private final RevZFrustumIntersection shadowFrustum = new RevZFrustumIntersection();
    private final Octree octree = new Octree();
    private final List<Octree.Item> items = new ArrayList<>();
    private final Set<Integer> candidates = new HashSet<>();
    private final Set<Integer> uncullable = new HashSet<>();
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
    public int collectionOrder() {
        return 30;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        Camera camera = scene.getCamera();

        List<Camera> shadowCameras = null;
        LightEnvironment lighting = scene.getLighting();
        if (lighting != null) {
            for (Light light : lighting.getLights()) {
                if (light.castsShadow() && light.shadowInfo.shadowCamera() != null) {
                    if (shadowCameras == null) shadowCameras = new ArrayList<>();
                    shadowCameras.add(light.shadowInfo.shadowCamera());
                }
            }
        }

        if (camera == null) {
            for (int e : entities) {
                TransformComponent t = transforms.getComponent(e);
                RenderableComponent r = renderables.getComponent(e);
                if (t == null || r == null) continue;
                scene.submit(new RenderCommand(r.mesh, r.material, t.getMatrix()));
            }
            return;
        }

        // Broad phase: collect world-space AABBs and build an octree over them.
        items.clear();
        uncullable.clear();
        for (int e : entities) {
            TransformComponent t = transforms.getComponent(e);
            RenderableComponent r = renderables.getComponent(e);
            if (t == null || r == null) continue;

            float[] aabb = r.mesh.getAABB();
            if (aabb == null) {
                uncullable.add(e);
                continue;
            }
            worldMin.set(aabb[0], aabb[1], aabb[2]);
            worldMax.set(aabb[3], aabb[4], aabb[5]);
            t.getMatrix().transformAab(worldMin, worldMax, worldMin, worldMax);
            items.add(new Octree.Item(e, worldMin.x, worldMin.y, worldMin.z,
                    worldMax.x, worldMax.y, worldMax.z));
        }

        octree.build(items);

        // Query the union of the main frustum and every shadow frustum so that
        // objects only visible to a shadow map are not culled.
        candidates.clear();
        frustum.setVp(camera.vp().get(vp));
        octree.query(frustum, candidates);
        if (shadowCameras != null) {
            for (Camera shadowCam : shadowCameras) {
                shadowFrustum.setVp(shadowCam.vp().get(vp));
                octree.query(shadowFrustum, candidates);
            }
        }

        int submitted = 0;
        for (int e : entities) {
            if (!candidates.contains(e) && !uncullable.contains(e)) continue;
            TransformComponent t = transforms.getComponent(e);
            RenderableComponent r = renderables.getComponent(e);
            if (t == null || r == null) continue;
            scene.submit(new RenderCommand(r.mesh, r.material, t.getMatrix()));
            submitted++;
        }

        if (submitted < entities.size()) {
            log.trace("cull: %d/%d submitted", submitted, entities.size());
        }
    }
}
