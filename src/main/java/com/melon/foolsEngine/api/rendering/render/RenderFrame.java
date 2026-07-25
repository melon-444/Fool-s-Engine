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

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowManager;

import java.util.Collections;
import java.util.List;

/**
 * Main rendering orchestrator. Manages a frame lifecycle and executes draw calls.
 * <p>
 * <b>Recommended usage (current API):</b>
 * <pre>{@code
 *   frame.init();
 *   frame.setShadowManager(shadowManager);
 *
 *   // Each frame:
 *   scene.setCamera(camera);
 *   scene.setLighting(lightEnv);
 *   scene.submit(new RenderCommand(mesh, material, transform));
 *   frame.render(scene);
 *   scene.clear();
 * }</pre>
 * <p>
 */
public interface RenderFrame {
    /** Initializes the renderer (must be called once before any rendering) */
    void init();

    /**
     * Renders a complete scene: shadow pass (if ShadowManager is set) followed by the color pass.
     * This is the preferred rendering entry point.
     */
    void render(RenderScene scene);

    /**
     * Returns the number of draw calls issued in the last {@link #render(RenderScene)} call.
     * Returns -1 if the backend does not track this metric.
     */
    default int getDrawCallCount() {
        return -1;
    }

    /**
     * Writes the current default framebuffer pixels (RGBA) into a pre-allocated {@code dstBuf}.
     * The buffer must have at least {@code width * height * 4} bytes of remaining capacity.
     */
    void screenShot(java.nio.ByteBuffer dstBuf);

    /**
     * Captures the current default framebuffer and saves it as a PNG file at the given path.
     */
    void screenShot(java.nio.file.Path path);

    /**
     * Captures the given render target and saves it as a PNG file at the given path.
     */
    void screenShot(java.nio.file.Path path, RenderTarget target);
}
