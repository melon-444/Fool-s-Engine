package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.util.OrthogonalProjection;
import com.melon.foolsEngine.util.PerspectiveProjection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;

import static java.lang.Math.*;

public class Light {

    public static final int DIRECTIONAL = 0;
    public static final int POINT = 1;
    public static final int SPOT = 2;

    private static final float FRUSTUM_Z_NEAR = 1.0f;
    private static final float FRUSTUM_Z_FAR = 0.0002f;

    public final int type;
    public final Vector3f color;
    public final Vector3f direction;
    public final Vector3f position;
    public final float intensity;
    public final float innerTheta;
    public final float outerTheta;
    public final ShadowInfo shadowInfo;

    private Light(int type, Vector3f color, Vector3f direction, Vector3f position,
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

    public static Light directional(Vector3f color, Vector3f direction) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(),
                1.0f, 0f, 0f, null, null, -1, null);
    }

    public static Light directional(Vector3f color, Vector3f direction, float intensity) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(),
                intensity, 0f, 0f, null, null, -1, null);
    }

    public static Light directional(Vector3f color, Vector3f direction, float intensity,
                                    List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                                    int shadowLayer, Camera shadowCamera) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(),
                intensity, 0f, 0f, shadowMaps, lightSpaceMatrices, shadowLayer, shadowCamera);
    }

    public static Light point(Vector3f color, Vector3f position) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position),
                1.0f, 0f, 0f, null, null, -1, null);
    }

    public static Light point(Vector3f color, Vector3f position, float intensity) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position),
                intensity, 0f, 0f, null, null, -1, null);
    }

    public static Light point(Vector3f color, Vector3f position, float intensity,
                              List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                              int shadowLayer, Camera shadowCamera) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position),
                intensity, 0f, 0f, shadowMaps, lightSpaceMatrices, shadowLayer, shadowCamera);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position,
                             float innerTheta, float outerTheta) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position),
                1.0f, innerTheta, outerTheta, null, null, -1, null);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position,
                             float innerTheta, float outerTheta, float intensity) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position),
                intensity, innerTheta, outerTheta, null, null, -1, null);
    }

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

    public boolean castsShadow() {
        return shadowInfo != null && shadowInfo.castsShadow();
    }

    public void buildDirLightShadowCam(Camera mainCamera) {
        if (type != DIRECTIONAL || shadowInfo == null || shadowInfo.shadowCamera == null) return;

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

        shadowInfo.shadowCamera.view.set(lightView);
        shadowInfo.shadowCamera.projection = ortho.get(new Matrix4f());
    }
}
