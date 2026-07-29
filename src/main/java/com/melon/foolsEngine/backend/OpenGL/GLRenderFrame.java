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
package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.*;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.pipeline.PassInput;
import com.melon.foolsEngine.api.rendering.pipeline.ShaderPass;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.util.VertexLayout;
import com.melon.foolsEngine.util.logger.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;

import static org.lwjgl.opengl.GL45.*;

class GLRenderFrame implements RenderFrame {

    private Camera camera;
    private boolean init = false;
    private LightEnvironment lightEnv;
    private int drawCallCounter;
    private float[] instanceBuffer = new float[0];
    private final float[] vpBuffer = new float[16];
    private final float[] uniformMatrixBuffer = new float[16];
    private GLMesh fullscreenQuad;
    private final TextureBinder binder = new TextureBinder();
    private static final Logger RENDERLOGGER = new Logger("Render Debug");

    /*
     * Renderer-owned OpenGL state cache. Code outside this renderer must not
     * mutate these states directly without invalidating the corresponding
     * cache entry.
     */
    private boolean viewportKnown;
    private int viewportX;
    private int viewportY;
    private int viewportW;
    private int viewportH;

    private boolean scissorEnabledKnown;
    private boolean scissorEnabled;
    private boolean scissorBoxKnown;
    private int scissorX;
    private int scissorY;
    private int scissorW;
    private int scissorH;

    private boolean clearColorKnown;
    private int clearColorRBits;
    private int clearColorGBits;
    private int clearColorBBits;
    private int clearColorABits;

    private boolean clearDepthKnown;
    private double clearDepth;

    private boolean depthTestEnabledKnown;
    private boolean depthTestEnabled;

    @Override
    public void init() {
        if (init) { return; }
        setDepthTestEnabled(true);
        glDepthFunc(GL_GREATER);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        setScissorEnabled(false);
        setClearDepth(0.0);
        init = true;
    }
    private void initTest() {
        if (!init) throw new IllegalStateException("RenderFrame didn't initialize yet!");
    }


    // ────────────────────── render / pass system ──────────────────────

    @Override
    public void render(RenderScene scene) {
        initTest();
        drawCallCounter = 0;

        /*
         * Window-system code may have changed GL_VIEWPORT since the previous
         * frame (for example after a resize). Keep the cache frame-local:
         * query at most once this frame, then reuse it across all passes.
         */
        viewportKnown = false;

        try {
            List<RenderCommand> commands = new ArrayList<>(scene.getCommands());

            this.lightEnv = scene.getLighting();

            TextureManager textureManager = scene.getTextureManager();
            if (textureManager != null) {
                textureManager.flushMipmaps();
            }

            List<ShaderPass> passes = scene.getPasses();
            for (ShaderPass pass : passes) {
                executePass(pass, scene, commands);
            }

        } finally {
            // Do not leak a screen-space clipping rectangle to external rendering code.
            setScissorEnabled(false);
            // Keep the renderer's documented default for external rendering hooks.
            setDepthTestEnabled(true);
        }
    }

    private void executePass(ShaderPass pass, RenderScene scene, List<RenderCommand> commands) {
        ensureViewportKnown();
        int savedViewportX = viewportX;
        int savedViewportY = viewportY;
        int savedViewportW = viewportW;
        int savedViewportH = viewportH;

        Camera passCam = pass.cameraOverride() != null ? pass.cameraOverride() : scene.getCamera();
        this.camera = passCam;
        if (pass.type() == ShaderPass.Type.CORE && passCam == null) {
            throw new IllegalStateException("CORE pass requires a scene or pass camera");
        }
        if (passCam != null) {
            RENDERLOGGER.trace("override=%s proj.m11=%.4f view.m03=%.1f m13=%.1f",
                    pass.cameraOverride() != null, passCam.projection.m11(),
                    passCam.view.m03(), passCam.view.m13());
        }

        RenderTarget target = pass.output();
        int layer = pass.arrayLayer();

        if (target != null) {
            setScissorEnabled(false);
            target.bind();
            if (target.getLayers() > 1 && layer >= 0) {
                target.attachLayer(layer);
            }
            setViewport(0, 0, target.getWidth(), target.getHeight());
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            applyScreenViewport(scene, passCam,
                    pass.colorLoadOp() == ShaderPass.LoadOp.CLEAR);
        }

        invalidateLoadAttachments(pass, target);
        clearPassAttachments(pass, scene, target);

        if (pass.type() == ShaderPass.Type.POSTEFFECT) {
            setDepthTestEnabled(false);
            executePostEffectPass(pass);
        } else {
            setDepthTestEnabled(true);
            if (!commands.isEmpty()) {
                renderCommands(commands, pass);
            }
        }

        invalidateStoreAttachments(pass, target);

        if (target != null) {
            target.unbind();
        }

        setViewport(savedViewportX, savedViewportY, savedViewportW, savedViewportH);
    }

