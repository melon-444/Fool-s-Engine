package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShadowInfo {

    public final Camera shadowCamera;
    public final List<Matrix4f> lightSpaceMatrices;
    public final List<RenderTarget> shadowMaps;
    public final int shadowLayer;

    public ShadowInfo(List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices,
                      int shadowLayer, Camera shadowCamera) {
        this.shadowMaps = shadowMaps != null ? Collections.unmodifiableList(new ArrayList<>(shadowMaps)) : Collections.emptyList();
        this.lightSpaceMatrices = lightSpaceMatrices != null ? Collections.unmodifiableList(new ArrayList<>(lightSpaceMatrices)) : Collections.emptyList();
        this.shadowLayer = shadowLayer;
        this.shadowCamera = shadowCamera;
    }

    public boolean castsShadow() {
        return !shadowMaps.isEmpty() && !lightSpaceMatrices.isEmpty() && shadowLayer >= 0;
    }
}
