package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.backend.OpenGL.GLInternalFactory;

public interface APIFactory {
    public WindowsManager getWindowsManager();
    public RenderFrame getRenderFrame();
    public ShaderProgram getShaderProgram();
    public Texture getTexture();
    public Mesh getMesh();
}
