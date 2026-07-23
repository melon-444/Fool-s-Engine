package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.render.RenderThreadPool;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowManager;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;

import static org.lwjgl.opengl.GL45.*;

class GLRenderFrame implements RenderFrame{

    private final Queue<RenderCommand> commandQueue = new LinkedList<RenderCommand>();
    private Camera camera;
    private boolean init = false;
    private LightEnvironment lightEnv;
    private int drawCallCounter;

    @Override
    public void init(){
        if(init){return;}
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        init = true;
    }

    private void initTest(){
        if(!init) throw new IllegalStateException("RenderFrame didn't initialize yet!");
    }

    @Override
    public void beginFrame() {
        initTest();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearDepth(0.0f);
        commandQueue.clear();
    }

    private final TextureBinder binder = new TextureBinder();

    @Override
    public void endFrame() {
        initTest();

        List<RenderCommand> commands = new ArrayList<>();
        RenderCommand cmd;
        while ((cmd = commandQueue.poll()) != null) {
            commands.add(cmd);
        }
        commandQueue.clear();

        if (!commands.isEmpty()) {
            renderCommands(commands, null, null, -1);
        }
    }

    @Override
    public void endFrame(RenderTarget target) {
        endFrame(target, null);
    }

    @Override
    public void endFrame(RenderTarget target, Material overrideMaterial) {
        endFrame(target, overrideMaterial, -1);
    }

    @Override
    public void endFrame(RenderTarget target, Material overrideMaterial, int arrayLayer) {
        initTest();

        List<RenderCommand> commands = new ArrayList<>(commandQueue);

        if (!commands.isEmpty()) {
            renderCommands(commands, target, overrideMaterial, arrayLayer);
        }
    }

    @Override
    public void render(RenderScene scene) {
        initTest();

        drawCallCounter = 0;

        List<RenderCommand> commands = new ArrayList<>(scene.getCommands());
        if (commands.isEmpty()) return;

        LightEnvironment lighting = scene.getLighting();
        ShadowManager shadowManager = lighting != null ? lighting.getShadowManager() : null;

        if (shadowManager != null) {
            Camera mainCamera = scene.getCamera();
            if (mainCamera != null) {
                for (Light light : lighting.getLights()) {
                    if (!light.castsShadow()) continue;
                    ShadowPassContext ctx = shadowManager.prepareShadow(light, mainCamera);
                    this.camera = ctx.shadowCamera();
                    renderCommands(commands, ctx.target(), ctx.depthMaterial(), ctx.layer());
                }
            }
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glClearColor(scene.getBgR(), scene.getBgG(), scene.getBgB(), scene.getBgA());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearDepth(0.0f);

        this.camera = scene.getCamera();
        this.lightEnv = scene.getLighting();

        TextureManager textureManager = scene.getTextureManager();
        if (textureManager != null) {
            textureManager.flushMipmaps();
        }

        renderCommands(commands, null, null, -1);
    }

    private void renderCommands(List<RenderCommand> commands, RenderTarget target, Material overrideMaterial, int arrayLayer) {
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

        Map<BatchKey, List<RenderCommand>> batches = groupCommands(commands);

        drawCallCounter += batches.size();

        for (Map.Entry<BatchKey, List<RenderCommand>> entry : batches.entrySet()) {
            Mesh mesh = entry.getKey().mesh;
            Material material = overrideMaterial != null ? overrideMaterial : entry.getKey().material;
            ShaderProgram shader = material.shader();
            List<RenderCommand> cmds = entry.getValue();

            GLMesh glMesh = (GLMesh) mesh;
            glMesh.configureInstancedModelMatrix();

            int instanceCount = cmds.size();
            float[] transforms = new float[instanceCount * 16];
            for (int i = 0; i < instanceCount; i++) {
                cmds.get(i).transform().get(transforms, i * 16);
            }

            glMesh.bind();
            glMesh.uploadInstanceData(transforms);

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
            shader.setMat4("vp", camera.vp().get(new float[16]));

            glDrawElementsInstanced(GL_TRIANGLES, mesh.indexCount(), GL_UNSIGNED_INT, 0, instanceCount);
        }

        if (target != null) {
            target.unbind();
            glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        }
    }

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

    private Map<BatchKey, List<RenderCommand>> groupCommands(List<RenderCommand> commands) {
        Map<BatchKey, List<RenderCommand>> batches = new LinkedHashMap<>();
        for (RenderCommand c : commands) {
            BatchKey key = new BatchKey(c.mesh(), c.material());
            batches.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        return batches;
    }

    private record BatchKey(Mesh mesh, Material material) {}

    @Override
    @Deprecated
    public void setCamera(Camera camera) {
        initTest();
        this.camera = camera;
    }

    @Override
    @Deprecated
    public void submit(RenderCommand command) {
        initTest();
        commandQueue.add(command);
    }

    @Override
    @Deprecated
    public void applyLightEnvironment(LightEnvironment env) {
        initTest();
        this.lightEnv = env;
    }

    @Override
    @Deprecated
    public void setBackGroundColor(float r, float g, float b,float a) {
        initTest();
        glClearColor(r, g, b, a);
    }

    @Override
    public int getDrawCallCount() {
        return drawCallCounter;
    }

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
