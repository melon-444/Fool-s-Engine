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

package com.melon.foolsEngine.api.rendering.resource.shadow;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.Material;

/**
 * Returned by {@link ShadowManager#prepareShadow(Light, Camera)} to bundle
 * all data needed by the renderer to execute a single shadow pass.
 * <p>
 * Not created directly by user code.
 *
 * @param shadowCamera the camera positioned at the light source
 * @param target the shadow map render target
 * @param depthMaterial the depth-only material for shadow rendering
 * @param layer the layer index in the shadow map array
 */
public record ShadowPassContext(Camera shadowCamera, RenderTarget target, Material depthMaterial, int layer) {
}
