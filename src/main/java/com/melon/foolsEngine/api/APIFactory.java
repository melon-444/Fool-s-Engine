package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

public interface APIFactory {
    WindowsManager getWindowsManager();
    RenderFrame getRenderFrame();
    ShaderProgram getShaderProgram();
    Texture getTexture();
    Mesh getMesh();
    RenderTarget createRenderTarget(int width, int height, int type);
    RenderTarget createRenderTarget(int width, int height, int type, int layers);
}
