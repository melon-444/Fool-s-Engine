package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderThreadPool;
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

        if (commands.isEmpty()) {
            return;
        }

        Map<BatchKey, List<RenderCommand>> batches = groupCommands(commands);

        for (Map.Entry<BatchKey, List<RenderCommand>> entry : batches.entrySet()) {
            Mesh mesh = entry.getKey().mesh;
            Material material = entry.getKey().material;
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

        commandQueue.clear();
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
