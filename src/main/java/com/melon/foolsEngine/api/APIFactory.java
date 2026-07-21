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

package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

/**
 * Backend-agnostic service factory.
 * All engine services (windows, render frame, shaders, meshes, textures, render targets)
 * are obtained through this interface. The concrete implementation is selected at engine startup
 * (currently OpenGL only).
 * <p>
 * Obtain via {@code FoolsEngine.serviceFactory}.
 */
public interface APIFactory {
    /** @return the window manager for creating and managing windows */
    WindowsManager getWindowsManager();
    /** @return the main render frame */
    RenderFrame getRenderFrame();
    /** @return a new unloaded shader program */
    ShaderProgram getShaderProgram();
    /** @return a new unloaded texture */
    Texture getTexture();
    /** @return a new unloaded mesh */
    Mesh getMesh();
    /** Creates a single-layer render target */
    RenderTarget createRenderTarget(int width, int height, int type);
    /** Creates a multi-layer render target (e.g., for shadow map arrays) */
    RenderTarget createRenderTarget(int width, int height, int type, int layers);

    /**
     * Creates an InputManager with platform-appropriate devices (keyboard, mouse) already
     * attached to the given environment and registered.
     * @param env the platform environment (e.g., a Window) to receive input from
     * @param <E> the environment type
     * @return a ready-to-use InputManager (just add bindings)
     */
    <E> InputManager createInputManager(E env);

    /**
     * Creates a TextureManager that packs same-size textures into a single
     * {@code GL_TEXTURE_2D_ARRAY}, avoiding atlas bleeding.
     * @param width required texture width for all uploads
     * @param height required texture height for all uploads
     * @param maxLayers maximum number of texture layers
     */
    TextureManager createTextureManager(int width, int height, int maxLayers);
}
