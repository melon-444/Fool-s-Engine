package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer,
                         Camera shadowCamera) {

    public ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                      int shadowLayer, Camera shadowCamera) {
        this.shadowMaps = shadowMaps != null ? List.copyOf(shadowMaps) : Collections.emptyList();
        this.lightSpaceMatrices = lightSpaceMatrices != null ? List.copyOf(lightSpaceMatrices) : Collections.emptyList();
        this.shadowLayer = shadowLayer;
        this.shadowCamera = shadowCamera;
    }

    public boolean castsShadow() {
        return !shadowMaps.isEmpty() && !lightSpaceMatrices.isEmpty() && shadowLayer >= 0;
    }
}
