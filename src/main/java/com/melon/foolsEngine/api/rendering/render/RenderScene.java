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
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates all render state for a single frame: camera, lighting, draw commands, and background color.
 * Passed to {@link com.melon.foolsEngine.api.rendering.render.RenderFrame#render(RenderScene)}.
 * <p>
 * Usage:
 * <pre>{@code
 *   RenderScene scene = new RenderScene();
 *   scene.setCamera(camera);
 *   scene.setLighting(lightEnv);
 *   scene.submit(new RenderCommand(mesh, material, transform));
 *   frame.render(scene);
 * }</pre>
 */
public class RenderScene {

    private Camera camera;
    private LightEnvironment lightEnv;
    private TextureManager textureManager;
    private final List<RenderCommand> commands = new ArrayList<>();
    private final List<ShaderPass> passes = new ArrayList<>();
    private float bgR = 0.05f;
    private float bgG = 0.05f;
    private float bgB = 0.1f;
    private float bgA = 1.0f;

    /** Sets the camera for this frame */
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /** @return the current camera, or null */
    public Camera getCamera() {
        return camera;
    }

    /** Sets the light environment for this frame */
    public void setLighting(LightEnvironment env) {
        this.lightEnv = env;
    }

    /** @return the current light environment, or null */
    public LightEnvironment getLighting() {
        return lightEnv;
    }

    /** Sets the texture manager for this frame (for array-texture enabled materials) */
    public void setTextureManager(TextureManager manager) {
        this.textureManager = manager;
    }

    /** @return the current texture manager, or null */
    public TextureManager getTextureManager() {
        return textureManager;
    }

    /** Submits a draw command for rendering this frame */
    public void submit(RenderCommand command) {
        commands.add(command);
    }

    /** @return an unmodifiable view of all submitted commands */
    public List<RenderCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    /** Sets the background clear color (RGBA, 0-1 range) */
    public void setBackGroundColor(float r, float g, float b, float a) {
        this.bgR = r;
        this.bgG = g;
        this.bgB = b;
        this.bgA = a;
    }

    public float getBgR() { return bgR; }
    public float getBgG() { return bgG; }
    public float getBgB() { return bgB; }
    public float getBgA() { return bgA; }

    /** Clears all submitted commands for the next frame */
    public void clear() {
        commands.clear();
        passes.clear();
    }

    /** Adds a {@link ShaderPass} to the rendering pipeline for this frame. */
    public void submitPass(ShaderPass pass) {
        passes.add(pass);
    }

    /** @return an unmodifiable view of all submitted passes */
    public List<ShaderPass> getPasses() {
        return Collections.unmodifiableList(passes);
    }

    /** Clears only the pass list. */
    public void clearPasses() {
        passes.clear();
    }
}
