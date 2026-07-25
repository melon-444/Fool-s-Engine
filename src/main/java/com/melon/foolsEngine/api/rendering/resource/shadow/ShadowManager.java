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

package com.melon.foolsEngine.api.rendering.resource.shadow;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.util.OrthogonalProjection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Collections;
import java.util.HashSet;
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
    private static final int FRUSTUM_DEPTH_SAMPLES = 4;
    private static final float DIR_SHADOW_BACK_OFFSET = 30f;
    private static final float DIR_SHADOW_XY_PADDING = 15f;
    private static final float DIR_SHADOW_Z_NEAR_PAD = 30f;
    private static final float DIR_SHADOW_Z_FAR_PAD = 30f;

    private final RenderTarget shadowArray;
    private final Material depthMaterial;
    private final int maxLayers;
    private final Set<Integer> releasedLayers = new HashSet<>();
    private int nextLayer;

    private final Vector4f[] ndcCorners = {
        new Vector4f(-1, -1, FRUSTUM_Z_NEAR, 1), new Vector4f(1, -1, FRUSTUM_Z_NEAR, 1),
        new Vector4f(-1,  1, FRUSTUM_Z_NEAR, 1), new Vector4f(1,  1, FRUSTUM_Z_NEAR, 1),
        new Vector4f(-1, -1, FRUSTUM_Z_FAR,  1), new Vector4f(1, -1, FRUSTUM_Z_FAR,  1),
        new Vector4f(-1,  1, FRUSTUM_Z_FAR,  1), new Vector4f(1,  1, FRUSTUM_Z_FAR,  1),
    };
    private final Vector3f frustumCenter = new Vector3f();
    private final Vector3f lightPos = new Vector3f();
    private final Vector3f shadowDir = new Vector3f();
    private final Vector3f shadowUp = new Vector3f();
    private final Matrix4f invVP = new Matrix4f();
    private final Matrix4f lightView = new Matrix4f();
    private final Vector4f tmpVec4 = new Vector4f();
    private final OrthogonalProjection reusableOrtho = new OrthogonalProjection(0, 0, 0, 0);
    private final Vector3f spotDir = new Vector3f();
    private final Vector3f spotUp = new Vector3f();
    private final Vector3f spotTarget = new Vector3f();

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

        invVP.set(mainCamera.vp()).invert();

        frustumCenter.zero();
        for (int i = 0; i < 8; i++) {
            ndcCorners[i].mul(invVP, tmpVec4);
            float invW = 1f / tmpVec4.w;
            frustumCenter.add(tmpVec4.x * invW, tmpVec4.y * invW, tmpVec4.z * invW);
        }
        frustumCenter.div(8);

        shadowDir.set(light.direction).normalize();
        shadowUp.set(0, 1, 0);
        if (abs(shadowDir.y) > 0.99f) shadowUp.set(1, 0, 0);
        lightPos.set(frustumCenter).fma(-DIR_SHADOW_BACK_OFFSET, shadowDir);
        lightView.identity().lookAt(lightPos, frustumCenter, shadowUp);

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int layer = 0; layer < FRUSTUM_DEPTH_SAMPLES; layer++) {
            float ndcZ = FRUSTUM_Z_NEAR + (float) layer / (FRUSTUM_DEPTH_SAMPLES - 1)
                    * (FRUSTUM_Z_FAR - FRUSTUM_Z_NEAR);
            for (int corner = 0; corner < 4; corner++) {
                float ndcX = (corner & 1) == 0 ? -1f : 1f;
                float ndcY = (corner & 2) == 0 ? -1f : 1f;

                tmpVec4.set(ndcX, ndcY, ndcZ, 1f).mul(invVP);
                float invW = 1f / tmpVec4.w;
                float wx = tmpVec4.x * invW;
                float wy = tmpVec4.y * invW;
                float wz = tmpVec4.z * invW;

                tmpVec4.set(wx, wy, wz, 1f).mul(lightView);
                minX = min(minX, tmpVec4.x); maxX = max(maxX, tmpVec4.x);
                minY = min(minY, tmpVec4.y); maxY = max(maxY, tmpVec4.y);
                minZ = min(minZ, -tmpVec4.z); maxZ = max(maxZ, -tmpVec4.z);
            }
        }

        float spanX = maxX - minX;
        float spanY = maxY - minY;
        float spanZ = maxZ - minZ;
        float adaptivePadXY = max(spanX, spanY) * 0.15f;
        float xyPad = max(DIR_SHADOW_XY_PADDING, adaptivePadXY);
        float zFarPad = max(spanZ * 0.4f, max(adaptivePadXY, DIR_SHADOW_Z_FAR_PAD));
        float zNearPad = max(zFarPad, DIR_SHADOW_Z_NEAR_PAD);

        reusableOrtho.right = spanX * 0.5f + xyPad;
        reusableOrtho.top = spanY * 0.5f + xyPad;
        reusableOrtho.near = max(minZ - zNearPad, 0.01f);
        reusableOrtho.far = maxZ + zFarPad;

        Camera shadowCam = light.shadowInfo.shadowCamera();
        shadowCam.view.set(lightView);
        reusableOrtho.get(shadowCam.projection);
    }

    /**
     * Rebuilds a spotlight's shadow camera view matrix from the light's current position
     * and direction. The perspective projection remains unchanged after creation.
     */
    private void updateSpotShadowCamera(Light light) {
        if (light.type != Light.SPOT || light.shadowInfo == null
                || light.shadowInfo.shadowCamera() == null) return;

        Camera shadowCam = light.shadowInfo.shadowCamera();
        spotDir.set(light.direction).normalize();
        spotUp.set(0, abs(spotDir.y) < 0.99f ? 1 : 0, 0);
        spotTarget.set(light.position).add(spotDir);
        shadowCam.view.identity().lookAt(light.position, spotTarget, spotUp);
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
