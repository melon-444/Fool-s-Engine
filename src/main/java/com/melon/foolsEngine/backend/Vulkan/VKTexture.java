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
