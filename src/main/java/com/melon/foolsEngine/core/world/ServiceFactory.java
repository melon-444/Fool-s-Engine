package com.melon.foolsEngine.core.world;


import com.melon.foolsEngine.api.APIFactory;
import com.melon.foolsEngine.api.InternalFactoryStub;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;


/**
 * Responsible for every service instance which needs to use underlying code.
 */
public class ServiceFactory implements APIFactory {


    public static final int OPENGL_BACKEND = 0;
    public static final int VULKAN_BACKEND = 1;

    private static int BackEndType = OPENGL_BACKEND;

    public static void setBackEndType(int type) {
        BackEndType = type;
    }

    @Override
    public WindowsManager getWindowsManager() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getWindowsManager();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getWindowsManager();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public RenderFrame getRenderFrame() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getRenderFrame();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getRenderFrame();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public ShaderProgram getShaderProgram() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getShaderProgram();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getShaderProgram();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public Texture getTexture() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getTexture();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getTexture();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public Mesh getMesh() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getMesh();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getMesh();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, int type) {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().createRenderTarget(width, height, type);
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().createRenderTarget(width, height, type);
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, int type, int layers) {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().createRenderTarget(width, height, type, layers);
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().createRenderTarget(width, height, type, layers);
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }
}