    private void clearPassAttachments(ShaderPass pass, RenderScene scene, RenderTarget target) {
        int mask = 0;

        if (pass.colorLoadOp() == ShaderPass.LoadOp.CLEAR
                && (target == null || target.getType() == RenderTarget.TARGET_COLOR)) {
            if (pass.hasCustomClearColor()) {
                setClearColor(pass.clearR(), pass.clearG(), pass.clearB(), pass.clearA());
            } else {
                setClearColor(scene.getBgR(), scene.getBgG(), scene.getBgB(), scene.getBgA());
            }
            mask |= GL_COLOR_BUFFER_BIT;
        }

        if (pass.depthLoadOp() == ShaderPass.LoadOp.CLEAR) {
            setClearDepth(pass.clearDepth());
            mask |= GL_DEPTH_BUFFER_BIT;
        }

        if (mask != 0) {
            glClear(mask);
        }
    }

    private void invalidateLoadAttachments(ShaderPass pass, RenderTarget target) {
        invalidateAttachments(
                pass.colorLoadOp() == ShaderPass.LoadOp.DONT_CARE,
                pass.depthLoadOp() == ShaderPass.LoadOp.DONT_CARE,
                target);
    }

    private void invalidateStoreAttachments(ShaderPass pass, RenderTarget target) {
        invalidateAttachments(
                pass.colorStoreOp() == ShaderPass.StoreOp.DONT_CARE,
                pass.depthStoreOp() == ShaderPass.StoreOp.DONT_CARE,
                target);
    }

    private void invalidateAttachments(
            boolean discardColor, boolean discardDepth, RenderTarget target) {
        boolean hasColor = target == null
                || target.getType() == RenderTarget.TARGET_COLOR;
        boolean invalidateColor = discardColor && hasColor;

        if (!invalidateColor && !discardDepth) {
            return;
        }

        int colorAttachment = target == null ? GL_COLOR : GL_COLOR_ATTACHMENT0;
        int depthAttachment = target == null ? GL_DEPTH : GL_DEPTH_ATTACHMENT;

        if (invalidateColor && discardDepth) {
            glInvalidateFramebuffer(
                    GL_FRAMEBUFFER,
                    new int[]{colorAttachment, depthAttachment});
        } else {
            glInvalidateFramebuffer(
                    GL_FRAMEBUFFER,
                    new int[]{invalidateColor ? colorAttachment : depthAttachment});
        }
    }

    /**
     * Applies a screen viewport, optionally letterbox/pillarbox to preserve the
     * camera projection aspect ratio. Clears the black-bar areas.
     * Falls back to the current GL viewport when the scene provides no dimensions.
     */
    private void applyScreenViewport(RenderScene scene, Camera cam, boolean clearBars) {
        int vpW = scene.getScreenViewportW();
        int vpH = scene.getScreenViewportH();

        if (vpW <= 0 || vpH <= 0) {
            ensureViewportKnown();
            vpW = viewportW;
            vpH = viewportH;
        }
        if (vpW <= 0 || vpH <= 0) return;

        if (!scene.isPreserveScreenAspect() || cam == null || cam.projection == null) {
            setViewport(0, 0, vpW, vpH);
            setScissorEnabled(false);
            return;
        }

        float projAspect = Math.abs(cam.projection.m11() / cam.projection.m00());
        int vx, vy, vw, vh;


        vh = (int) (vpW / projAspect);
        vw = vpW;
        vx = 0;
        vy = (vpH - vh) / 2;


        setScissorEnabled(true);
        if (clearBars) {
            setScissor(0, 0, vpW, vpH);
            setClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);
        }

