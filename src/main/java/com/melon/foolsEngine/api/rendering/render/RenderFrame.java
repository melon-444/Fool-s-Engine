package com.melon.foolsEngine.api.rendering.render;

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;

public interface RenderFrame {
    void init();

    void beginFrame();

    void endFrame();

    void endFrame(RenderTarget target);

    void endFrame(RenderTarget target, Material overrideMaterial);

    void setCamera(Camera camera);

    void submit(RenderCommand command);

    void setBackGroundColor(float r, float g, float b, float a);

    void applyLightEnvironment(LightEnvironment env);
}
