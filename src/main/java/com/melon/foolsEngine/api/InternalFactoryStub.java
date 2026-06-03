package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

public abstract class InternalFactoryStub implements APIFactory {

    static{
            try {
                Class.forName("com.melon.foolsEngine.backend.OpenGL.GLInternalFactory");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
    }

    protected abstract WindowsManager windowsManager();
    protected abstract RenderFrame renderFrame();
    protected abstract ShaderProgram shaderProgram();
    protected abstract Texture texture();
    protected abstract Mesh mesh();
    protected abstract RenderTarget renderTarget(int width, int height, int type);

    protected RenderTarget renderTarget(int width, int height, int type, int layers) {
        return renderTarget(width, height, type);
    }

    private static InternalFactoryStub OpenGLINSTANCE;
    private static InternalFactoryStub VulkanINSTANCE;

    public static APIFactory OpenGLINSTANCE() {
        if (OpenGLINSTANCE == null)
            throw new IllegalStateException("Factory not initialized");
        return OpenGLINSTANCE;
    }

    public static APIFactory VulkanINSTANCE() {
        if (VulkanINSTANCE == null)
            throw new IllegalStateException("Factory not initialized");
        return OpenGLINSTANCE;
    }

    protected static void InjectVulkan(InternalFactoryStub instance) {
        if (VulkanINSTANCE != null)
            throw new IllegalStateException("Factory already initialized");
        VulkanINSTANCE = instance;
    }

    protected static void InjectOpenGL(InternalFactoryStub instance) {
        if (OpenGLINSTANCE != null)
            throw new IllegalStateException("Factory already initialized");
        OpenGLINSTANCE = instance;
    }

    @Override
    public WindowsManager getWindowsManager(){
        return windowsManager();
    }
    @Override
    public RenderFrame getRenderFrame(){
        return renderFrame();
    }
    @Override
    public ShaderProgram getShaderProgram(){
        return shaderProgram();
    }
    @Override
    public Texture getTexture(){
        return texture();
    }
    @Override
    public Mesh getMesh(){
        return mesh();
    }
    @Override
    public RenderTarget createRenderTarget(int width, int height, int type) {
        return renderTarget(width, height, type);
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, int type, int layers) {
        return renderTarget(width, height, type, layers);
    }
}
