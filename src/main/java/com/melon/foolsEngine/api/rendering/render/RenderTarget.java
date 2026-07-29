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
package com.melon.foolsEngine.api.rendering.render;

/**
 * An off-screen render target (FBO).
 * Supports color and depth-only types, with optional layered rendering for texture arrays.
 * <p>
 * Create via {@link com.melon.foolsEngine.core.world.ServiceFactory#createRenderTarget(int, int, int)}:
 * <pre>{@code
 *   RenderTarget rt = factory.createRenderTarget(1024, 1024, RenderTarget.TARGET_DEPTH, 16);
 * }</pre>
 */
public interface RenderTarget {

    /** Color render target type */
    int TARGET_COLOR = 0;
    /** Depth-only render target type (used for shadow maps) */
    int TARGET_DEPTH = 1;

    /** Initializes the render target with the given dimensions and type */
    void init(int width, int height, int type);

    /** Initializes a layered render target (e.g., 2D texture array) */
    default void init(int width, int height, int type, int layers) {
        init(width, height, type);
    }

    /** Binds this render target for subsequent draw calls */
    void bind();

    /** Unbinds the render target, restoring the default framebuffer */
    void unbind();

    /** Attaches a specific layer for rendering (for array targets) */
    default void attachLayer(int layer) {
    }

    /** @return the number of layers (1 for non-array targets) */
    default int getLayers() {
        return 1;
    }

    /** @return the width in pixels */
    int getWidth();

    /** @return the height in pixels */
    int getHeight();

    /** @return the OpenGL texture ID */
    int getTextureId();

    /** @return {@link #TARGET_COLOR} or {@link #TARGET_DEPTH} */
    int getType();

    /** Releases GPU resources */
    void destroy();
}
