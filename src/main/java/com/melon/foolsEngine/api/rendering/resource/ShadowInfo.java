package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

/**
 * Immutable record holding shadow mapping data for a single light.
 * Managed by {@link ShadowManager} — not created directly by user code.
 *
 * @param shadowMaps the depth render targets (typically one element for a 2D array slice)
 * @param lightSpaceMatrices the light-space transformation matrices
 * @param shadowLayer the layer index in the shadow map array
 * @param shadowCamera the camera used to render from the light's perspective
 */
public record ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer,
                         Camera shadowCamera) {

    public ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                      int shadowLayer, Camera shadowCamera) {
        this.shadowMaps = shadowMaps != null ? List.copyOf(shadowMaps) : Collections.emptyList();
        this.lightSpaceMatrices = lightSpaceMatrices != null ? List.copyOf(lightSpaceMatrices) : Collections.emptyList();
        this.shadowLayer = shadowLayer;
        this.shadowCamera = shadowCamera;
    }

    /** @return true if this light has valid shadow mapping configuration */
    public boolean castsShadow() {
        return !shadowMaps.isEmpty() && !lightSpaceMatrices.isEmpty() && shadowLayer >= 0;
    }
}
