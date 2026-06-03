package com.melon.foolsEngine.api.rendering.resource;

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

    /**
     * @param shader the shader program this material binds to
     */
    public Material(ShaderProgram shader) {
        this.shader = shader;
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
