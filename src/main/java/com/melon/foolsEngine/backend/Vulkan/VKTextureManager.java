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
