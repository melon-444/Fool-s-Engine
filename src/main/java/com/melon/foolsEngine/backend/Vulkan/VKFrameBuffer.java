package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;

class VKFrameBuffer implements RenderTarget {

    private int width;
    private int height;
    private int type;
    private int layers = 1;

    @Override
    public void init(int width, int height, int type) {
        this.width = width;
        this.height = height;
        this.type = type;
    }

    @Override
    public void init(int width, int height, int type, int layers) {
        this.width = width;
        this.height = height;
        this.type = type;
        this.layers = layers;
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException("Vulkan backend: bind via VkRenderPass / VkFramebuffer");
    }

    @Override
    public void unbind() {
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getTextureId() {
        return 0;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getLayers() {
        return layers;
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Vulkan backend framebuffer destroy not yet implemented");
    }
}
