package com.melon.foolsEngine.api.rendering.resource;

public interface Mesh {
    void upload(MeshData data);
    void destroy();
    void bind();
    void unbind();
    int indexCount();
}
