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

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;

import java.util.*;

/**
 * A single rendering pass: shader + output + draw mode + inputs + uniforms.
 * <p>
 * All passes in the engine — color, shadow, post-processing — are instances of this class.
 * A pass knows which shader to use, where to render (null = screen), whether to draw
 * a fullscreen quad (post-process) or scene geometry (forward), and carries per-pass
 * uniform overrides and input texture bindings.
 * <p>
 * System uniforms (VP matrix, lights, shadow array, camera position) are bound
 * automatically by the renderer — they do not belong in ShaderPass.
 * <p>
 * Builder usage:
 * <pre>{@code
 *   // Forward color pass
 *   ShaderPass colorPass = new ShaderPass(mainShader);
 *
 *   // Post-process bloom
 *   ShaderPass bloom = new ShaderPass(bloomShader)
 *       .output(bloomRT)
 *       .fullscreen()
 *       .input(colorRT, "sceneColor")
 *       .input(bloomRT, "bloom")
 *       .uniform("intensity", 1.5f);
 *
 *   // Convenience factories
 *   ShaderPass.color(mainShader);
 *   ShaderPass.postProcess(bloomShader).output(bloomRT);
 * }</pre>
 */
public class ShaderPass {

    private final ShaderProgram shader;
    private RenderTarget output;
    private boolean fullscreenQuad;
    private final List<PassInput> inputs = new ArrayList<>();
    private final Map<String, Object> uniforms = new LinkedHashMap<>();

    public ShaderPass(ShaderProgram shader) {
        if (shader == null) throw new NullPointerException("shader must not be null");
        this.shader = shader;
    }

    /** Set the output render target. null = default framebuffer (screen). */
    public ShaderPass output(RenderTarget target) {
        this.output = target;
        return this;
    }

    /** Mark this pass as drawing a fullscreen quad (post-process). Default draws scene geometry. */
    public ShaderPass fullscreen() {
        this.fullscreenQuad = true;
        return this;
    }

    /**
     * Bind an input render target to a sampler for reading in the shader.
     * Used by post-process passes that read the output of previous passes.
     */
    public ShaderPass input(RenderTarget texture, String samplerName) {
        inputs.add(new PassInput(texture, samplerName));
        return this;
    }

    /** Set a per-pass uniform override (applied after material uniforms). */
    public ShaderPass uniform(String name, Object value) {
        uniforms.put(name, value);
        return this;
    }

    // ── Getters ──

    public ShaderProgram shader() { return shader; }
    public RenderTarget output() { return output; }
    public boolean isFullscreen() { return fullscreenQuad; }
    public List<PassInput> inputs() { return Collections.unmodifiableList(inputs); }
    public Map<String, Object> uniforms() { return Collections.unmodifiableMap(uniforms); }

    // ── Convenience factories ──

    /** Forward color pass: draws scene geometry to the screen. */
    public static ShaderPass color(ShaderProgram shader) {
        return new ShaderPass(shader);
    }

    /** Post-process pass: draws a fullscreen quad (usually to an off-screen target). */
    public static ShaderPass postProcess(ShaderProgram shader) {
        return new ShaderPass(shader).fullscreen();
    }
}
