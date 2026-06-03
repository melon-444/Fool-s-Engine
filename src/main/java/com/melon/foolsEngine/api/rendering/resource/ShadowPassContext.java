package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;

/**
 * Returned by {@link ShadowManager#prepareShadow(Light, Camera)} to bundle
 * all data needed by the renderer to execute a single shadow pass.
 * <p>
 * Not created directly by user code.
 *
 * @param shadowCamera the camera positioned at the light source
 * @param target the shadow map render target
 * @param depthMaterial the depth-only material for shadow rendering
 * @param layer the layer index in the shadow map array
 */
public record ShadowPassContext(Camera shadowCamera, RenderTarget target, Material depthMaterial, int layer) {
}
