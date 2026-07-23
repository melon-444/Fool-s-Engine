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
 * Thread-safe graphics context abstraction for the render thread.
 * Backend-agnostic — GLFW implements for OpenGL, Vulkan swapchain for future backend.
 * <p>
 * Typical render-thread usage:
 * <pre>{@code
 *   ctx.makeCurrent();
 *   try {
 *       while (running) {
 *           frame.render(scene);
 *           ctx.swapBuffers();
 *           ctx.pollEvents();
 *       }
 *   } finally {
 *       ctx.releaseCurrent();
 *   }
 * }</pre>
 */
public interface GraphicsContext {

    /** Binds the rendering context to the calling thread. Must be called before any GL/Vulkan commands. */
    void makeCurrent();

    /** Unbinds the rendering context from the calling thread. */
    void releaseCurrent();

    /** Swaps front and back buffers, presenting the rendered frame to the display. */
    void swapBuffers();

    /** Polls pending window events (input, resize, close-request). Required for callbacks to fire. */
    void pollEvents();

    /** @return true if the window has been requested to close */
    boolean shouldClose();

    /** @return the platform-native handle (GLFW window ID / Vulkan surface handle) */
    long nativeHandle();
}
