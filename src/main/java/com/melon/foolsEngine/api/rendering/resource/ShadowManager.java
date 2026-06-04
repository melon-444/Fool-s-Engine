package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.util.OrthogonalProjection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Math.*;

/**
 * Manages shadow map resources: layer allocation, shadow camera creation, and per-frame shadow preparation.
 * <p>
 * Does <b>not</b> execute any draw commands — rendering is performed by {@link com.melon.foolsEngine.api.rendering.render.RenderFrame}
 * using the {@link ShadowPassContext} returned by {@link #prepareShadow(Light, Camera)}.
 * <p>
 * Typical usage:
 * <pre>{@code
 *   LightEnvironment lightEnv = new LightEnvironment();
 *   lightEnv.enableShadows(shadowArray, depthMaterial, maxLayers);
 *
 *   Light dirLight = lightEnv.enableDirLightShadow(baseLight, mainCamera);
 *   lightEnv.add(dirLight);
 *   // Shadow pass is handled automatically by frame.render(scene)
 * }</pre>
 */
public class ShadowManager {

    private static final float FRUSTUM_Z_NEAR = 1.0f;
    private static final float FRUSTUM_Z_FAR = 0.0002f;
    private static final float DIR_SHADOW_BACK_OFFSET = 30f;
    private static final float DIR_SHADOW_XY_PADDING = 15f;
    private static final float DIR_SHADOW_Z_PADDING = 30f;

    private final RenderTarget shadowArray;
    private final Material depthMaterial;
    private final int maxLayers;
    private final Set<Integer> releasedLayers = new HashSet<>();
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

        updateDirShadowCamera(shadowLight, mainCamera);
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
            updateDirShadowCamera(light, mainCamera);
        } else if (light.type == Light.SPOT) {
            updateSpotShadowCamera(light);
        }

        light.shadowInfo.lightSpaceMatrices().getFirst().set(
                light.shadowInfo.shadowCamera().vp());

        return new ShadowPassContext(
                light.shadowInfo.shadowCamera(),
                shadowArray,
                depthMaterial,
                light.shadowInfo.shadowLayer());
    }

    /**
     * Rebuilds a directional light's shadow camera frustum from the main camera's view.
     * Formerly part of {@link Light#buildDirLightShadowCam(Camera)}.
     */
    private void updateDirShadowCamera(Light light, Camera mainCamera) {
        if (light.type != Light.DIRECTIONAL || light.shadowInfo == null
                || light.shadowInfo.shadowCamera() == null) return;

        Vector4f[] ndc = {
            new Vector4f(-1, -1, FRUSTUM_Z_NEAR, 1), new Vector4f(1, -1, FRUSTUM_Z_NEAR, 1),
            new Vector4f(-1,  1, FRUSTUM_Z_NEAR, 1), new Vector4f(1,  1, FRUSTUM_Z_NEAR, 1),
            new Vector4f(-1, -1, FRUSTUM_Z_FAR,  1), new Vector4f(1, -1, FRUSTUM_Z_FAR,  1),
            new Vector4f(-1,  1, FRUSTUM_Z_FAR,  1), new Vector4f(1,  1, FRUSTUM_Z_FAR,  1),
        };

        Matrix4f invVP = new Matrix4f(mainCamera.vp());
        invVP.invert();

        Vector3f[] corners = new Vector3f[8];
        Vector3f center = new Vector3f();
        for (int i = 0; i < 8; i++) {
            Vector4f w = ndc[i].mul(invVP, new Vector4f());
            float invW = 1f / w.w;
            corners[i] = new Vector3f(w.x * invW, w.y * invW, w.z * invW);
            center.add(corners[i]);
        }
        center.div(8);

        Vector3f dir = new Vector3f(light.direction).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        if (abs(dir.y) > 0.99f) up.set(1, 0, 0);
        Vector3f lightPos = new Vector3f(center).add(new Vector3f(dir).mul(-DIR_SHADOW_BACK_OFFSET));

        Matrix4f lightView = new Matrix4f().lookAt(lightPos, center, up);

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Vector3f c : corners) {
            Vector4f ls = new Vector4f(c, 1).mul(lightView);
            minX = min(minX, ls.x); maxX = max(maxX, ls.x);
            minY = min(minY, ls.y); maxY = max(maxY, ls.y);
            minZ = min(minZ, -ls.z); maxZ = max(maxZ, -ls.z);
        }

        float halfW = (maxX - minX) * 0.5f + DIR_SHADOW_XY_PADDING;
        float halfH = (maxY - minY) * 0.5f + DIR_SHADOW_XY_PADDING;
        OrthogonalProjection ortho = new OrthogonalProjection(halfW, halfH,
                max(minZ - DIR_SHADOW_Z_PADDING, 0.01f), maxZ + DIR_SHADOW_Z_PADDING);

        Camera shadowCam = light.shadowInfo.shadowCamera();
        shadowCam.view.set(lightView);
        shadowCam.projection = ortho.get(new Matrix4f());
    }

    /**
     * Rebuilds a spot light's shadow camera view matrix from the light's current position
     * and direction. The perspective projection remains unchanged after creation.
     */
    private void updateSpotShadowCamera(Light light) {
        if (light.type != Light.SPOT || light.shadowInfo == null
                || light.shadowInfo.shadowCamera() == null) return;

        Camera shadowCam = light.shadowInfo.shadowCamera();
        Vector3f dir = new Vector3f(light.direction).normalize();
        Vector3f up = new Vector3f(0, abs(dir.y) < 0.99f ? 1 : 0, 0);
        shadowCam.view.identity().lookAt(
                light.position,
                new Vector3f(light.position).add(dir),
                up);
    }

    /** Resets the layer allocator and free list. Call when clearing all lights. */
    public void reset() {
        nextLayer = 0;
        releasedLayers.clear();
    }

    /**
     * Releases a shadow layer for reuse by future shadow-casting lights.
     * Called automatically by {@link LightEnvironment#remove(Light)}.
     * Safe to call with layers not currently allocated (no-op).
     *
     * @param layer the layer index to release (from {@link Light#shadowInfo#shadowLayer})
     */
    public void releaseLayer(int layer) {
        if (layer >= 0 && layer < nextLayer) {
            releasedLayers.add(layer);
        }
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

    /** @return the number of currently active (non-released) shadow layers */
    public int getCurrentLayerCount() {
        return nextLayer - releasedLayers.size();
    }

    /** @return the maximum number of shadow layers */
    public int getMaxLayers() {
        return maxLayers;
    }

    private int allocateLayer() {
        if (!releasedLayers.isEmpty()) {
            int layer = releasedLayers.iterator().next();
            releasedLayers.remove(layer);
            return layer;
        }
        if (nextLayer >= maxLayers) {
            throw new IllegalStateException("Shadow layer limit exceeded: " + maxLayers);
        }
        return nextLayer++;
    }
}
