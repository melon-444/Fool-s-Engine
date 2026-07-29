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
