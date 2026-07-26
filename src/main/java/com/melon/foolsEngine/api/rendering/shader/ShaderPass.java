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

package com.melon.foolsEngine.api.rendering.shader;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes one ordered stage of a render pipeline.
 *
 * <p>A {@link Type#CORE CORE} pass consumes the scene's render commands and
 * therefore keeps instanced batching. A {@link Type#POSTEFFECT POSTEFFECT}
 * pass draws one fullscreen quad.</p>
 *
 * <p>Material parameters and pass parameters have different lifetimes:
 * material parameters describe a surface and remain part of the batch key;
 * pass uniforms describe this execution of the pipeline and are applied after
 * the selected material parameters.</p>
 */
public class ShaderPass {

    /** What the pass consumes. */
    public enum Type {
        /** Consume and instance-batch the scene's render commands. */
        CORE,
        /** Draw one fullscreen quad. */
        POSTEFFECT
    }

    /** How a CORE pass resolves the shader and material parameters. */
    public enum MaterialMode {
        /** Use each render command's material shader and parameters. */
        COMMAND_MATERIAL,
        /** Use the pass shader with each render command's material parameters. */
        PASS_SHADER,
        /** Use one replacement material for every render command. */
        OVERRIDE_MATERIAL
    }

    /** Whether an attachment keeps its previous contents or is cleared. */
    public enum LoadOp {
        LOAD,
        CLEAR
    }

    private Type type;
    private MaterialMode materialMode;
    private final ShaderProgram shader;

    private RenderTarget output;
    private final List<PassInput> inputs = new ArrayList<>();
    private final Map<String, Object> uniforms = new LinkedHashMap<>();

    private Camera cameraOverride;
    private Material overrideMaterial;
    private int arrayLayer = -1;

    private LoadOp colorLoadOp;
    private LoadOp depthLoadOp;
    private boolean customClearColor;
    private float clearR;
    private float clearG;
    private float clearB;
    private float clearA = 1.0f;
    private double clearDepth = 0.0;

    /**
     * Creates a CORE pass that uses {@code shader} with each command material's
     * parameters. This preserves the old constructor while making its shader
     * effective during CORE rendering.
     */
    public ShaderPass(ShaderProgram shader) {
        this(Type.CORE, MaterialMode.PASS_SHADER,
                Objects.requireNonNull(shader, "shader"));
    }

    private ShaderPass(Type type, MaterialMode materialMode, ShaderProgram shader) {
        this.type = type;
        this.materialMode = materialMode;
        this.shader = shader;
        this.colorLoadOp = type == Type.CORE ? LoadOp.CLEAR : LoadOp.LOAD;
        this.depthLoadOp = type == Type.CORE ? LoadOp.CLEAR : LoadOp.LOAD;
    }

    /** Set the output render target. {@code null} means framebuffer 0. */
    public ShaderPass output(RenderTarget target) {
        this.output = target;
        return this;
    }

    /**
     * Compatibility builder for the old API. Prefer
     * {@link #postEffect(ShaderProgram)} for new code.
     */
    public ShaderPass fullscreen() {
        this.type = Type.POSTEFFECT;
        return this;
    }

    /** Bind a previous render target to a sampler. */
    public ShaderPass input(RenderTarget texture, String samplerName) {
        inputs.add(new PassInput(texture, samplerName));
        return this;
    }

    /** Set a per-pass uniform, applied after material parameters. */
    public ShaderPass uniform(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("uniform name must not be blank");
        }
        uniforms.put(name, Objects.requireNonNull(value, "uniform value"));
        return this;
    }

    /** Use {@code cam} instead of the scene camera. */
    public ShaderPass camera(Camera cam) {
        this.cameraOverride = cam;
        return this;
    }

    /**
     * Replace every command material with {@code mat}. This also selects
     * {@link MaterialMode#OVERRIDE_MATERIAL}.
     */
    public ShaderPass overrideMaterial(Material mat) {
        this.overrideMaterial = Objects.requireNonNull(mat, "material");
        this.materialMode = MaterialMode.OVERRIDE_MATERIAL;
        return this;
    }

    /** Select CORE material resolution explicitly. */
    public ShaderPass materialMode(MaterialMode mode) {
        this.materialMode = Objects.requireNonNull(mode, "mode");
        return this;
    }

    /** Select a texture-array layer on the output target. */
    public ShaderPass arrayLayer(int layer) {
        if (layer < -1) throw new IllegalArgumentException("layer must be >= -1");
        this.arrayLayer = layer;
        return this;
    }

    public ShaderPass colorLoad(LoadOp op) {
        this.colorLoadOp = Objects.requireNonNull(op, "op");
        return this;
    }

    public ShaderPass depthLoad(LoadOp op) {
        this.depthLoadOp = Objects.requireNonNull(op, "op");
        return this;
    }

    /** Clear color using this value instead of the scene background color. */
    public ShaderPass clearColor(float r, float g, float b, float a) {
        this.colorLoadOp = LoadOp.CLEAR;
        this.customClearColor = true;
        this.clearR = r;
        this.clearG = g;
        this.clearB = b;
        this.clearA = a;
        return this;
    }

    /** Clear depth using this value. Reverse-Z rendering normally uses 0. */
    public ShaderPass clearDepth(double depth) {
        this.depthLoadOp = LoadOp.CLEAR;
        this.clearDepth = depth;
        return this;
    }

    // ── Getters ──

    public Type type() { return type; }
    public MaterialMode materialMode() { return materialMode; }
    public ShaderProgram shader() { return shader; }
    public RenderTarget output() { return output; }
    public boolean isFullscreen() { return type == Type.POSTEFFECT; }
    public List<PassInput> inputs() { return Collections.unmodifiableList(inputs); }
    public Map<String, Object> uniforms() { return Collections.unmodifiableMap(uniforms); }
    public Camera cameraOverride() { return cameraOverride; }
    public Material overrideMaterial() { return overrideMaterial; }
    public int arrayLayer() { return arrayLayer; }
    public LoadOp colorLoadOp() { return colorLoadOp; }
    public LoadOp depthLoadOp() { return depthLoadOp; }
    public boolean hasCustomClearColor() { return customClearColor; }
    public float clearR() { return clearR; }
    public float clearG() { return clearG; }
    public float clearB() { return clearB; }
    public float clearA() { return clearA; }
    public double clearDepth() { return clearDepth; }

    /**
     * Checks combinations that cannot be represented safely by the renderer.
     * Called by the backend immediately before executing the pass.
     */
    public void validate() {
        if (type == Type.POSTEFFECT) {
            if (shader == null) {
                throw new IllegalStateException("POSTEFFECT pass requires a shader");
            }
            if (overrideMaterial != null) {
                throw new IllegalStateException("POSTEFFECT pass cannot override a material");
            }
            return;
        }

        if (materialMode == MaterialMode.PASS_SHADER && shader == null) {
            throw new IllegalStateException("PASS_SHADER mode requires a pass shader");
        }
        if (materialMode == MaterialMode.OVERRIDE_MATERIAL && overrideMaterial == null) {
            throw new IllegalStateException("OVERRIDE_MATERIAL mode requires an override material");
        }
    }

    // ── Factories ──

    /** CORE pass using each command's complete material. */
    public static ShaderPass core() {
        return new ShaderPass(Type.CORE, MaterialMode.COMMAND_MATERIAL, null);
    }

    /** CORE pass using one pass shader and each command material's parameters. */
    public static ShaderPass core(ShaderProgram shader) {
        return new ShaderPass(shader);
    }

    /** Compatibility alias for {@link #core(ShaderProgram)}. */
    public static ShaderPass color(ShaderProgram shader) {
        return core(shader);
    }

    /** Fullscreen post-effect pass. It loads existing attachments by default. */
    public static ShaderPass postEffect(ShaderProgram shader) {
        return new ShaderPass(Type.POSTEFFECT, MaterialMode.PASS_SHADER,
                Objects.requireNonNull(shader, "shader"));
    }

    /** Compatibility alias for {@link #postEffect(ShaderProgram)}. */
    public static ShaderPass postProcess(ShaderProgram shader) {
        return postEffect(shader);
    }
}