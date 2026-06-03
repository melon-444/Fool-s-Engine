package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;

public record ShadowPassContext(Camera shadowCamera, RenderTarget target, Material depthMaterial, int layer) {
}
