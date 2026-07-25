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
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.core.ECS.basicComponents.*;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Deprecated
public class SceneCollector extends ClientSystem {

    private final SparseSet<CameraComponent> cameras;
    private final SparseSet<TransformComp> transforms;
    private final SparseSet<LightComp> ecsLights;
    private final SparseSet<RenderableComp> renderables;
    private final SparseSet<LightEnvComponent> lightEnvs;
    private final SparseSet<TextureManagerComponent> textureMnrs;
    private final Map<Integer, Light> activeLights = new HashMap<>();

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private final PerspectiveProjection persp = new PerspectiveProjection(0, 0, 0);
    private final Quaternionf conjugateTmp = new Quaternionf();

    {
        requiredComponents.add(TransformComp.class);
    }

    public SceneCollector(FoolsEngine engine) {
        super(engine);
        cameras = getSparseSet(CameraComponent.class);
        transforms = getSparseSet(TransformComp.class);
        ecsLights = getSparseSet(LightComp.class);
        renderables = getSparseSet(RenderableComp.class);
        lightEnvs = getSparseSet(LightEnvComponent.class);
        textureMnrs = getSparseSet(TextureManagerComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        collectInfrastructure(scene);
        collectCamera(scene);
        collectLights(scene);
        collectRenderables(scene);
    }

    private void collectInfrastructure(RenderScene scene) {
        for (int e : entities) {
            LightEnvComponent le = lightEnvs.getComponent(e);
            if (le != null) {
                scene.setLighting(le.env);
            }
            TextureManagerComponent tm = textureMnrs.getComponent(e);
            if (tm != null) {
                scene.setTextureManager(tm.manager);
            }
        }
    }

    private void collectCamera(RenderScene scene) {
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (cam == null || !cam.active) continue;

            deactivateOtherCam(e);

            TransformComp t = transforms.getComponent(e);
            if (t == null) continue;

            view.identity().set(t.getMatrix().invert());
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

        if (env.getLights().isEmpty() && !activeLights.isEmpty()) {
            activeLights.clear();
            return;
        }

        toRemove.removeAll(current);

        for (int eid : toRemove) {
            Light removed = activeLights.remove(eid);
            if (removed != null) env.remove(removed);
        }

        for (int eid : current) {
            if (activeLights.containsKey(eid)) continue;

            LightComp LightComp = ecsLights.getComponent(eid);
            if (LightComp == null) continue;

            Light apiLight = convertToApiLight(LightComp);

            if (LightComp.castsShadow && mainCamera != null) {
                switch (LightComp.lightType) {
                    case PARALLEL -> apiLight = env.enableDirLightShadow(apiLight, mainCamera);
                    case SPOT -> apiLight = env.enableSpotLightShadow(apiLight, LightComp.shadowNear);
                }
            }

            env.add(apiLight);
            activeLights.put(eid, apiLight);
        }
    }

    private Light convertToApiLight(LightComp lightComp) {
        return switch (lightComp.lightType) {
            case PARALLEL -> Light.directional(lightComp.color, lightComp.direction, lightComp.intensity);
            case POINT -> Light.point(lightComp.color, lightComp.position, lightComp.intensity);
            case SPOT -> Light.spot(lightComp.color, lightComp.direction, lightComp.position,
                    lightComp.innerTheta, lightComp.outerTheta, lightComp.intensity);
        };
    }

    private void collectRenderables(RenderScene scene) {
        for (int e : entities) {
            RenderableComp r = renderables.getComponent(e);
            if (r == null) continue;

            TransformComp t = transforms.getComponent(e);
            if (t == null) continue;

            scene.submit(new RenderCommand(r.mesh, r.material, t.getMatrix()));
        }
    }
}
