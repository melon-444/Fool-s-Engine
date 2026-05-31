package com.melon.foolsEngine.api.rendering.render;

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;

public interface RenderFrame {
    void init();

    void beginFrame();

    void endFrame();

    void setCamera(Camera camera);

    void submit(RenderCommand command);

    void setBackGroundColor(float r, float g, float b, float a);

    void setLights(Light[] lights);
}