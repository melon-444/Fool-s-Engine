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
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.ShadowManager;

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
 * The deprecated methods ({@link #beginFrame()}, {@link #endFrame()}, {@link #submit(RenderCommand)},
 * {@link #setCamera(Camera)}, etc.) are retained for backward compatibility only.
 */
public interface RenderFrame {
    /** Initializes the renderer (must be called once before any rendering) */
    void init();

    /** @deprecated Use {@link #render(RenderScene)} instead */
    @Deprecated void beginFrame();

    /** @deprecated Use {@link #render(RenderScene)} instead */
    @Deprecated void endFrame();

    /** @deprecated Use {@link #render(RenderScene)} with a {@link RenderTarget} overload */
    @Deprecated void endFrame(RenderTarget target);

    /** @deprecated Use {@link #render(RenderScene)} with a {@link RenderTarget} overload */
    @Deprecated void endFrame(RenderTarget target, Material overrideMaterial);

    /** @deprecated Use {@link #render(RenderScene)} with a {@link RenderTarget} overload */
    @Deprecated void endFrame(RenderTarget target, Material overrideMaterial, int arrayLayer);

    /**
     * Renders a complete scene: shadow pass (if ShadowManager is set) followed by the color pass.
     * This is the preferred rendering entry point.
     */
    void render(RenderScene scene);

    /** @deprecated Use {@link RenderScene#setCamera(Camera)} instead */
    @Deprecated void setCamera(Camera camera);

    /** @deprecated Use {@link RenderScene#submit(RenderCommand)} instead */
    @Deprecated void submit(RenderCommand command);

    /** @deprecated Use {@link RenderScene#setBackGroundColor(float, float, float, float)} instead */
    @Deprecated void setBackGroundColor(float r, float g, float b, float a);

    /** @deprecated Use {@link RenderScene#setLighting(LightEnvironment)} instead */
    @Deprecated void applyLightEnvironment(LightEnvironment env);

    /**
     * @deprecated Use {@link LightEnvironment#enableShadows(com.melon.foolsEngine.api.rendering.render.RenderTarget, com.melon.foolsEngine.api.rendering.resource.Material, int)}
     * instead. The renderer now reads the ShadowManager from the scene's
     * LightEnvironment via {@link LightEnvironment#getShadowManager()}.
     */
    @Deprecated
    default void setShadowManager(ShadowManager shadowManager) {
    }

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
