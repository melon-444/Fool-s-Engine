package com.melon.foolsEngine.api.rendering.resource;

import java.nio.file.Path;

/**
 * Manages a {@code GL_TEXTURE_2D_ARRAY} for packing multiple same-size textures into
 * a single GPU texture unit, avoiding atlas bleeding and simplifying multi-texture rendering.
 * <p>
 * Each uploaded texture is returned as a {@link Texture} whose {@link Texture#belongsTo()}
 * points back to this manager and whose {@link Texture#getLayer()} exposes the array layer index.
 * The renderer detects these methods and routes sampling through
 * {@code sampler2DArray} + {@code textureLayer} uniform instead of per-texture slots.
 * <p>
 * Usage:
 * <pre>{@code
 *   TextureManager tm = factory.createTextureManager(512, 512, 64);
 *   Texture stone = tm.upload(Path.of("stone.png"));
 *   Texture wood  = tm.upload(Path.of("wood.png"));
 *   material.set("textureSampler", stone);
 * }</pre>
 */
public interface TextureManager {

    /** Fixed texture unit for the array sampler (shadow array occupies slot 8) */
    int TEXTURE_ARRAY_SLOT = 9;

    /**
     * Uploads an image to the texture array, allocating the next free layer.
     * The image must match the array dimensions declared at construction time.
     *
     * @return a {@link Texture} whose {@code belongsTo()} is this manager
     */
    Texture upload(Path path);

    /**
     * Uploads an image to a specific layer, overwriting any existing content.
     *
     * @return a {@link Texture} referencing the given layer
     */
    Texture upload(Path path, int layer);

    /**
     * Returns a static 1x1 white placeholder texture. Safe to use without calling
     * {@link #upload(Path)} first; the renderer can fall back to this when no
     * texture has been assigned to a material.
     */
    Texture getPlaceholder();

    /** Releases a layer for reuse by future {@link #upload(Path)} calls */
    void releaseLayer(int layer);

    /** Generates mipmaps for the texture array if any layer has been modified */
    void flushMipmaps();

    /** Binds the underlying {@code GL_TEXTURE_2D_ARRAY} to the given slot */
    void bind(int slot);

    int getWidth();
    int getHeight();
    int getMaxLayers();
    int getActiveLayerCount();

    /** Destroys the texture array and all associated resources */
    void destroy();
}
