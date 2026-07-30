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

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.core.ECS.basicComponents.CameraComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.MainCameraChangedEvent;
import com.melon.foolsEngine.util.ProjectionType;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Matrix4f;

public class CameraCollector extends ClientSystem {

    private final SparseSet<CameraComponent> cameras;
    private final SparseSet<TransformComponent> transforms;
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();

    {
        requiredComponents.add(CameraComponent.class);
        requiredComponents.add(TransformComponent.class);
    }

    public CameraCollector(FoolsEngine engine) {
        super(engine);
        cameras = getSparseSet(CameraComponent.class);
        transforms = getSparseSet(TransformComponent.class);
    }

    @Override
    public int collectionOrder() {
        return 10;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (cam == null || !cam.active || !cam.isMainCam) continue;

            TransformComponent t = transforms.getComponent(e);
            if (t == null) continue;

            // Never invert TransformComponent's cached model matrix in place.
            // Doing so makes the cached value alternate between model and view
            // space on frames where the transform is not rebuilt.
            view.set(t.getMatrix()).invert();
            proj.identity();
            if (cam.projectionType == ProjectionType.PERSPECTIVE) {
                com.melon.foolsEngine.util.PerspectiveProjection persp =
                        new com.melon.foolsEngine.util.PerspectiveProjection(cam.FOVy, INSTANCE.aspect, cam.near);
                persp.get(proj);
            } else if (cam.projectionType == ProjectionType.ORTHOGRAPHIC) {
                com.melon.foolsEngine.util.OrthogonalProjection orth =
                        new com.melon.foolsEngine.util.OrthogonalProjection(
                                cam.orthoSize * INSTANCE.aspect, cam.orthoSize, cam.near, cam.far);
                orth.get(proj);
            } else throw new IllegalStateException("Unknown projection type");
            Camera camera = new Camera(new Matrix4f(view), new Matrix4f(proj));
            scene.setCamera(camera);

            EventBus bus = EventBus.get("SystemBus");
            if (bus != null) bus.emit(new MainCameraChangedEvent(camera));
            return;
        }
    }
}
