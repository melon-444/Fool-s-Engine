package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.util.LoadMode;
import com.melon.foolsEngine.util.WrapMode;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

class VKTextureManager implements TextureManager {

    private final int width;
    private final int height;
    private final int maxLayers;

    VKTextureManager(int width, int height, int maxLayers) {
        this.width = width;
        this.height = height;
        this.maxLayers = maxLayers;
    }

    @Override
    public Texture upload(Path path) {
        throw new UnsupportedOperationException("Vulkan backend texture array upload not yet implemented");
    }

    @Override
    public Texture upload(Path path, LoadMode mode) {
        throw new UnsupportedOperationException("Vulkan backend texture array upload not yet implemented");
    }

    @Override
    public Texture upload(Path path, LoadMode mode, WrapMode wrap) {
        throw new UnsupportedOperationException("Vulkan backend texture array upload not yet implemented");
    }

    @Override
    public Texture upload(Path path, int layer) {
        throw new UnsupportedOperationException("Vulkan backend texture array upload not yet implemented");
    }

    @Override
    public Texture upload(Path path, int layer, LoadMode mode, WrapMode wrap) {
        throw new UnsupportedOperationException("Vulkan backend texture array upload not yet implemented");
    }

    @Override
    public Texture getPlaceholder() {
        throw new UnsupportedOperationException("Vulkan backend placeholder not yet implemented");
    }

    @Override
    public void releaseLayer(int layer) {
    }

    @Override
    public void flushMipmaps() {
    }

    @Override
    public void bind(int slot) {
        throw new UnsupportedOperationException("Vulkan backend: bind via descriptor sets");
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
    public int getMaxLayers() {
        return maxLayers;
    }

    @Override
    public int getActiveLayerCount() {
        return 0;
    }

    @Override
    public Texture getTexture(int layer) {
        return null;
    }

    @Override
    public List<Texture> getTextures() {
        return Collections.emptyList();
    }

    @Override
    public void free(Texture texture) {
    }

    @Override
    public void destroy() {
    }
}