        setViewport(vx, vy, vw, vh);
        setScissor(vx, vy, vw, vh);
    }

    // ────────────────────── OpenGL state cache ──────────────────────

    private void ensureViewportKnown() {
        if (viewportKnown) return;

        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        viewportX = viewport[0];
        viewportY = viewport[1];
        viewportW = viewport[2];
        viewportH = viewport[3];
        viewportKnown = true;
    }

    private void setViewport(int x, int y, int width, int height) {
        if (viewportKnown
                && viewportX == x
                && viewportY == y
                && viewportW == width
                && viewportH == height) {
            return;
        }

        glViewport(x, y, width, height);
        viewportX = x;
        viewportY = y;
        viewportW = width;
        viewportH = height;
        viewportKnown = true;
    }

    private void setScissorEnabled(boolean enabled) {
        if (scissorEnabledKnown && scissorEnabled == enabled) {
            return;
        }

        if (enabled) {
            glEnable(GL_SCISSOR_TEST);
        } else {
            glDisable(GL_SCISSOR_TEST);
        }

        scissorEnabled = enabled;
        scissorEnabledKnown = true;
    }

    private void setScissor(int x, int y, int width, int height) {
        if (scissorBoxKnown
                && scissorX == x
                && scissorY == y
                && scissorW == width
                && scissorH == height) {
            return;
        }

        glScissor(x, y, width, height);
        scissorX = x;
        scissorY = y;
        scissorW = width;
        scissorH = height;
        scissorBoxKnown = true;
    }

    private void setClearColor(float red, float green, float blue, float alpha) {
        int redBits = Float.floatToIntBits(red);
        int greenBits = Float.floatToIntBits(green);
        int blueBits = Float.floatToIntBits(blue);
        int alphaBits = Float.floatToIntBits(alpha);

        if (clearColorKnown
                && clearColorRBits == redBits
                && clearColorGBits == greenBits
                && clearColorBBits == blueBits
                && clearColorABits == alphaBits) {
            return;
        }

        glClearColor(red, green, blue, alpha);
        clearColorRBits = redBits;
        clearColorGBits = greenBits;
        clearColorBBits = blueBits;
        clearColorABits = alphaBits;
        clearColorKnown = true;
    }

    private void setClearDepth(double depth) {
        if (clearDepthKnown
                && Double.doubleToLongBits(clearDepth) == Double.doubleToLongBits(depth)) {
            return;
        }

        glClearDepth(depth);
        clearDepth = depth;
        clearDepthKnown = true;
    }

    private void setDepthTestEnabled(boolean enabled) {
        if (depthTestEnabledKnown && depthTestEnabled == enabled) {
            return;
        }

        if (enabled) {
            glEnable(GL_DEPTH_TEST);
        } else {
            glDisable(GL_DEPTH_TEST);
        }

        depthTestEnabled = enabled;
        depthTestEnabledKnown = true;
    }

    /**
     * Must be called after switching this renderer to another OpenGL context,
     * or after external code mutates cached OpenGL state directly.
     */
    void invalidateStateCache() {
        viewportKnown = false;
        scissorEnabledKnown = false;
        scissorBoxKnown = false;
        clearColorKnown = false;
        clearDepthKnown = false;
        depthTestEnabledKnown = false;
    }

    private void executePostEffectPass(ShaderPass pass) {
        pass.shader().bind();

        int slot = 0;
        for (PassInput input : pass.inputs()) {
            slot = nextUserTextureSlot(slot);
            glActiveTexture(GL_TEXTURE0 + slot);
            if (input.texture().getType() == RenderTarget.TARGET_DEPTH) {
                glBindTexture(GL_TEXTURE_2D_ARRAY, input.texture().getTextureId());
            } else {
                glBindTexture(GL_TEXTURE_2D, input.texture().getTextureId());
            }
            pass.shader().setInt(input.samplerName(), slot);
            slot++;
        }

        binder.reset(slot);
        for (var e : pass.uniforms().entrySet()) {
            bindUniformValue(pass.shader(), e.getKey(), e.getValue());
        }

        drawFullscreenQuad();

        pass.shader().unbind();
    }

    private void drawFullscreenQuad() {
        if (fullscreenQuad == null) createFullscreenQuad();
        fullscreenQuad.bind();
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        fullscreenQuad.unbind();
    }

    private static int nextUserTextureSlot(int slot) {
        while (slot == LightEnvironment.SHADOW_ARRAY_SLOT
                || slot == TextureManager.TEXTURE_ARRAY_SLOT) {
            slot++;
        }
        return slot;
    }

    private void createFullscreenQuad() {
        float[] vertices = {
                -1, -1, 0,   0, 0,
                1, -1, 0,   1, 0,
                1,  1, 0,   1, 1,
                -1,  1, 0,   0, 1,
        };
        int[] indices = {0, 1, 2, 2, 3, 0};
        VertexLayout layout = new VertexLayout().add(0, 3).add(1, 2);
        MeshData data = new MeshData(vertices, indices, layout);
        fullscreenQuad = new GLMesh();
        fullscreenQuad.upload(data);
    }

    // ────────────────────── core draw ──────────────────────

    private void renderCommands(List<RenderCommand> commands, ShaderPass pass) {
        Map<BatchKey, List<RenderCommand>> batches = groupCommands(commands, pass);
        drawCallCounter += batches.size();

        for (Map.Entry<BatchKey, List<RenderCommand>> batch : batches.entrySet()) {
            BatchKey key = batch.getKey();
            List<RenderCommand> cmds = batch.getValue();
            RenderCommand first = cmds.getFirst();
            Mesh mesh = first.mesh();
            Material material = key.material;
            ShaderProgram shader = key.shader;

            GLMesh glMesh = (GLMesh) mesh;
            glMesh.configureInstancedModelMatrix();

            int instanceCount = cmds.size();
            int floatCount = instanceCount * 16;
            if (instanceBuffer.length < floatCount)
                instanceBuffer = new float[floatCount];
            for (int i = 0; i < instanceCount; i++) {
                cmds.get(i).transform().get(instanceBuffer, i * 16);
            }

            glMesh.bind();
            glMesh.uploadInstanceData(instanceBuffer);

            shader.bind();
            shader.setInt("textureLayer", -1);
            if (pass.materialMode() != ShaderPass.MaterialMode.OVERRIDE_MATERIAL
                    && lightEnv != null) {
                bindShadowArrayTexture();
                lightEnv.apply(shader);
            }
            //texture and Light
            binder.reset();
            for (String pkey : material.params().keySet()) {
                Object param = material.params().get(pkey);
                bindUniformValue(shader, pkey, param);
            }

            if (pass != null) {
                for (var e : pass.uniforms().entrySet()) {
                    bindUniformValue(shader, e.getKey(), e.getValue());
                }
            }

            camera.vp().get(vpBuffer);
            shader.setMat4("vp", vpBuffer);

            glDrawElementsInstanced(GL_TRIANGLES, mesh.indexCount(), GL_UNSIGNED_INT, 0, instanceCount);
            shader.unbind();
            glMesh.unbind();
        }
    }

    private void bindUniformValue(ShaderProgram shader, String name, Object value) {
        if (value instanceof Float f) {
            shader.setFloat(name, f);
        } else if (value instanceof Integer i) {
            shader.setInt(name, i);
        } else if (value instanceof Vector2f v) {
            shader.setVec2(name, v.x, v.y);
        } else if (value instanceof Vector3f v) {
            shader.setVec3(name, v.x, v.y, v.z);
        } else if (value instanceof Vector4f v) {
            shader.setVec4(name, v.x, v.y, v.z, v.w);
        } else if (value instanceof Matrix4f m) {
            shader.setMat4(name, m.get(uniformMatrixBuffer));
        } else if (value instanceof Texture t) {
            TextureManager tm = t.belongsTo();
            if (tm != null) {
                tm.bind(TextureManager.TEXTURE_ARRAY_SLOT);
                shader.setInt("textureArray", TextureManager.TEXTURE_ARRAY_SLOT);
                shader.setInt("textureLayer", t.getLayer());
            } else {
                int slot = binder.bind(t);
                shader.setInt(name, slot);
            }
        }
    }

    // ────────────────────── screenshots ──────────────────────

    @Override
    public void screenShot(ByteBuffer dstBuf) {
        initTest();
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        glReadPixels(viewport[0], viewport[1], viewport[2], viewport[3], GL_RGBA, GL_UNSIGNED_BYTE, dstBuf);
    }

    @Override
    public void screenShot(Path path) {
        initTest();
        int[] viewport = new int[4];
        glGetIntegerv(GL_VIEWPORT, viewport);
        int w = viewport[2], h = viewport[3];
        ByteBuffer pixels = org.lwjgl.system.MemoryUtil.memAlloc(w * h * 4);
        try {
            glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            writePng(path, pixels, w, h);
        } finally {
            org.lwjgl.system.MemoryUtil.memFree(pixels);
        }
    }

    @Override
    public void screenShot(Path path, RenderTarget target) {
        initTest();
        int w = target.getWidth(), h = target.getHeight();
        target.bind();
        ByteBuffer pixels = org.lwjgl.system.MemoryUtil.memAlloc(w * h * 4);
        try {
            glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            writePng(path, pixels, w, h);
        } finally {
            org.lwjgl.system.MemoryUtil.memFree(pixels);
            target.unbind();
        }
    }

    private void writePng(Path path, ByteBuffer pixels, int w, int h) {
        ByteBuffer flipped = org.lwjgl.system.MemoryUtil.memAlloc(w * h * 4);
        try {
            int rowSize = w * 4;
            byte[] row = new byte[rowSize];
            for (int y = 0; y < h; y++) {
                int srcOffset = (h - 1 - y) * rowSize;
                pixels.position(srcOffset);
                pixels.get(row);
                flipped.position(y * rowSize);
                flipped.put(row);
            }
            flipped.flip();
            org.lwjgl.stb.STBImageWrite.stbi_write_png(path.toString(), w, h, 4, flipped, rowSize);
        } finally {
            org.lwjgl.system.MemoryUtil.memFree(flipped);
        }
    }

    // ────────────────────── shadows ──────────────────────

    private void bindShadowArrayTexture() {
        if (lightEnv == null || lightEnv.getLights().isEmpty()) return;
        for (var l : lightEnv.getLights()) {
            if (l.castsShadow() && !l.shadowInfo.shadowMaps().isEmpty()) {
                RenderTarget sm = l.shadowInfo.shadowMaps().get(0);
                glActiveTexture(GL_TEXTURE0 + LightEnvironment.SHADOW_ARRAY_SLOT);
                if (sm.getType() == RenderTarget.TARGET_DEPTH) {
                    glBindTexture(GL_TEXTURE_2D_ARRAY, sm.getTextureId());
                } else {
                    glBindTexture(GL_TEXTURE_2D, sm.getTextureId());
                }
                return;
            }
        }
    }

    // ────────────────────── batching ──────────────────────

    private Map<BatchKey, List<RenderCommand>> groupCommands(
            List<RenderCommand> commands, ShaderPass pass) {
        Map<BatchKey, List<RenderCommand>> batches = new LinkedHashMap<>();
        for (RenderCommand c : commands) {
            Material material;
            ShaderProgram shader;

            switch (pass.materialMode()) {
                case COMMAND_MATERIAL -> {
                    material = c.material();
                    shader = material.shader();
                }
                case PASS_SHADER -> {
                    material = c.material();
                    shader = pass.shader();
                }
                case OVERRIDE_MATERIAL -> {
                    material = pass.overrideMaterial();
                    shader = material.shader();
                }
                default -> throw new IllegalStateException(
                        "Unsupported material mode: " + pass.materialMode());
            }

            BatchKey key = new BatchKey(c.mesh(), material, shader);
            batches.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        return batches;
    }

    /**
     * Identity is intentional: GPU resources and mutable materials are batching
     * units even if a future implementation adds value-based equals methods.
     */
    private static final class BatchKey {
        private final Mesh mesh;
        private final Material material;
        private final ShaderProgram shader;
        private final int hash;

        private BatchKey(Mesh mesh, Material material, ShaderProgram shader) {
            this.mesh = Objects.requireNonNull(mesh, "mesh");
            this.material = Objects.requireNonNull(material, "material");
            this.shader = Objects.requireNonNull(shader, "shader");
            this.hash = 31 * (31 * System.identityHashCode(mesh)
                    + System.identityHashCode(material))
                    + System.identityHashCode(shader);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof BatchKey key
                    && mesh == key.mesh
                    && material == key.material
                    && shader == key.shader;
        }
    }


    @Override
    public int getDrawCallCount() {
        return drawCallCounter;
    }

    // ────────────────────── TextureBinder ──────────────────────

    private static class TextureBinder {
        private final Map<Texture, Integer> bound = new HashMap<>();
        private int nextSlot = 0;

        public int bind(Texture texture) {
            if (bound.containsKey(texture)) {
                return bound.get(texture);
            }
            nextSlot = nextUserTextureSlot(nextSlot);
            int slot = nextSlot++;
            texture.bind(slot);
            bound.put(texture, slot);
            return slot;
        }

        public void reset() {
            reset(0);
        }

        public void reset(int firstFreeSlot) {
            bound.clear();
            nextSlot = firstFreeSlot;
        }
    }
}
