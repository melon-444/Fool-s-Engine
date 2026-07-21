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
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

/**
 * Immutable record holding shadow mapping data for a single light.
 * Managed by {@link ShadowManager} — not created directly by user code.
 *
 * @param shadowMaps the depth render targets (typically one element for a 2D array slice)
 * @param lightSpaceMatrices the light-space transformation matrices
 * @param shadowLayer the layer index in the shadow map array
 * @param shadowCamera the camera used to render from the light's perspective
 */
public record ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer,
                         Camera shadowCamera) {

    public ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                      int shadowLayer, Camera shadowCamera) {
        this.shadowMaps = shadowMaps != null ? List.copyOf(shadowMaps) : Collections.emptyList();
        this.lightSpaceMatrices = lightSpaceMatrices != null ? List.copyOf(lightSpaceMatrices) : Collections.emptyList();
        this.shadowLayer = shadowLayer;
        this.shadowCamera = shadowCamera;
    }

    /** @return true if this light has valid shadow mapping configuration */
    public boolean castsShadow() {
        return !shadowMaps.isEmpty() && !lightSpaceMatrices.isEmpty() && shadowLayer >= 0;
    }
}
