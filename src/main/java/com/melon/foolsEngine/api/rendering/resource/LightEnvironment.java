package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LightEnvironment {

    private static final int MAX_LIGHTS = 16;
    public static final int SHADOW_ARRAY_SLOT = 8;

    private final List<Light> lights = new ArrayList<>();
    private final Vector3f ambientColor = new Vector3f(0.06f, 0.06f, 0.06f);

    public void add(Light light) {
        lights.add(light);
    }

    public void remove(Light light) {
        lights.remove(light);
    }

    public void clear() {
        lights.clear();
    }

    public void setAmbient(float r, float g, float b) {
        ambientColor.set(r, g, b);
    }

    public void setAmbient(Vector3f color) {
        ambientColor.set(color);
    }

    public Vector3f getAmbient() {
        return ambientColor;
    }

    public List<Light> getLights() {
        return Collections.unmodifiableList(lights);
    }

    public int size() {
        return lights.size();
    }

    public void apply(ShaderProgram shader) {
        shader.setVec3("ambientColor", ambientColor.x, ambientColor.y, ambientColor.z);
        int count = Math.min(lights.size(), MAX_LIGHTS);
        shader.setInt("lightCount", count);

        for (int i = 0; i < count; i++) {
            Light l = lights.get(i);
            String idx = "[" + i + "]";
            shader.setVec4("lightColor" + idx, l.color.x, l.color.y, l.color.z, l.intensity);
            shader.setVec4("lightDir" + idx, l.direction.x, l.direction.y, l.direction.z, 0f);
            shader.setVec4("lightPos" + idx, l.position.x, l.position.y, l.position.z, 0f);

            boolean hasShadow = l.castsShadow() && l.lightSpaceMatrices != null
                    && !l.lightSpaceMatrices.isEmpty() && !l.shadowMaps.isEmpty();
            float layer = hasShadow ? (float) l.shadowLayer : 0f;
            shader.setVec4("lightParams" + idx, (float) l.type, l.cutOff, hasShadow ? 1f : 0f, layer);

            if (hasShadow) {
                Matrix4f lsMatrix = l.lightSpaceMatrices.get(0);
                shader.setMat4("lightSpaceMatrices" + idx, lsMatrix.get(new float[16]));
            }
        }

        shader.setInt("shadowMapArray", SHADOW_ARRAY_SLOT);
    }
}
