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

import com.melon.foolsEngine.api.APIFactory;

import java.util.Objects;

/**
 * Typed bundle of shaders shipped with the engine.
 *
 * <p>Loading is explicit because shader programs belong to the current
 * graphics backend/context. The caller owns the returned programs.</p>
 */
public record BuiltinShaders(
        ShaderProgram main,
        ShaderProgram shadowDepth
) implements AutoCloseable {

    public BuiltinShaders {
        Objects.requireNonNull(main, "main");
        Objects.requireNonNull(shadowDepth, "shadowDepth");
    }

    /** Loads the built-in main and shadow-depth shader programs. */
    public static BuiltinShaders load(APIFactory factory) {
        Objects.requireNonNull(factory, "factory");

        ShaderProgram main = factory.getShaderProgram();
        ShaderProgram shadowDepth = null;
        try {
            main.load(
                    "/shader/main/main_vsh.glsl",
                    "/shader/main/main_fsh.glsl");

            shadowDepth = factory.getShaderProgram();
            shadowDepth.load(
                    "/shader/depth/depth_vsh.glsl",
                    "/shader/depth/depth_fsh.glsl");

            return new BuiltinShaders(main, shadowDepth);
        } catch (RuntimeException | Error failure) {
            if (shadowDepth != null) {
                shadowDepth.destroy();
            }
            main.destroy();
            throw failure;
        }
    }

    /** Releases both GPU shader programs. */
    @Override
    public void close() {
        main.destroy();
        shadowDepth.destroy();
    }
}
