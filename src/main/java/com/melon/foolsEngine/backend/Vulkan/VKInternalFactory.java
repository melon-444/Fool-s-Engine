package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.InternalFactoryStub;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

public class VKInternalFactory extends InternalFactoryStub {

    private VKInternalFactory() {}

    static {
        InternalFactoryStub.InjectVulkan(new VKInternalFactory());
    }

    @Override
    protected WindowsManager windowsManager() {
        return new VKWindowsManager();
    }

    @Override
    protected RenderFrame renderFrame() {
        return new VKRenderFrame();
    }

    @Override
    protected ShaderProgram shaderProgram() {
        return new VKShaderProgram();
    }

    @Override
    protected Texture texture() {
        return new VKTexture();
    }

    @Override
    protected Mesh mesh() {
        return new VKMesh();
    }

    @Override
    protected RenderTarget renderTarget(int width, int height, int type) {
        VKFrameBuffer fbo = new VKFrameBuffer();
        fbo.init(width, height, type);
        return fbo;
    }

    @Override
    protected RenderTarget renderTarget(int width, int height, int type, int layers) {
        VKFrameBuffer fbo = new VKFrameBuffer();
        fbo.init(width, height, type, layers);
        return fbo;
    }

    @Override
    protected <E> InputManager inputManager(E env) {
        if (env instanceof com.melon.foolsEngine.api.windows.Window window) {
            InputManager im = new InputManager();
            VKKeyBoard kb = new VKKeyBoard();
            VKMouse mouse = new VKMouse();
            kb.attachEnvironment(window);
            mouse.attachEnvironment(window);
            im.registerKeyboard(kb);
            im.registerMouse(mouse);
            return im;
        }
        throw new IllegalArgumentException("Vulkan implementation needs Window instance instead of " + env.getClass().getName());
    }

    @Override
    protected TextureManager textureManager(int width, int height, int maxLayers) {
        return new VKTextureManager(width, height, maxLayers);
    }
}
