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
