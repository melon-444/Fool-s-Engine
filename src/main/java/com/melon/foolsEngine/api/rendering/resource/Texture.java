package com.melon.foolsEngine.api.rendering.resource;

import java.nio.file.Path;

/**
 * A GPU-resident 2D texture.
 * Obtain an instance via {@link com.melon.foolsEngine.core.world.ServiceFactory#getTexture()}
 * or {@link TextureManager#upload(Path)}.
 */
public interface Texture {
    /** Loads a texture from a file and uploads it to the GPU */
    void upload(Path texture);
    /** Releases GPU resources */
    void destroy();
    /** Binds the texture to a specific texture unit slot */
    void bind(int slot);
    /** Unbinds the texture */
    void unbind();

    /**
     * Returns the {@link TextureManager} that owns this texture, or null if this is a
     * standalone texture created via {@code factory.getTexture()}.
     */
    default TextureManager belongsTo() { return null; }

    /**
     * Returns the layer index within the owning TextureManager's texture array,
     * or -1 if this is a standalone texture.
     */
    default int getLayer() { return -1; }
}
