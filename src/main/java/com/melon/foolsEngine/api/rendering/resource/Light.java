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

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowInfo;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowManager;
import com.melon.foolsEngine.util.LightType;
import com.melon.foolsEngine.util.OrthogonalProjection;
import com.melon.foolsEngine.util.PerspectiveProjection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

import static java.lang.Math.*;

/**
 * Defines a light source for the rendering pipeline.
 * Supports three light types: directional, point, and spot.
 * A light may optionally cast shadows when configured via {@link ShadowManager}.
 * <p>
 * Use the static factory methods to create lights:
 * <pre>{@code
 *   Light dir = Light.directional(color, direction);
 *   Light pt  = Light.point(color, position, 3.0f);
 *   Light sp  = Light.spot(color, direction, position, inner, outer);
 * }</pre>
 */
public class Light {

    /** Parallel light (aka directional / sun light) */
    public static final LightType DIRECTIONAL = LightType.PARALLEL;
    /** Omnidirectional point light with attenuation */
    public static final LightType POINT = LightType.POINT;
    /** Spotlight with inner/outer cone angles */
    public static final LightType SPOT = LightType.SPOT;

    private static final float FRUSTUM_Z_NEAR = 1.0f;
    private static final float FRUSTUM_Z_FAR = 0.0002f;

    /** The light type ({@link #DIRECTIONAL}, {@link #POINT}, or {@link #SPOT}) */
    public final LightType type;
    /** RGB color of the light */
    public final Vector3f color;
    /** Normalized direction vector (primarily for directional and spot lights) */
    public final Vector3f direction;
    /** Position in world space (primarily for point and spot lights) */
    public final Vector3f position;
    /** Intensity multiplier */
    public final float intensity;
    /** Inner cone angle in degrees (spot lights only) */
    public final float innerTheta;
    /** Outer cone angle in degrees (spot lights only) */
    public final float outerTheta;
    /** Shadow data; null if this light does not cast shadows */
    public final ShadowInfo shadowInfo;

    private Light(LightType type, Vector3f color, Vector3f direction, Vector3f position,
                  float intensity, float innerTheta, float outerTheta,
                  List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                  int shadowLayer, Camera shadowCamera) {
        this.type = type;
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.intensity = intensity;
        this.innerTheta = innerTheta;
        this.outerTheta = outerTheta;
        this.shadowInfo = (shadowMaps != null && lightSpaceMatrices != null)
                ? new ShadowInfo(shadowMaps, lightSpaceMatrices, shadowLayer, shadowCamera)
                : null;
    }

