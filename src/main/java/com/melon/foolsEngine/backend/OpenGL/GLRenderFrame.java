package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.*;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowManager;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;
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

    @Override
    public void init() {
        if (init) { return; }
        glEnable(GL_DEPTH_TEST);
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
//            boolean hardcodedPass = passes.isEmpty() && !commands.isEmpty();
//            if (hardcodedPass)
//                hardcoded(scene,commands);
//            else
                for (ShaderPass pass : passes) {
                    executePass(pass, scene, commands);
                }

        } finally {
            // Do not leak a screen-space clipping rectangle to external rendering code.
            setScissorEnabled(false);
        }
    }

    @Deprecated
    private void hardcoded(RenderScene scene,List<RenderCommand> commands) {
        //hardcoded shadow pass
        LightEnvironment lighting = this.lightEnv;
        ShadowManager shadowManager = lighting != null ? lighting.getShadowManager() : null;
        if (shadowManager != null) {
            Camera mainCamera = scene.getCamera();
            if (mainCamera != null) {
                Camera camCopy = new Camera(
                        new org.joml.Matrix4f(mainCamera.view),
                        new org.joml.Matrix4f(mainCamera.projection));
                for (Light light : lighting.getLights()) {
                    if (!light.castsShadow()) continue;
                    ShadowPassContext ctx = shadowManager.prepareShadow(light, camCopy);
                    this.camera = ctx.shadowCamera();
                    renderCommands(commands, ctx.target(), ctx.depthMaterial(), ctx.layer(), null);
                }
            }
        }

        this.camera = scene.getCamera();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        applyScreenViewport(scene, this.camera);
        setClearColor(scene.getBgR(), scene.getBgG(), scene.getBgB(), scene.getBgA());
        setClearDepth(0.0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderCommands(commands, null, null, -1, null);
    }

    private void executePass(ShaderPass pass, RenderScene scene, List<RenderCommand> commands) {
        ensureViewportKnown();
        int savedViewportX = viewportX;
        int savedViewportY = viewportY;
        int savedViewportW = viewportW;
        int savedViewportH = viewportH;

        Camera passCam = pass.cameraOverride() != null ? pass.cameraOverride() : scene.getCamera();
        this.camera = passCam;
        RENDERLOGGER.trace( "override=%s proj.m11=%.4f view.m03=%.1f m13=%.1f", pass.cameraOverride() != null, passCam.projection.m11(), passCam.view.m03(), passCam.view.m13());
        Material overrideMat = pass.overrideMaterial();
        RenderTarget target = pass.output();
        int layer = pass.arrayLayer();

        if (target != null) {
            setScissorEnabled(false);
            target.bind();
            if (target.getLayers() > 1 && layer >= 0) {
                target.attachLayer(layer);
            }
            setViewport(0, 0, target.getWidth(), target.getHeight());
            setClearDepth(0.0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            applyScreenViewport(scene, passCam);
            setClearColor(scene.getBgR(), scene.getBgG(), scene.getBgB(), scene.getBgA());
            setClearDepth(0.0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }

        if (pass.isFullscreen()) {
            executeFullscreenPass(pass);
        } else if (!commands.isEmpty()) {
            renderCommands(commands, null, overrideMat, layer, pass);
        }

        if (target != null) {
            target.unbind();
        }

        setViewport(savedViewportX, savedViewportY, savedViewportW, savedViewportH);
    }

    /**
     * Applies a screen viewport, optionally letterbox/pillarbox to preserve the
     * camera projection aspect ratio. Clears the black-bar areas.
     * Falls back to the current GL viewport when the scene provides no dimensions.
     */
    private void applyScreenViewport(RenderScene scene, Camera cam) {
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
            setScissor(0, 0, vpW, vpH);
            return;
        }

        float projAspect = Math.abs(cam.projection.m11() / cam.projection.m00());
        int vx, vy, vw, vh;


        vh = (int) (vpW / projAspect);
        vw = vpW;
        vx = 0;
        vy = (vpH - vh) / 2;


        setScissorEnabled(true);
        setScissor(0, 0, vpW, vpH);
        setClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);

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
        int savedViewportX = 0;
        int savedViewportY = 0;
        int savedViewportW = 0;
        int savedViewportH = 0;
        if (target != null) {
            ensureViewportKnown();
            savedViewportX = viewportX;
            savedViewportY = viewportY;
            savedViewportW = viewportW;
            savedViewportH = viewportH;

            setScissorEnabled(false);
            target.bind();
            if (target.getLayers() > 1 && arrayLayer >= 0) {
                target.attachLayer(arrayLayer);
            }
            setViewport(0, 0, target.getWidth(), target.getHeight());
            setClearDepth(0.0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }

        Map<Long, List<RenderCommand>> batches = groupCommands(commands);
        drawCallCounter += batches.size();

        for (List<RenderCommand> cmds : batches.values()) {
            RenderCommand first = cmds.getFirst();
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
            //texture and Light
            binder.reset();
            for (String key : material.params().keySet()) {
                Object param = material.params().get(key);
                bindUniformValue(shader, key, param);
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
            setViewport(savedViewportX, savedViewportY, savedViewportW, savedViewportH);
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