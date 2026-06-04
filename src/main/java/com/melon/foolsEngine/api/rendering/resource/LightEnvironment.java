package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages a collection of up to 16 lights, an ambient color, and an optional
 * {@link ShadowManager} for shadow-casting lights.
 * Pushes light data to shader uniforms via {@link #apply(ShaderProgram)}.
 * <p>
 * Shadow usage:
 * <pre>{@code
 *   LightEnvironment env = new LightEnvironment();
 *   env.setAmbient(0.08f, 0.08f, 0.08f);
 *   env.enableShadows(shadowArray, depthMaterial, maxLayers);
 *
 *   Light dirLight = env.enableDirLightShadow(Light.directional(color, dir), camera);
 *   env.add(dirLight);
 *
 *   scene.setLighting(env);
 * }</pre>
 */
public class LightEnvironment {

    private static final int MAX_LIGHTS = 16;
    /** Texture unit slot used for the shadow map array in shaders */
    public static final int SHADOW_ARRAY_SLOT = 8;

    private final List<Light> lights = new ArrayList<>();
    private final Vector3f ambientColor = new Vector3f(0.06f, 0.06f, 0.06f);
    private int shadowMapSize = 1024;
    private ShadowManager shadowManager;

    /** Adds a light to the environment */
    public void add(Light light) {
        lights.add(light);
    }

    /**
     * Removes a light from the environment.
     * If the light casts shadows, its shadow layer is released for reuse.
     */
    public void remove(Light light) {
        if (shadowManager != null && light.castsShadow()) {
            shadowManager.releaseLayer(light.shadowInfo.shadowLayer());
        }
        lights.remove(light);
    }

    /**
     * Enables shadow support by creating an internal {@link ShadowManager}.
     * Must be called before {@link #enableDirLightShadow(Light, Camera)} or
     * {@link #enableSpotLightShadow(Light, float)}.
     *
     * @param shadowArray the depth texture array render target
     * @param depthMaterial the depth-only material used for shadow rendering
     * @param maxLayers maximum number of shadow-casting lights
     * @throws IllegalStateException if shadows are already enabled
     */
    public void enableShadows(RenderTarget shadowArray, Material depthMaterial, int maxLayers) {
        if (shadowManager != null) {
            throw new IllegalStateException("Shadows already enabled");
        }
        this.shadowManager = new ShadowManager(shadowArray, depthMaterial, maxLayers);
        this.shadowMapSize = shadowArray.getWidth();
    }

    /**
     * Allocates a shadow layer and returns a new directional light with shadow support.
     * Requires {@link #enableShadows(RenderTarget, Material, int)} to be called first.
     *
     * @param light the base light (color, direction copied)
     * @param mainCamera used to build the initial shadow camera frustum
     * @return a new Light with shadow info
     * @throws IllegalStateException if shadows are not enabled
     */
    public Light enableDirLightShadow(Light light, Camera mainCamera) {
        checkShadowEnabled();
        return shadowManager.enableDirLightShadow(light, mainCamera);
    }

    /**
     * Allocates a shadow layer and returns a new spot light with shadow support.
     * Requires {@link #enableShadows(RenderTarget, Material, int)} to be called first.
     *
     * @param light the base light (color, direction, position, cone angles copied)
     * @param shadowNear near plane distance for the spot shadow camera
     * @return a new Light with shadow info
     * @throws IllegalStateException if shadows are not enabled
     */
    public Light enableSpotLightShadow(Light light, float shadowNear) {
        checkShadowEnabled();
        return shadowManager.enableSpotLightShadow(light, shadowNear);
    }

    /** @return the {@link ShadowManager}, or null if shadows are not enabled */
    public ShadowManager getShadowManager() {
        return shadowManager;
    }

    /** Removes all lights and resets shadow layers */
    public void clear() {
        lights.clear();
        if (shadowManager != null) {
            shadowManager.reset();
        }
    }

    /**
     * Destroys the underlying shadow map resources.
     * Safe to call even if shadows are not enabled.
     */
    public void destroy() {
        if (shadowManager != null) {
            shadowManager.destroy();
        }
    }

    private void checkShadowEnabled() {
        if (shadowManager == null) {
            throw new IllegalStateException("Shadows not enabled. Call enableShadows() first.");
        }
    }

    /** Sets the ambient light color (per-component) */
    public void setAmbient(float r, float g, float b) {
        ambientColor.set(r, g, b);
    }

    /** Sets the ambient light color */
    public void setAmbient(Vector3f color) {
        ambientColor.set(color);
    }

    /** Sets the shadow map resolution (affects PCF sampling in the shader) */
    public void setShadowMapSize(int size) {
        this.shadowMapSize = size;
    }

    /** @return the current ambient color (mutable reference) */
    public Vector3f getAmbient() {
        return ambientColor;
    }

    /** @return an unmodifiable view of the current lights */
    public List<Light> getLights() {
        return Collections.unmodifiableList(lights);
    }

    /** @return the number of lights currently in the environment */
    public int size() {
        return lights.size();
    }

    /** @return the current shadow map resolution (affects PCF sampling) */
    public int getShadowMapSize() {
        return shadowMapSize;
    }

    /**
     * Pushes all light data and shadow map binding info to the given shader.
     * Called internally by the renderer.
     */
    public void apply(ShaderProgram shader) {
        shader.setVec3("ambientColor", ambientColor.x, ambientColor.y, ambientColor.z);
        int count = Math.min(lights.size(), MAX_LIGHTS);
        shader.setInt("lightCount", count);

        for (int i = 0; i < count; i++) {
            Light l = lights.get(i);
            String idx = "[" + i + "]";
            shader.setVec4("lightColor" + idx, l.color.x, l.color.y, l.color.z, l.intensity);
            shader.setVec4("lightDir" + idx, l.direction.x, l.direction.y, l.direction.z, 0f);
            float innerCutOff = (float) Math.cos(Math.toRadians(l.innerTheta));
            shader.setVec4("lightPos" + idx, l.position.x, l.position.y, l.position.z, innerCutOff);

            boolean hasShadow = l.castsShadow() && l.shadowInfo != null
                    && !l.shadowInfo.lightSpaceMatrices().isEmpty() && !l.shadowInfo.shadowMaps().isEmpty();
            float layer = hasShadow ? (float) l.shadowInfo.shadowLayer() : 0f;
            float outerCutOff = (float) Math.cos(Math.toRadians(l.outerTheta));
            shader.setVec4("lightParams" + idx, (float) l.type.ordinal(), outerCutOff, hasShadow ? 1f : 0f, layer);

            if (hasShadow) {
                Matrix4f lsMatrix = l.shadowInfo.lightSpaceMatrices().get(0);
                shader.setMat4("lightSpaceMatrices" + idx, lsMatrix.get(new float[16]));
            }
        }

        shader.setInt("shadowMapArray", SHADOW_ARRAY_SLOT);
        shader.setFloat("shadowMapSize", (float) shadowMapSize);
    }
}
