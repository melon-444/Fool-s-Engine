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

package com.melon.foolsEngine.api.rendering.pipeline;

import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;

import java.util.Objects;

/**
 * Optional templates for non-trivial, commonly used rendering passes.
 *
 * <p>Templates only return configurable builders. They never create ECS
 * entities, register systems, or submit passes to a scene.</p>
 */
public final class StandardPasses {

    private StandardPasses() {
    }

    /**
     * Builds the standard depth-only shadow configuration for a prepared
     * shadow context.
     */
    public static ShaderPass.Builder shadow(ShadowPassContext context) {
        Objects.requireNonNull(context, "context");

        return ShaderPass.core()
                .output(context.target())
                .camera(context.shadowCamera())
                .overrideMaterial(context.depthMaterial())
                .arrayLayer(context.layer())
                .colorOps(
                        ShaderPass.LoadOp.DONT_CARE,
                        ShaderPass.StoreOp.DONT_CARE)
                .depthOps(
                        ShaderPass.LoadOp.CLEAR,
                        ShaderPass.StoreOp.STORE)
                .clearDepth(0.0);
    }
}
