package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.texture.LoadedImage;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;

import java.nio.file.Path;

class GLArrayTexture implements Texture {

    private final GLTextureManager manager;
    private final int layer;
    private final LoadedImage image;

    GLArrayTexture(GLTextureManager manager, int layer) {
        this(manager, layer, null);
    }

    GLArrayTexture(GLTextureManager manager, int layer, LoadedImage image) {
        this.manager = manager;
        this.layer = layer;
        this.image = image;
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
    public LoadedImage getImage() {
        return image;
    }

    @Override
    public void upload(Path texture) {
        throw new UnsupportedOperationException("Use TextureManager.upload() for array textures");
    }

    @Override
    public void destroy() {
        if (image != null) image.free();
        manager.freeLayer(layer);
    }
}