    /**
     * Creates a directional light with default intensity (1.0).
     * @param color RGB color
     * @param direction light direction (will be normalized)
     */
    public static Light directional(Vector3f color, Vector3f direction) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(),
                1.0f, 0f, 0f, null, null, -1, null);
    }

    /**
     * Creates a directional light.
     * @param color RGB color
     * @param direction light direction (will be normalized)
     * @param intensity brightness multiplier
     */
    public static Light directional(Vector3f color, Vector3f direction, float intensity) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(),
                intensity, 0f, 0f, null, null, -1, null);
    }

    /**
     * Creates a directional light with shadow support.
     * Typically called by {@link ShadowManager#enableDirLightShadow(Light, Camera)} rather than directly.
     */
    public static Light directional(Vector3f color, Vector3f direction, float intensity,
                                    List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                                    int shadowLayer, Camera shadowCamera) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(),
                intensity, 0f, 0f, shadowMaps, lightSpaceMatrices, shadowLayer, shadowCamera);
    }

    /**
     * Creates a point light with default intensity (1.0).
     * @param color RGB color
     * @param position world-space position
     */
    public static Light point(Vector3f color, Vector3f position) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position),
                1.0f, 0f, 0f, null, null, -1, null);
    }

    /**
     * Creates a point light.
     * @param color RGB color
     * @param position world-space position
     * @param intensity brightness multiplier
     */
    public static Light point(Vector3f color, Vector3f position, float intensity) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position),
                intensity, 0f, 0f, null, null, -1, null);
    }

    /**
     * Creates a point light with shadow support.
     * Typically called by {@link ShadowManager}.
     */
    public static Light point(Vector3f color, Vector3f position, float intensity,
                              List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                              int shadowLayer, Camera shadowCamera) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position),
                intensity, 0f, 0f, shadowMaps, lightSpaceMatrices, shadowLayer, shadowCamera);
    }

    /**
     * Creates a spot light with default intensity (1.0).
     * @param color RGB color
     * @param direction light direction (will be normalized)
     * @param position world-space position
     * @param innerTheta inner cone angle in degrees
     * @param outerTheta outer cone angle in degrees
     */
    public static Light spot(Vector3f color, Vector3f direction, Vector3f position,
                             float innerTheta, float outerTheta) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position),
                1.0f, innerTheta, outerTheta, null, null, -1, null);
    }

    /**
     * Creates a spot light.
     * @param color RGB color
     * @param direction light direction (will be normalized)
     * @param position world-space position
     * @param innerTheta inner cone angle in degrees
     * @param outerTheta outer cone angle in degrees
     * @param intensity brightness multiplier
     */
    public static Light spot(Vector3f color, Vector3f direction, Vector3f position,
                             float innerTheta, float outerTheta, float intensity) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position),
                intensity, innerTheta, outerTheta, null, null, -1, null);
    }

    /**
     * Creates a spot light with shadow support.
     * Typically called by {@link ShadowManager#enableSpotLightShadow(Light, float)}.
     * @param shadowNear near plane distance for the shadow camera
     */
    public static Light spot(Vector3f color, Vector3f direction, Vector3f position,
                             float innerTheta, float outerTheta, float intensity,
                             List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                             int shadowLayer, float shadowNear) {
        Vector3f dir = new Vector3f(direction).normalize();
        Camera shadowCam = null;
        if (shadowMaps != null && !shadowMaps.isEmpty() && shadowLayer >= 0) {
            Matrix4f view = new Matrix4f().lookAt(position,
                    new Vector3f(position).add(dir),
                    new Vector3f(0, abs(dir.y) < 0.99f ? 1 : 0, 0));
            float fov = max(innerTheta, outerTheta) * 2;
            PerspectiveProjection proj = new PerspectiveProjection(fov, 1f, shadowNear);
            shadowCam = new Camera(view, proj.get(new Matrix4f()));
        }
        return new Light(SPOT, color, dir, new Vector3f(position),
                intensity, innerTheta, outerTheta,
                shadowMaps, lightSpaceMatrices, shadowLayer, shadowCam);
    }

    /**
     * @return true if this light is configured to cast shadows
     */
    public boolean castsShadow() {
        return shadowInfo != null && shadowInfo.castsShadow();
    }

    /**
     * @deprecated Shadow camera updates are now handled by
     * {@link ShadowManager#prepareShadow(Light, Camera)} which calls the internal
     * {@code updateDirShadowCamera} / {@code updateSpotShadowCamera} methods.
     * This method is retained for backward compatibility only.
     */
    @Deprecated
    public void buildDirLightShadowCam(Camera mainCamera) {
        if (type != DIRECTIONAL || shadowInfo == null || shadowInfo.shadowCamera() == null) return;

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

        Vector3f dir = new Vector3f(direction).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        if (abs(dir.y) > 0.99f) up.set(1, 0, 0);
        Vector3f lightPos = new Vector3f(center).add(new Vector3f(dir).mul(-30));

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

        float halfW = (maxX - minX) * 0.5f + 15f;
        float halfH = (maxY - minY) * 0.5f + 15f;
        OrthogonalProjection ortho = new OrthogonalProjection(halfW, halfH, max(minZ - 30, 0.01f), maxZ + 30);

        shadowInfo.shadowCamera().view.set(lightView);
        shadowInfo.shadowCamera().projection = ortho.get(new Matrix4f());
    }
}
