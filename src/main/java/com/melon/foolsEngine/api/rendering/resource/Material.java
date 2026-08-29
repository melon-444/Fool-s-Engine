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
package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Associates a {@link ShaderProgram} with its uniform parameter values.
 * Parameters are stored by name and support Float, Integer, JOML vectors/matrices, and Texture.
 * <p>
 * Usage:
 * <pre>{@code
 *   Material mat = new Material(shader);
 *   mat.set("textureSampler", texture);
 *   mat.set("roughness", 0.5f);
 * }</pre>
 */
public class Material{
    private final ShaderProgram shader;
    private final Map<String, Object> params = new HashMap<>();
    private boolean transparent = false;

    /**
     * @param shader the shader program this material binds to
     */
    public Material(ShaderProgram shader) {
        this.shader = shader;
    }

    /**
     * Marks this material as transparent. Transparent materials are drawn by a
     * CORE pass with a non-{@code OPAQUE} blend mode (back-to-front), while
     * opaque materials are drawn by the regular opaque pass.
     * @param transparent true to treat the material as transparent
     */
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    /** @return true when this material should be rendered in a blended pass */
    public boolean isTransparent() {
        return transparent;
    }

    /** @return the shader program used by this material */
    public ShaderProgram shader() {
        return shader;
    }

    /**
     * Sets a uniform parameter for this material instance.
     * Supported types: {@link Float}, {@link Integer}, {@link Vector2f}, {@link Vector3f},
     * {@link Vector4f}, {@link Matrix4f}, {@link Texture}.
     * @param name the uniform name in the shader
     * @param value the value to set
     * @throws IllegalArgumentException if the value type is unsupported
     */
    public void set(String name, Object value) {
        if (!(value instanceof Float ||
                value instanceof Integer ||
                value instanceof Vector2f ||
                value instanceof Vector3f ||
                value instanceof Vector4f ||
                value instanceof Matrix4f ||
                value instanceof Texture)) {
            throw new IllegalArgumentException("Unsupported material param type: " + value);
        }
        params.put(name, value);
    }

    /** @return the modifiable map of parameter name to value */
    public Map<String, Object> params() {
        return params;
    }

}
