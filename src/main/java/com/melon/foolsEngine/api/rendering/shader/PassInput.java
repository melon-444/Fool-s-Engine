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

package com.melon.foolsEngine.api.rendering.shader;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;

/**
 * Binds a {@link RenderTarget} as a texture input to a {@link ShaderPass} sampler.
 * Used for post-process passes that read the output of preceding passes.
 */
public record PassInput(RenderTarget texture, String samplerName) {
    public PassInput {
        if (texture == null) throw new NullPointerException("texture must not be null");
        if (samplerName == null || samplerName.isBlank()) throw new NullPointerException("samplerName must not be blank");
    }
}
