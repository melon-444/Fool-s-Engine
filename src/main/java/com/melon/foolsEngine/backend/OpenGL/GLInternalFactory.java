package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.InternalFactoryStub;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

public class GLInternalFactory extends InternalFactoryStub {

    static{
        InternalFactoryStub.InjectOpenGL(new GLInternalFactory());
    }

    @Override
    protected WindowsManager windowsManager() {
        return new GLFWWindowsManager();
    }

    @Override
    protected RenderFrame renderFrame() {
        return new GLRenderFrame();
    }

    @Override
    protected ShaderProgram shaderProgram() {
        return new GLShaderProgram();
    }

    @Override
    protected Texture texture() {
        return new GLTexture();
    }

    @Override
    protected Mesh mesh() {
        return new GLMesh();
    }
}
