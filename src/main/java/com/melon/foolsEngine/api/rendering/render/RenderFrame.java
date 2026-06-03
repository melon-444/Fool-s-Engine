package com.melon.foolsEngine.api.rendering.render;

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.ShadowManager;

public interface RenderFrame {
    void init();

    void beginFrame();

    void endFrame();

    void endFrame(RenderTarget target);

    void endFrame(RenderTarget target, Material overrideMaterial);

    void endFrame(RenderTarget target, Material overrideMaterial, int arrayLayer);

    void render(RenderScene scene);

    @Deprecated
    void setCamera(Camera camera);

    @Deprecated
    void submit(RenderCommand command);

    @Deprecated
    void setBackGroundColor(float r, float g, float b, float a);

    @Deprecated
    void applyLightEnvironment(LightEnvironment env);

    default void setShadowManager(ShadowManager shadowManager) {
    }
}
