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

import java.nio.file.Path;

/**
 * A GPU-resident texture resource.
 * <p>
 * There are two sources of {@code Texture} instances:
 * <ul>
 *   <li>{@link com.melon.foolsEngine.core.world.ServiceFactory#getTexture()} — a standalone
 *       {@code GL_TEXTURE_2D} that binds to a unique texture unit slot.</li>
 *   <li>{@link TextureManager#upload(Path)} — a slice of a {@code GL_TEXTURE_2D_ARRAY}
 *       owned by a manager. The renderer detects array textures via
 *       {@link #belongsTo()} and routes sampling through {@code sampler2DArray} +
 *       {@code textureLayer} uniform.</li>
 * </ul>
 * Lifecycle:
 * <pre>{@code
 *   Texture tex = factory.getTexture();
 *   tex.upload(Path.of("image.png"));
 *   // ... use in render loop ...
 *   tex.destroy();
 * }</pre>
 */
public interface Texture {

    /** Uploads pixel data from a file to the GPU. Safe to call only once per instance. */
    void upload(Path texture);

    /**
     * Releases GPU resources associated with this texture.
     * For array textures ({@link #belongsTo()} != null), this returns the layer
     * to the owning {@link TextureManager}'s free pool.
     */
    void destroy();

    /** Binds the texture to the specified texture unit slot ({@code GL_TEXTURE0 + slot}). */
    void bind(int slot);

    /** Unbinds the texture from its current slot. */
    void unbind();

    /**
     * Returns the raw pixel data uploaded to this texture.
     * For array textures, this is valid until {@link #destroy()} is called.
     * For standalone textures, returns {@code null} after upload (data is freed).
     */
    LoadedImage getImage();

    /**
     * Returns the {@link TextureManager} that owns this texture, or {@code null} if
     * this is a standalone texture created via {@code factory.getTexture()}.
     */
    default TextureManager belongsTo() { return null; }

    /**
     * Returns the layer index within the owning {@link TextureManager}'s texture array,
     * or {@code -1} if this is a standalone texture.
     */
    default int getLayer() { return -1; }
}
