package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.util.VertexLayout;
import org.lwjgl.BufferUtils;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;

class GLMesh implements Mesh {
    private int vao;
    private int vbo;
    private int ebo;
    private boolean uploaded = false;
    private MeshData meshData;

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
    }

    @Override
    public void destroy() {
        unbindVAO();
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
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
