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

import com.melon.foolsEngine.api.rendering.render.RenderTarget;

class VKFrameBuffer implements RenderTarget {

    private int width;
    private int height;
    private int type;
    private int layers = 1;

    @Override
    public void init(int width, int height, int type) {
        this.width = width;
        this.height = height;
        this.type = type;
    }

    @Override
    public void init(int width, int height, int type, int layers) {
        this.width = width;
        this.height = height;
        this.type = type;
        this.layers = layers;
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException("Vulkan backend: bind via VkRenderPass / VkFramebuffer");
    }

    @Override
    public void unbind() {
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
    public int getTextureId() {
        return 0;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getLayers() {
        return layers;
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Vulkan backend framebuffer destroy not yet implemented");
    }
}
