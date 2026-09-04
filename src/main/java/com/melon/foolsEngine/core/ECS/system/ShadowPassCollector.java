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

import com.melon.foolsEngine.api.rendering.shader.StandardPasses;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowManager;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.annotation.OnlyIn;
import com.melon.foolsEngine.util.Distribution;
import org.joml.Matrix4f;

/**
 * Optional built-in client system that contributes standard shadow passes.
 *
 * <p>Register this system explicitly to enable the engine's standard shadow
 * pipeline. Users may omit it or replace it with their own ClientSystem.</p>
 */
@OnlyIn(Distribution.Client)
public final class ShadowPassCollector extends ClientSystem {

    public ShadowPassCollector(FoolsEngine engine) {
        super(engine);
    }

    @Override
    public int collectionOrder() {
        return 40;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        LightEnvironment lighting = scene.getLighting();
        Camera mainCamera = scene.getCamera();
        if (lighting == null || mainCamera == null) {
            return;
        }

        ShadowManager shadowManager = lighting.getShadowManager();
        if (shadowManager == null) {
            return;
        }

        Camera cameraCopy = new Camera(
                new Matrix4f(mainCamera.view),
                new Matrix4f(mainCamera.projection));

        for (Light light : lighting.getLights()) {
            if (!light.castsShadow()) {
                continue;
            }

            ShadowPassContext context =
                    shadowManager.prepareShadow(light, cameraCopy);
            scene.submitPass(StandardPasses.shadow(context).build());
        }
    }
}
