package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.*;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
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

    private final Queue<RenderCommand> commandQueue = new LinkedList<>();
    private Camera camera;
    private boolean init = false;
    private LightEnvironment lightEnv;
    private int drawCallCounter;
    private float[] instanceBuffer = new float[0];
    private final float[] vpBuffer = new float[16];
    private GLMesh fullscreenQuad;
    private final TextureBinder binder = new TextureBinder();
    private static final Logger RENDERLOGGER = new Logger("Render Debug");

    @Override
    public void init() {
        if (init) { return; }
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
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

        List<RenderCommand> commands = new ArrayList<>(scene.getCommands());

        this.lightEnv = scene.getLighting();
        //RENDERLOGGER.debug("lightEnv=" + scene.getLighting() + " passes=" + scene.getPasses().size());

        TextureManager textureManager = scene.getTextureManager();
        if (textureManager != null) {
            textureManager.flushMipmaps();
        }

        List<ShaderPass> passes = scene.getPasses();

        if (passes.isEmpty() && !commands.isEmpty()) {
            this.camera = scene.getCamera();
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClearColor(scene.getBgR(), scene.getBgG(), scene.getBgB(), scene.getBgA());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearDepth(0.0f);
            renderCommands(commands, null, null, -1, null);
        } else {
            for (ShaderPass pass : passes) {
                executePass(pass, scene, commands);
            }
        }
    }

    private void executePass(ShaderPass pass, RenderScene scene, List<RenderCommand> commands) {
        Camera passCam = pass.cameraOverride() != null ? pass.cameraOverride() : scene.getCamera();
        this.camera = passCam;

        Material overrideMat = pass.overrideMaterial();
        RenderTarget target = pass.output();
        int layer = pass.arrayLayer();

        if (target != null) {
            target.bind();
            if (target.getLayers() > 1 && layer >= 0) {
                target.attachLayer(layer);
            }
            glViewport(0, 0, target.getWidth(), target.getHeight());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearDepth(0.0f);
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glClearColor(scene.getBgR(), scene.getBgG(), scene.getBgB(), scene.getBgA());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearDepth(0.0f);
        }

        if (pass.isFullscreen()) {
            executeFullscreenPass(pass);
        } else if (!commands.isEmpty()) {
            renderCommands(commands, null, overrideMat, layer, pass);
        }

        if (target != null) {
            target.unbind();
        }
    }

    private void executeFullscreenPass(ShaderPass pass) {
        pass.shader().bind();

        int slot = 0;
        for (PassInput input : pass.inputs()) {
            glActiveTexture(GL_TEXTURE0 + slot);
            if (input.texture().getType() == RenderTarget.TARGET_DEPTH) {
                glBindTexture(GL_TEXTURE_2D_ARRAY, input.texture().getTextureId());
            } else {
                glBindTexture(GL_TEXTURE_2D, input.texture().getTextureId());
            }
            pass.shader().setInt(input.samplerName(), slot);
            slot++;
        }

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

    private void renderCommands(List<RenderCommand> commands, RenderTarget target,
                                 Material overrideMaterial, int arrayLayer, ShaderPass pass) {
        int[] savedViewport = null;
        if (target != null) {
            savedViewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, savedViewport);
            target.bind();
            if (target.getLayers() > 1 && arrayLayer >= 0) {
                target.attachLayer(arrayLayer);
            }
            glViewport(0, 0, target.getWidth(), target.getHeight());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearDepth(0.0f);
        }

        Map<Long, List<RenderCommand>> batches = groupCommands(commands);
        drawCallCounter += batches.size();

        for (List<RenderCommand> cmds : batches.values()) {
            RenderCommand first = cmds.get(0);
            Mesh mesh = first.mesh();
            Material material = overrideMaterial != null ? overrideMaterial : first.material();
            ShaderProgram shader = material.shader();

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
            if (overrideMaterial == null && lightEnv != null) {
                bindShadowArrayTexture();
                lightEnv.apply(shader);
            }
            binder.reset();
            for (String key : material.params().keySet()) {
                Object param = material.params().get(key);
                if (param instanceof Float f) {
                    shader.setFloat(key, f);
                } else if (param instanceof Integer i) {
                    shader.setInt(key, i);
                } else if (param instanceof Vector2f v) {
                    shader.setVec2(key, v.x, v.y);
                } else if (param instanceof Vector3f v) {
                    shader.setVec3(key, v.x, v.y, v.z);
                } else if (param instanceof Vector4f v) {
                    shader.setVec4(key, v.x, v.y, v.z, v.w);
                } else if (param instanceof Matrix4f m) {
                    shader.setMat4(key, m.get(new float[16]));
                } else if (param instanceof Texture t) {
                    TextureManager tm = t.belongsTo();
                    if (tm != null) {
                        tm.bind(TextureManager.TEXTURE_ARRAY_SLOT);
                        shader.setInt("textureArray", TextureManager.TEXTURE_ARRAY_SLOT);
                        shader.setInt("textureLayer", t.getLayer());
                    } else {
                        int slot = binder.bind(t);
                        shader.setInt(key, slot);
                    }
                }
            }

            if (pass != null) {
                for (var e : pass.uniforms().entrySet()) {
                    bindUniformValue(shader, e.getKey(), e.getValue());
                }
            }

            camera.vp().get(vpBuffer);
            shader.setMat4("vp", vpBuffer);

            glDrawElementsInstanced(GL_TRIANGLES, mesh.indexCount(), GL_UNSIGNED_INT, 0, instanceCount);
        }

        if (target != null) {
            target.unbind();
            glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        }
    }

    private static void bindUniformValue(ShaderProgram shader, String name, Object value) {
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
            shader.setMat4(name, m.get(new float[16]));
        } else if (value instanceof Texture t) {
            // handled by material texture binding path above
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

    private Map<Long, List<RenderCommand>> groupCommands(List<RenderCommand> commands) {
        Map<Long, List<RenderCommand>> batches = new LinkedHashMap<>();
        for (RenderCommand c : commands) {
            long key = ((long) c.mesh().hashCode() << 32) ^ (c.material().hashCode() & 0xFFFFFFFFL);
            batches.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        return batches;
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
            int slot = nextSlot++;
            texture.bind(slot);
            bound.put(texture, slot);
            return slot;
        }

        public void reset() {
            bound.clear();
            nextSlot = 0;
        }
    }
}
