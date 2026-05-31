package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;


public class Material{
    private final ShaderProgram shader;
    private final Map<String, Object> params = new HashMap<>();

    public Material(ShaderProgram shader) {
        this.shader = shader;
    }

    public ShaderProgram shader() {
        return shader;
    }

    /**
     * set the parameter of this material instance
     * @param name param name
     * @param value the value to set
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

    public Map<String, Object> params() {
        return params;
    }

}