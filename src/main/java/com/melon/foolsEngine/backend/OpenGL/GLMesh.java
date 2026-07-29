package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.MeshDestroyedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.MeshUploadedEvent;
import com.melon.foolsEngine.util.VertexLayout;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL45.*;

class GLMesh implements Mesh {
    private int vao;
    private int vbo;
    private int ebo;
    private boolean uploaded = false;
    private MeshData meshData;
    private float[] aabb;

    private int instanceVBO = 0;
    private boolean instanceConfigured = false;
    private int instanceDataCapacity = 0;
    private static final int INSTANCE_MODEL_BASE = 3;

    @Override
    public void upload(MeshData data) {
        if(uploaded) return;
        meshData = data;
        uploaded = true;
        vao = createVAO();
        bindVAO();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        //vbo
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data.vertices(), GL_STATIC_DRAW);

        int strideBytes = data.layout().stride() * Float.BYTES;

        for (VertexLayout.VertexAttribute attr : data.layout().attributes()) {
            glEnableVertexAttribArray(attr.location());
            glVertexAttribPointer(
                    attr.location(),
                    attr.size(),
                    GL_FLOAT,
                    false,
                    strideBytes,
                    (long) attr.offset() * Float.BYTES
            );
        }
        //ebo
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indicesBuffer = BufferUtils.createIntBuffer(data.indices().length);
        indicesBuffer.put(data.indices());
        indicesBuffer.flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER,0);

        unbindVAO();

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new MeshUploadedEvent(this));
        computeAABB(data);
    }

    private void computeAABB(MeshData data) {
        float[] verts = data.vertices();
        VertexLayout layout = data.layout();
        int stride = layout.stride();
        int posOff = 0;
        for (VertexLayout.VertexAttribute attr : layout.attributes()) {
            if (attr.location() == 0) { posOff = attr.offset(); break; }
        }
        if (stride == 0 || verts.length < posOff + 3) { aabb = null; return; }
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = posOff; i + 2 < verts.length; i += stride) {
            float x = verts[i], y = verts[i + 1], z = verts[i + 2];
            if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
            if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
        }
        aabb = new float[]{ minX, minY, minZ, maxX, maxY, maxZ };
    }

    @Override
    public void destroy() {
        unbindVAO();
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        if (instanceVBO != 0) {
            glDeleteBuffers(instanceVBO);
            instanceVBO = 0;
        }
        glDeleteVertexArrays(vao);

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new MeshDestroyedEvent(this));
    }

    @Override
    public void bind() {
        if(!uploaded) {throw new IllegalStateException("Not uploaded!");}
        bindVAO();
    }

    @Override
    public void unbind() {
        if(!uploaded) {throw new IllegalStateException("Not uploaded!");}
        unbindVAO();
    }

    @Override
    public int indexCount() {
        return meshData.indices().length;
    }

    @Override
    public float[] getAABB() {
        return aabb;
    }

    void configureInstancedModelMatrix() {
        if (instanceConfigured) return;
        bindVAO();
        instanceVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        int stride = 16 * Float.BYTES;
        for (int i = 0; i < 4; i++) {
            int loc = INSTANCE_MODEL_BASE + i;
            glEnableVertexAttribArray(loc);
            glVertexAttribPointer(loc, 4, GL_FLOAT, false, stride, (long)i * 4 * Float.BYTES);
            glVertexAttribDivisor(loc, 1);
        }
        instanceConfigured = true;
        unbindVAO();
    }

    void uploadInstanceData(float[] data) {
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        int byteSize = data.length * Float.BYTES;
        if (byteSize > instanceDataCapacity) {
            glBufferData(GL_ARRAY_BUFFER, data, GL_DYNAMIC_DRAW);
            instanceDataCapacity = byteSize;
        } else {
            glBufferSubData(GL_ARRAY_BUFFER, 0, data);
        }
    }

    private int createVAO(){
        vao = glGenVertexArrays();
        return vao;
    }

    private void bindVAO(){
        glBindVertexArray(vao);
    }

    private void unbindVAO(){
        glBindVertexArray(0);
    }
}
