package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

public class ShadowManager {

    private final RenderFrame frame;
    private final RenderTarget shadowArray;
    private final Material depthMaterial;
    private final int maxLayers;
    private int nextLayer;

    public ShadowManager(RenderFrame frame, RenderTarget shadowArray, Material depthMaterial, int maxLayers) {
        this.frame = frame;
        this.shadowArray = shadowArray;
        this.depthMaterial = depthMaterial;
        this.maxLayers = maxLayers;
        this.nextLayer = 0;
    }

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

    public Light enableSpotLightShadow(Light light, float shadowNear) {
        int layer = allocateLayer();
        Matrix4f lightSpace = new Matrix4f();

        return Light.spot(light.color, light.direction, light.position,
                light.innerTheta, light.outerTheta, light.intensity,
                Collections.singletonList(shadowArray),
                Collections.singletonList(lightSpace),
                layer, shadowNear);
    }

    public void renderShadows(List<Light> lights, Camera mainCamera) {
        for (Light light : lights) {
            if (!light.castsShadow()) continue;

            if (light.type == Light.DIRECTIONAL) {
                light.buildDirLightShadowCam(mainCamera);
                light.shadowInfo.lightSpaceMatrices.get(0).set(light.shadowInfo.shadowCamera.vp());
            } else if (light.type == Light.SPOT) {
                light.shadowInfo.lightSpaceMatrices.get(0).set(light.shadowInfo.shadowCamera.vp());
            }

            frame.setCamera(light.shadowInfo.shadowCamera);
            frame.endFrame(shadowArray, depthMaterial, light.shadowInfo.shadowLayer);
        }
    }

    public void reset() {
        nextLayer = 0;
    }

    public void destroy() {
        shadowArray.destroy();
    }

    private int allocateLayer() {
        if (nextLayer >= maxLayers) {
            throw new IllegalStateException("Shadow layer limit exceeded: " + maxLayers);
        }
        return nextLayer++;
    }
}
