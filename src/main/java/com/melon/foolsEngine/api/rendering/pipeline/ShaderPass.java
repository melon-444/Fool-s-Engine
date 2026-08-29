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
package com.melon.foolsEngine.api.rendering.pipeline;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable description of one ordered rendering pass.
 *
 * <p>A {@link Type#CORE CORE} pass consumes the scene's render commands and
 * keeps instanced batching. A {@link Type#POSTEFFECT POSTEFFECT} pass draws
 * one fullscreen quad. Construct passes through the static builder factories
 * and finish them with {@link Builder#build()}.</p>
 */
public final class ShaderPass {

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

    /**
     * Alpha-blending behavior of a CORE pass.
     *
     * <p>An {@link #OPAQUE OPAQUE} pass draws only non-transparent render
     * commands with depth writes enabled. Any other mode enables GL blending,
     * disables depth writes (depth stays read-only) and draws only commands
     * whose {@link Material#isTransparent()} is true, sorted back-to-front by
     * camera distance.</p>
     */
    public enum BlendMode {
        /** No blending, depth writes enabled. */
        OPAQUE,
        /** Standard alpha blending: {@code src*alpha + dst*(1-alpha)}. */
        ALPHA_BLEND,
        /** Additive blending: {@code src + dst} (glows, fire, particles). */
        ADDITIVE
    }

    /** Required state of an attachment at the beginning of a pass. */
    public enum LoadOp {
        /** Preserve and use the previous contents. */
        LOAD,
        /** Clear the attachment to the configured clear value. */
        CLEAR,
        /** Previous contents are undefined and may be discarded. */
        DONT_CARE
    }

    /** Required state of an attachment after a pass completes. */
    public enum StoreOp {
        /** Preserve the produced contents for later use. */
        STORE,
        /** Produced contents are not needed and may be discarded. */
        DONT_CARE
    }

    private final Type type;
    private final MaterialMode materialMode;
    private final BlendMode blendMode;
    private final ShaderProgram shader;
    private final RenderTarget output;
    private final List<PassInput> inputs;
    private final Map<String, Object> uniforms;
    private final Camera cameraOverride;
    private final Material overrideMaterial;
    private final int arrayLayer;

    private final LoadOp colorLoadOp;
    private final StoreOp colorStoreOp;
    private final LoadOp depthLoadOp;
    private final StoreOp depthStoreOp;
    private final boolean customClearColor;
    private final float clearR;
    private final float clearG;
    private final float clearB;
    private final float clearA;
    private final double clearDepth;

    private ShaderPass(Builder builder) {
        this.type = builder.type;
        this.materialMode = builder.materialMode;
        this.blendMode = builder.blendMode;
        this.shader = builder.shader;
        this.output = builder.output;
        this.inputs = List.copyOf(builder.inputs);
        this.uniforms = Collections.unmodifiableMap(
                new LinkedHashMap<>(builder.uniforms));
        this.cameraOverride = builder.cameraOverride;
        this.overrideMaterial = builder.overrideMaterial;
        this.arrayLayer = builder.arrayLayer;
        this.colorLoadOp = builder.colorLoadOp;
        this.colorStoreOp = builder.colorStoreOp;
        this.depthLoadOp = builder.depthLoadOp;
        this.depthStoreOp = builder.depthStoreOp;
        this.customClearColor = builder.customClearColor;
        this.clearR = builder.clearR;
        this.clearG = builder.clearG;
        this.clearB = builder.clearB;
        this.clearA = builder.clearA;
        this.clearDepth = builder.clearDepth;
    }

    // ── Factories ──

    /** CORE pass using each command's complete material. */
    public static Builder core() {
        return new Builder(Type.CORE, MaterialMode.COMMAND_MATERIAL, null);
    }

    /** CORE pass using one pass shader and each command material's parameters. */
    public static Builder core(ShaderProgram shader) {
        return new Builder(Type.CORE, MaterialMode.PASS_SHADER,
                Objects.requireNonNull(shader, "shader"));
    }

    /** Compatibility alias for {@link #core(ShaderProgram)}. */
    public static Builder color(ShaderProgram shader) {
        return core(shader);
    }

    /** Fullscreen post-effect pass. */
    public static Builder postEffect(ShaderProgram shader) {
        return new Builder(Type.POSTEFFECT, MaterialMode.PASS_SHADER,
                Objects.requireNonNull(shader, "shader"));
    }

    /** Compatibility alias for {@link #postEffect(ShaderProgram)}. */
    public static Builder postProcess(ShaderProgram shader) {
        return postEffect(shader);
    }

    // ── Getters ──

    public Type type() { return type; }
    public MaterialMode materialMode() { return materialMode; }
    public BlendMode blendMode() { return blendMode; }
    public ShaderProgram shader() { return shader; }
    public RenderTarget output() { return output; }
    public boolean isFullscreen() { return type == Type.POSTEFFECT; }
    public List<PassInput> inputs() { return inputs; }
    public Map<String, Object> uniforms() { return uniforms; }
    public Camera cameraOverride() { return cameraOverride; }
    public Material overrideMaterial() { return overrideMaterial; }
    public int arrayLayer() { return arrayLayer; }
    public LoadOp colorLoadOp() { return colorLoadOp; }
    public StoreOp colorStoreOp() { return colorStoreOp; }
    public LoadOp depthLoadOp() { return depthLoadOp; }
    public StoreOp depthStoreOp() { return depthStoreOp; }
    public boolean hasCustomClearColor() { return customClearColor; }
    public float clearR() { return clearR; }
    public float clearG() { return clearG; }
    public float clearB() { return clearB; }
    public float clearA() { return clearA; }
    public double clearDepth() { return clearDepth; }

    /** Mutable construction state for an immutable {@link ShaderPass}. */
    public static final class Builder {

        private final Type type;
        private MaterialMode materialMode;
        private BlendMode blendMode = BlendMode.OPAQUE;
        private final ShaderProgram shader;
        private RenderTarget output;
        private final List<PassInput> inputs = new ArrayList<>();
        private final Map<String, Object> uniforms = new LinkedHashMap<>();
        private Camera cameraOverride;
        private Material overrideMaterial;
        private int arrayLayer = -1;

        private LoadOp colorLoadOp;
        private StoreOp colorStoreOp = StoreOp.STORE;
        private LoadOp depthLoadOp;
        private StoreOp depthStoreOp;
        private boolean customClearColor;
        private float clearR;
        private float clearG;
        private float clearB;
        private float clearA = 1.0f;
        private double clearDepth;

        private Builder(Type type, MaterialMode materialMode, ShaderProgram shader) {
            this.type = type;
            this.materialMode = materialMode;
            this.shader = shader;

            if (type == Type.CORE) {
                colorLoadOp = LoadOp.CLEAR;
                depthLoadOp = LoadOp.CLEAR;
                depthStoreOp = StoreOp.STORE;
            } else {
                colorLoadOp = LoadOp.LOAD;
                depthLoadOp = LoadOp.DONT_CARE;
                depthStoreOp = StoreOp.DONT_CARE;
            }
        }

        /** Set the output target. {@code null} means framebuffer 0. */
        public Builder output(RenderTarget target) {
            this.output = target;
            return this;
        }

        /** Bind a previous render target to a post-effect sampler. */
        public Builder input(RenderTarget texture, String samplerName) {
            inputs.add(new PassInput(texture, samplerName));
            return this;
        }

        /** Set a per-pass uniform, applied after material parameters. */
        public Builder uniform(String name, Object value) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("uniform name must not be blank");
            }
            uniforms.put(name, Objects.requireNonNull(value, "uniform value"));
            return this;
        }

        /** Use {@code camera} instead of the scene camera. */
        public Builder camera(Camera camera) {
            this.cameraOverride = camera;
            return this;
        }

        /**
         * Replace every command material with {@code material} and select
         * {@link MaterialMode#OVERRIDE_MATERIAL}.
         */
        public Builder overrideMaterial(Material material) {
            this.overrideMaterial = Objects.requireNonNull(material, "material");
            this.materialMode = MaterialMode.OVERRIDE_MATERIAL;
            return this;
        }

        /** Select CORE material resolution explicitly. */
        public Builder materialMode(MaterialMode mode) {
            this.materialMode = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /**
         * Select the alpha-blending behavior of this CORE pass.
         *
         * <p>For non-{@link BlendMode#OPAQUE OPAQUE} modes the default color
         * and depth load operations become {@link LoadOp#LOAD} so the pass
         * composites over earlier passes; override with {@link #colorOps} or
         * {@link #depthOps} if a different behavior is required.</p>
         */
        public Builder blend(BlendMode mode) {
            this.blendMode = Objects.requireNonNull(mode, "mode");
            if (mode != BlendMode.OPAQUE && type == Type.CORE) {
                if (colorLoadOp == LoadOp.CLEAR) colorLoadOp = LoadOp.LOAD;
                if (depthLoadOp == LoadOp.CLEAR) depthLoadOp = LoadOp.LOAD;
            }
            return this;
        }

        /** Convenience alias for {@link #blend(BlendMode)} with {@link BlendMode#ALPHA_BLEND}. */
        public Builder transparent() {
            return blend(BlendMode.ALPHA_BLEND);
        }

        /** Convenience alias for {@link #blend(BlendMode)} with {@link BlendMode#ADDITIVE}. */
        public Builder additive() {
            return blend(BlendMode.ADDITIVE);
        }

        /** Select a texture-array layer on the output target. */
        public Builder arrayLayer(int layer) {
            if (layer < -1) {
                throw new IllegalArgumentException("layer must be >= -1");
            }
            this.arrayLayer = layer;
            return this;
        }

        public Builder colorOps(LoadOp load, StoreOp store) {
            this.colorLoadOp = Objects.requireNonNull(load, "load");
            this.colorStoreOp = Objects.requireNonNull(store, "store");
            return this;
        }

        public Builder depthOps(LoadOp load, StoreOp store) {
            this.depthLoadOp = Objects.requireNonNull(load, "load");
            this.depthStoreOp = Objects.requireNonNull(store, "store");
            return this;
        }

        public Builder colorLoad(LoadOp op) {
            this.colorLoadOp = Objects.requireNonNull(op, "op");
            return this;
        }

        public Builder colorStore(StoreOp op) {
            this.colorStoreOp = Objects.requireNonNull(op, "op");
            return this;
        }

        public Builder depthLoad(LoadOp op) {
            this.depthLoadOp = Objects.requireNonNull(op, "op");
            return this;
        }

        public Builder depthStore(StoreOp op) {
            this.depthStoreOp = Objects.requireNonNull(op, "op");
            return this;
        }

        /** Clear color using this value instead of the scene background. */
        public Builder clearColor(float r, float g, float b, float a) {
            this.colorLoadOp = LoadOp.CLEAR;
            this.customClearColor = true;
            this.clearR = r;
            this.clearG = g;
            this.clearB = b;
            this.clearA = a;
            return this;
        }

        /** Clear depth using this value. Reverse-Z normally uses 0. */
        public Builder clearDepth(double depth) {
            this.depthLoadOp = LoadOp.CLEAR;
            this.clearDepth = depth;
            return this;
        }

        /**
         * Validate and snapshot this builder. Subsequent builder changes do not
         * affect the returned pass.
         */
        public ShaderPass build() {
            validate();
            return new ShaderPass(this);
        }

        private void validate() {
            if (type == Type.POSTEFFECT) {
                if (shader == null) {
                    throw new IllegalStateException(
                            "POSTEFFECT pass requires a shader");
                }
                if (materialMode != MaterialMode.PASS_SHADER
                        || overrideMaterial != null) {
                    throw new IllegalStateException(
                            "POSTEFFECT pass cannot select a material mode");
                }
                if (blendMode != BlendMode.OPAQUE) {
                    throw new IllegalStateException(
                            "POSTEFFECT pass does not support blending");
                }
            } else {
                if (materialMode == MaterialMode.PASS_SHADER && shader == null) {
                    throw new IllegalStateException(
                            "PASS_SHADER mode requires a pass shader");
                }
                if (materialMode == MaterialMode.OVERRIDE_MATERIAL
                        && overrideMaterial == null) {
                    throw new IllegalStateException(
                            "OVERRIDE_MATERIAL mode requires an override material");
                }
                if (materialMode != MaterialMode.OVERRIDE_MATERIAL
                        && overrideMaterial != null) {
                    throw new IllegalStateException(
                            "override material requires OVERRIDE_MATERIAL mode");
                }
                if (!inputs.isEmpty()) {
                    throw new IllegalStateException(
                            "CORE pass inputs are not supported yet");
                }
            }

            if (arrayLayer >= 0) {
                if (output == null) {
                    throw new IllegalStateException(
                            "arrayLayer requires an output target");
                }
                if (output.getLayers() <= 1) {
                    throw new IllegalStateException(
                            "arrayLayer requires a layered output target");
                }
                if (arrayLayer >= output.getLayers()) {
                    throw new IllegalStateException(
                            "arrayLayer exceeds output target layers");
                }
            } else if (output != null && output.getLayers() > 1) {
                throw new IllegalStateException(
                        "A layered output target requires arrayLayer");
            }

            for (PassInput input : inputs) {
                if (input.texture() == output) {
                    throw new IllegalStateException(
                            "A pass cannot sample from its output target");
                }
            }
        }
    }
}
