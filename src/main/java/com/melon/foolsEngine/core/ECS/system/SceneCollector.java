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
import com.melon.foolsEngine.core.ECS.basicComponents.CameraComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.Renderable;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SceneCollector extends ClientSystem {

    private final SparseSet<CameraComponent> cameras;
    private final SparseSet<Transform> transforms;
    private final SparseSet<com.melon.foolsEngine.core.ECS.basicComponents.Light> ecsLights;
    private final SparseSet<Renderable> renderables;
    private final Map<Integer, Light> activeLights = new HashMap<>();

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private final PerspectiveProjection persp = new PerspectiveProjection(0, 0, 0);
    private final Quaternionf conjugateTmp = new Quaternionf();

    {
        requiredComponents.add(Transform.class);
    }

    public SceneCollector(FoolsEngine engine) {
        super(engine);
        cameras = getSparseSet(CameraComponent.class);
        transforms = getSparseSet(Transform.class);
        ecsLights = getSparseSet(com.melon.foolsEngine.core.ECS.basicComponents.Light.class);
        renderables = getSparseSet(Renderable.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        collectCamera(scene);
        collectLights(scene);
        collectRenderables(scene);
    }

    private void collectCamera(RenderScene scene) {
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (cam == null || !cam.active) continue;

            deactivateOtherCam(e);

            Transform t = transforms.getComponent(e);
            if (t == null) continue;

            view.identity()
                    .rotate(t.rotation.conjugate(conjugateTmp))
                    .translate(-t.position.x, -t.position.y, -t.position.z);
            persp.aspect = INSTANCE.aspect;
            persp.fov = cam.FOVy;
            persp.near = cam.near;
            proj.identity();
            scene.setCamera(new Camera(new Matrix4f(view), persp.get(proj)));
            return;
        }
    }

    private void deactivateOtherCam(int excludeEntityID) {
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (cam != null && e != excludeEntityID)
                cam.active = false;
        }
    }

    private void collectLights(RenderScene scene) {
        LightEnvironment env = scene.getLighting();
        if (env == null) return;

        Camera mainCamera = scene.getCamera();

        Set<Integer> current = new HashSet<>(entities);
        Set<Integer> toRemove = new HashSet<>(activeLights.keySet());
        toRemove.removeAll(current);

        for (int eid : toRemove) {
            Light removed = activeLights.remove(eid);
            if (removed != null) env.remove(removed);
        }

        for (int eid : current) {
            if (activeLights.containsKey(eid)) continue;

            com.melon.foolsEngine.core.ECS.basicComponents.Light ecsLight = ecsLights.getComponent(eid);
            if (ecsLight == null) continue;

            Light apiLight = convertToApiLight(ecsLight);

            if (ecsLight.castsShadow && mainCamera != null) {
                switch (ecsLight.lightType) {
                    case PARALLEL -> apiLight = env.enableDirLightShadow(apiLight, mainCamera);
                    case SPOT -> apiLight = env.enableSpotLightShadow(apiLight, ecsLight.shadowNear);
                }
            }

            env.add(apiLight);
            activeLights.put(eid, apiLight);
        }
    }

    private Light convertToApiLight(com.melon.foolsEngine.core.ECS.basicComponents.Light ecsLight) {
        return switch (ecsLight.lightType) {
            case PARALLEL -> Light.directional(ecsLight.color, ecsLight.direction, ecsLight.intensity);
            case POINT -> Light.point(ecsLight.color, ecsLight.position, ecsLight.intensity);
            case SPOT -> Light.spot(ecsLight.color, ecsLight.direction, ecsLight.position,
                    ecsLight.innerTheta, ecsLight.outerTheta, ecsLight.intensity);
        };
    }

    private void collectRenderables(RenderScene scene) {
        for (int e : entities) {
            Renderable r = renderables.getComponent(e);
            if (r == null) continue;

            Transform t = transforms.getComponent(e);
            if (t == null) continue;

            scene.submit(new RenderCommand(r.mesh, r.material, t.getMatrix()));
        }
    }
}
