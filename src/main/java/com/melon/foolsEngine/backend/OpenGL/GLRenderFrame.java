package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
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



    @Override
    public void init(){
        if(init){throw new IllegalStateException("Already init");}
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
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearDepth(0.0f);
        commandQueue.clear();
    }

    private final TextureBinder binder = new TextureBinder();

    @Override
    public void endFrame() {
        initTest();
        RenderCommand cmd = commandQueue.poll();
        ShaderProgram shader = null;
        Mesh mesh = null;

        while(cmd!=null){
            Material material = cmd.material();

            if(shader != material.shader()){
                shader = material.shader();
                material.shader().bind();
            }

            if(mesh != cmd.mesh()){
                mesh = cmd.mesh();
                cmd.mesh().bind();
            }

            binder.reset();
            for(String key:material.params().keySet()){
                Object param = material.params().get(key);
                if(param instanceof Float f){
                    material.shader().setFloat(key, f);
                }else if(param instanceof Integer i){
                    material.shader().setInt(key, i);
                }else if(param instanceof Vector2f v){
                    material.shader().setVec2(key,v.x,v.y);
                }else if(param instanceof Vector3f v){
                    material.shader().setVec3(key,v.x,v.y,v.z);
                }else  if(param instanceof Vector4f v){
                    material.shader().setVec4(key,v.x,v.y,v.z,v.w);
                }else  if(param instanceof Matrix4f m){
                    material.shader().setMat4(key,m.get(new float[16]));
                }else if(param instanceof Texture t){
                    int slot = binder.bind(t);
                    material.shader().setInt(key, slot);
                }
            }
            material.shader().setMat4("mvp",new Matrix4f(camera.vp()).mul(cmd.transform()).get(new float[16]));
            glDrawElements(GL_TRIANGLES, cmd.mesh().indexCount(), GL_UNSIGNED_INT, 0);

            cmd = commandQueue.poll();
        }
        commandQueue.clear();
    }

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
