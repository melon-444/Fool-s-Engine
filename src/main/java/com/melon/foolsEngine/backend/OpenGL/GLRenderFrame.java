package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.render.RenderThreadPool;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.RenderCommand;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;

import static org.lwjgl.opengl.GL45.*;

class GLRenderFrame implements RenderFrame{

    private final Queue<RenderCommand> commandQueue = new LinkedList<RenderCommand>();
    private Camera camera;
    private boolean init = false;
    private RenderThreadPool renderThreadPool;
    private LightEnvironment lightEnv;
    private static final int MAX_LIGHTS = 16;
    private static final int SHADOW_TEXTURE_BASE = 8;

    @Override
    public void init(){
        if(init){throw new IllegalStateException("Already init");}
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_GREATER);
        glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
        renderThreadPool = new RenderThreadPool();
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
            renderCommands(commands, null, null);
        }
    }

    @Override
    public void endFrame(RenderTarget target) {
        endFrame(target, null);
    }

    @Override
    public void endFrame(RenderTarget target, Material overrideMaterial) {
        initTest();

        List<RenderCommand> commands = new ArrayList<>(commandQueue);

        if (!commands.isEmpty()) {
            renderCommands(commands, target, overrideMaterial);
        }
    }

    private void renderCommands(List<RenderCommand> commands, RenderTarget target, Material overrideMaterial) {
        int[] savedViewport = null;
        if (target != null) {
            savedViewport = new int[4];
            glGetIntegerv(GL_VIEWPORT, savedViewport);
            target.bind();
            glViewport(0, 0, target.getWidth(), target.getHeight());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearDepth(0.0f);
        }

        Map<BatchKey, List<RenderCommand>> batches = groupCommands(commands);

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
            if (overrideMaterial == null && lightEnv != null) {
                uploadLightEnvironment(shader);
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
                    int slot = binder.bind(t);
                    shader.setInt(key, slot);
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
    public void setCamera(Camera camera) {
        initTest();
        this.camera = camera;
    }

    @Override
    public void submit(RenderCommand command) {
        initTest();
        commandQueue.add(command);
    }

    @Override
    public void applyLightEnvironment(LightEnvironment env) {
        initTest();
        this.lightEnv = env;
    }

    private void uploadLightEnvironment(ShaderProgram shader) {
        List<Light> lights = lightEnv.getLights();
        shader.setVec3("ambientColor", lightEnv.getAmbient().x, lightEnv.getAmbient().y, lightEnv.getAmbient().z);
        int count = Math.min(lights.size(), MAX_LIGHTS);
        shader.setInt("lightCount", count);

        int shadowSlot = SHADOW_TEXTURE_BASE;
        for (int i = 0; i < count; i++) {
            Light l = lights.get(i);
            String idx = "[" + i + "]";
            shader.setVec4("lightColor" + idx, l.color.x, l.color.y, l.color.z, l.intensity);
            shader.setVec4("lightDir" + idx, l.direction.x, l.direction.y, l.direction.z, 0f);
            shader.setVec4("lightPos" + idx, l.position.x, l.position.y, l.position.z, 0f);

            boolean hasShadow = l.castsShadow() && l.lightSpaceMatrices != null
                    && !l.lightSpaceMatrices.isEmpty() && !l.shadowMaps.isEmpty();
            shader.setVec4("lightParams" + idx, (float) l.type, l.cutOff, hasShadow ? 1f : 0f, 0f);

            if (hasShadow) {
                RenderTarget sm = l.shadowMaps.get(0);
                Matrix4f lsMatrix = l.lightSpaceMatrices.get(0);
                glActiveTexture(GL_TEXTURE0 + shadowSlot);
                glBindTexture(GL_TEXTURE_2D, sm.getTextureId());
                shader.setInt("shadowMaps[" + i + "]", shadowSlot);
                shader.setMat4("lightSpaceMatrices[" + i + "]", lsMatrix.get(new float[16]));
                shadowSlot++;
            }
        }
    }

    @Override
    public void setBackGroundColor(float r, float g, float b,float a) {
        initTest();
        glClearColor(r, g, b, a);
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
