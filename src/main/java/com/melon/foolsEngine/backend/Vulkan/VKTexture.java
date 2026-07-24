package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.resource.texture.LoadedImage;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;

import java.nio.file.Path;

class VKTexture implements Texture {

    private boolean uploaded = false;

    @Override
    public void upload(Path texture) {
        throw new UnsupportedOperationException("Vulkan backend texture upload not yet implemented");
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Vulkan backend texture destroy not yet implemented");
    }

    @Override
    public void bind(int slot) {
        throw new UnsupportedOperationException("Vulkan backend: bind via descriptor sets");
    }

    @Override
    public void unbind() {
    }

    @Override
    public LoadedImage getImage() {
        return null;
    }
}
