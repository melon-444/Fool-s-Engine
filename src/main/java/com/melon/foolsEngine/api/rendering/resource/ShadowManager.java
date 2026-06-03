package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

/**
 * Manages shadow map resources: layer allocation, shadow camera creation, and per-frame shadow preparation.
 * <p>
 * Does <b>not</b> execute any draw commands — rendering is performed by {@link com.melon.foolsEngine.api.rendering.render.RenderFrame}
 * using the {@link ShadowPassContext} returned by {@link #prepareShadow(Light, Camera)}.
 * <p>
 * Typical usage:
 * <pre>{@code
 *   ShadowManager sm = new ShadowManager(shadowArray, depthMaterial, maxLayers);
 *   frame.setShadowManager(sm);
 *
 *   Light dirLight = sm.enableDirLightShadow(baseLight, mainCamera);
 *   lightEnv.add(dirLight);
 *   // Shadow pass is handled automatically by frame.render(scene)
 * }</pre>
 */
public class ShadowManager {

    private final RenderTarget shadowArray;
    private final Material depthMaterial;
    private final int maxLayers;
    private int nextLayer;

    /**
     * @param shadowArray the depth texture array render target for shadow maps
     * @param depthMaterial the depth-only material used for shadow rendering
     * @param maxLayers maximum number of shadow-casting lights
     */
    public ShadowManager(RenderTarget shadowArray, Material depthMaterial, int maxLayers) {
        this.shadowArray = shadowArray;
        this.depthMaterial = depthMaterial;
        this.maxLayers = maxLayers;
        this.nextLayer = 0;
    }

    /**
     * Allocates a shadow layer and returns a new directional light with shadow support.
     * @param light the base light (color, direction copied)
     * @param mainCamera used to build the initial shadow camera frustum
     * @return a new Light with shadow info — add this to your LightEnvironment
     */
    public Light enableDirLightShadow(Light light, Camera mainCamera) {
        int layer = allocateLayer();
        Camera shadowCam = new Camera(new Matrix4f(), new Matrix4f());
        Matrix4f lsMatrix = new Matrix4f();

        Light shadowLight = Light.directional(
                light.color, light.direction, light.intensity,
                Collections.singletonList(shadowArray),
                Collections.singletonList(lsMatrix),
                layer, shadowCam);

        shadowLight.buildDirLightShadowCam(mainCamera);
        return shadowLight;
    }

    /**
     * Allocates a shadow layer and returns a new spot light with shadow support.
     * @param light the base light (color, direction, position, cone angles copied)
     * @param shadowNear near plane distance for the spot shadow camera
     * @return a new Light with shadow info — add this to your LightEnvironment
     */
    public Light enableSpotLightShadow(Light light, float shadowNear) {
        int layer = allocateLayer();
        Matrix4f lightSpace = new Matrix4f();

        return Light.spot(light.color, light.direction, light.position,
                light.innerTheta, light.outerTheta, light.intensity,
                Collections.singletonList(shadowArray),
                Collections.singletonList(lightSpace),
                layer, shadowNear);
    }

    /**
     * Prepares shadow rendering context for a single light.
     * Updates the light's shadow camera and light-space matrix, then returns a context
     * that the renderer uses to execute the shadow pass.
     * <p>
     * Called each frame by the renderer, not by user code.
     *
     * @param light the shadow-casting light
     * @param mainCamera the main scene camera (needed for directional shadow frustum fitting)
     * @return a context bundling the shadow camera, target, material, and layer
     */
    public ShadowPassContext prepareShadow(Light light, Camera mainCamera) {
        if (light.type == Light.DIRECTIONAL) {
            light.buildDirLightShadowCam(mainCamera);
            light.shadowInfo.lightSpaceMatrices().getFirst().set(light.shadowInfo.shadowCamera().vp());
        } else if (light.type == Light.SPOT) {
            light.shadowInfo.lightSpaceMatrices().getFirst().set(light.shadowInfo.shadowCamera().vp());
        }

        return new ShadowPassContext(
                light.shadowInfo.shadowCamera(),
                shadowArray,
                depthMaterial,
                light.shadowInfo.shadowLayer());
    }

    /** Resets the layer allocator. Call when clearing all lights. */
    public void reset() {
        nextLayer = 0;
    }

    /** Destroys the underlying shadow array render target */
    public void destroy() {
        shadowArray.destroy();
    }

    /** @return the shadow map texture array */
    public RenderTarget getShadowArray() {
        return shadowArray;
    }

    /** @return the depth-only material */
    public Material getDepthMaterial() {
        return depthMaterial;
    }

    private int allocateLayer() {
        if (nextLayer >= maxLayers) {
            throw new IllegalStateException("Shadow layer limit exceeded: " + maxLayers);
        }
        return nextLayer++;
    }
}
