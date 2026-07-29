// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package com.melon.foolsEngine.api.rendering.resource.texture;

import com.melon.foolsEngine.util.LoadMode;
import com.melon.foolsEngine.util.WrapMode;

import java.nio.file.Path;
import java.util.List;

/**
 * Manages a {@code GL_TEXTURE_2D_ARRAY} GPU resource, packing multiple same-size
 * textures into a single texture unit to eliminate per-texture slot binding.
 * <p>
 * Each uploaded texture is returned as a {@link Texture} whose {@link Texture#belongsTo()}
 * points back to this manager and whose {@link Texture#getLayer()} exposes the array layer
 * index. The renderer detects array-texture ownership and routes sampling through
 * {@code sampler2DArray} + {@code textureLayer} uniform instead of per-texture slots.
 * <p>
 * Layers are pre-allocated at construction time from a free-list pool. Releasing a layer
 * clears the corresponding slice of the GPU array (via {@code glTexSubImage3D} with zeros)
 * and returns the slot to the pool for reuse.
 * <p>
 * Usage:
 * <pre>{@code
 *   TextureManager tm = factory.createTextureManager(512, 512, 64);
 *   Texture stone = tm.upload(Path.of("stone.png"));
 *   Texture wood  = tm.upload(Path.of("wood.png"));
 *   material.set("textureSampler", stone);
 *
 *   tm.free(stone);     // release single texture, layer returns to pool
 *   tm.getTexture(2);   // lookup by layer index
 *   tm.destroy();       // delete entire array and all GPU resources
 * }</pre>
 */
public interface TextureManager {

    /** Fixed texture unit for the array sampler (shadow array occupies slot 8). */
    int TEXTURE_ARRAY_SLOT = 9;

    /**
     * Uploads an image to the next available layer in the texture array.
     * Equivalent to {@code upload(path, LoadMode.STRETCH, WrapMode.CLAMP_TO_BORDER)}.
     *
     * @param path image file path
     * @return a {@link Texture} whose {@link Texture#belongsTo()} points back to this manager
     *         and whose {@link Texture#getLayer()} contains the allocated array slice index
     */
    Texture upload(Path path);

    /**
     * Uploads an image with the given sizing strategy.
     * Equivalent to {@code upload(path, mode, WrapMode.CLAMP_TO_BORDER)}.
     *
     * @param path image file path
     * @param mode how to handle dimension mismatches between source and array tile size
     */
    Texture upload(Path path, LoadMode mode);

    /**
     * Uploads an image with full control over load mode and wrap behavior.
     *
     * @param path image file path
     * @param mode how to handle dimension mismatches ({@link LoadMode#STRETCH},
     *             {@link LoadMode#CROP_WRAP}, or {@link LoadMode#STRICT})
     * @param wrap wrapping strategy used when {@link LoadMode#CROP_WRAP} needs to fill
     *             areas beyond the source image bounds
     */
    Texture upload(Path path, LoadMode mode, WrapMode wrap);

    /**
     * Uploads an image to a specific layer, overwriting any existing content.
     * If the layer was previously occupied, the old texture's {@link LoadedImage} is freed.
     * Uses {@link LoadMode#STRETCH} and {@link WrapMode#CLAMP_TO_BORDER}.
     *
     * @param path image file path
     * @param layer target array slice (must be within {@code [1, maxLayers-1]})
     */
    Texture upload(Path path, int layer);

    /**
     * Uploads an image to a specific layer with full sizing control.
     * If the layer was previously occupied, the old texture's {@link LoadedImage} is freed.
     *
     * @param path image file path
     * @param layer target array slice
     * @param mode how to handle dimension mismatches
     * @param wrap wrapping strategy for {@link LoadMode#CROP_WRAP}
     */
    Texture upload(Path path, int layer, LoadMode mode, WrapMode wrap);

    /**
     * Returns a static 1x1 white placeholder texture on layer 0.
     * Safe to use without calling {@link #upload(Path)} first; the renderer can fall
     * back to this when no texture has been assigned to a material.
     */
    Texture getPlaceholder();

    /**
     * Releases a layer and clears it from the GPU array, making it available for future
     * {@link #upload(Path)} calls. If the layer held a {@link Texture} with an associated
     * {@link LoadedImage}, the image is freed.
     *
     * @see #free(Texture)
     */
    void releaseLayer(int layer);

    /**
     * Generates mipmaps for the entire texture array if any layer has been modified
     * since the last flush. Called automatically by the renderer each frame.
     */
    void flushMipmaps();

    /**
     * Binds the underlying {@code GL_TEXTURE_2D_ARRAY} to the specified texture unit.
     * Subsequent shader bindings that reference array textures will sample from this unit.
     */
    void bind(int slot);

    /** @return the fixed width, in pixels, of every tile in this array */
    int getWidth();

    /** @return the fixed height, in pixels, of every tile in this array */
    int getHeight();

    /** @return the total number of array slices (including placeholder layer 0) */
    int getMaxLayers();

    /** @return the number of currently occupied layers (excluding the placeholder) */
    int getActiveLayerCount();

    /**
     * Looks up the {@link Texture} currently occupying the given layer.
     *
     * @param layer the array slice index
     * @return the texture, or {@code null} if the layer is free, unallocated, or layer 0 (placeholder)
     */
    Texture getTexture(int layer);

    /**
     * Returns a snapshot of all active textures managed by this instance.
     * Does not include the placeholder.
     */
    List<Texture> getTextures();

    /**
     * Releases the texture's layer and returns it to the free pool. The layer is cleared
     * on the GPU so that stale references cannot render old data. The texture's
     * {@link LoadedImage} is freed if present.
     * <p>
     * Equivalent to calling {@link Texture#destroy()} on the texture.
     */
    void free(Texture texture);

    /**
     * Destroys the entire texture array and all associated GPU resources.
     * Frees all {@link LoadedImage} pixel buffers and invalidates every tracked texture.
     * After this call, the manager must not be used.
     */
    void destroy();
}
