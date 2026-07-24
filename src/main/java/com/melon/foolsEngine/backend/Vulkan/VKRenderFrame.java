package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;

import java.nio.ByteBuffer;
import java.nio.file.Path;

class VKRenderFrame implements RenderFrame {

    private boolean init = false;

    @Override
    public void init() {
        if (init) return;
        init = true;
    }

    @Override
    @Deprecated
    public void beginFrame() {
    }

    @Override
    @Deprecated
    public void endFrame() {
    }

    @Override
    @Deprecated
    public void endFrame(RenderTarget target) {
    }

    @Override
    @Deprecated
    public void endFrame(RenderTarget target, Material overrideMaterial) {
    }

    @Override
    @Deprecated
    public void endFrame(RenderTarget target, Material overrideMaterial, int arrayLayer) {
    }

    @Override
    public void render(RenderScene scene) {
        throw new UnsupportedOperationException("Vulkan backend render() not yet implemented");
    }

    @Override
    @Deprecated
    public void setCamera(Camera camera) {
    }

    @Override
    @Deprecated
    public void submit(RenderCommand command) {
    }

    @Override
    @Deprecated
    public void setBackGroundColor(float r, float g, float b, float a) {
    }

    @Override
    @Deprecated
    public void applyLightEnvironment(LightEnvironment env) {
    }

    @Override
    public void screenShot(ByteBuffer dstBuf) {
        throw new UnsupportedOperationException("Vulkan backend screenShot not yet implemented");
    }

    @Override
    public void screenShot(Path path) {
        throw new UnsupportedOperationException("Vulkan backend screenShot not yet implemented");
    }

    @Override
    public void screenShot(Path path, RenderTarget target) {
        throw new UnsupportedOperationException("Vulkan backend screenShot not yet implemented");
    }
}
