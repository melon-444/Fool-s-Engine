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

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.CameraComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class CameraCollector extends ClientSystem {

    private final SparseSet<CameraComponent> cameras;
    private final SparseSet<Transform> transforms;
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private final PerspectiveProjection perspective = new PerspectiveProjection(0, 0, 0);
    private final Quaternionf conjugateTmp = new Quaternionf();

    {
        requiredComponents.add(CameraComponent.class);
        requiredComponents.add(Transform.class);
    }

    public CameraCollector(FoolsEngine engine) {
        super(engine);
        cameras = getSparseSet(CameraComponent.class);
        transforms = getSparseSet(Transform.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        super.update(dt, scene);
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (!cam.active)
                continue;
            else
                deactivateOtherCam(e);

            Transform t = transforms.getComponent(e);
            Matrix4f view = this.view.identity().set(t.getMatrix());
            perspective.aspect = INSTANCE.aspect;
            perspective.fov = cam.FOVy;
            perspective.near = cam.near;
            Matrix4f proj = perspective.get(this.proj.identity());
            scene.setCamera(new Camera(view, proj));
            break;
        }
    }

    private void deactivateOtherCam(int excludeEntityID) {
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (e != excludeEntityID)
                cam.active = false;
        }
    }
}
