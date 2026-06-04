package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.resource.TextureManager;

import java.nio.file.Path;

class GLArrayTexture implements Texture {

    private final GLTextureManager manager;
    private final int layer;

    GLArrayTexture(GLTextureManager manager, int layer) {
        this.manager = manager;
        this.layer = layer;
    }

    @Override
    public TextureManager belongsTo() {
        return manager;
    }

    @Override
    public int getLayer() {
        return layer;
    }

    @Override
    public void bind(int slot) {
        manager.bind(slot);
    }

    @Override
    public void unbind() {
    }

    @Override
    public void upload(Path texture) {
        throw new UnsupportedOperationException("Use TextureManager.upload() for array textures");
    }

    @Override
    public void destroy() {
        manager.releaseLayer(layer);
    }
}
