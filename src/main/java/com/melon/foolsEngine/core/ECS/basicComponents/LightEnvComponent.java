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
package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;

public class LightEnvComponent extends Component {

    public final LightEnvironment env;

    public LightEnvComponent() {
        this.env = new LightEnvironment();
    }

    public LightEnvComponent(LightEnvironment env) {
        this.env = env;
    }

    public LightEnvComponent(float ambientR, float ambientG, float ambientB) {
        this.env = new LightEnvironment();
        this.env.setAmbient(ambientR, ambientG, ambientB);
    }

    public void enableShadows(RenderTarget shadowArray, Material depthMaterial, int maxLayers) {
        env.enableShadows(shadowArray, depthMaterial, maxLayers);
    }

    public void destroy() {
        env.destroy();
    }
}
