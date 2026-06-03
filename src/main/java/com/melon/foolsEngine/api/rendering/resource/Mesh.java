package com.melon.foolsEngine.api.rendering.resource;

/**
 * A GPU-resident mesh with vertex and index buffers.
 * Obtain an instance via {@link com.melon.foolsEngine.core.world.ServiceFactory#getMesh()}.
 */
public interface Mesh {
    /** Uploads mesh data to the GPU */
    void upload(MeshData data);
    /** Releases GPU resources */
    void destroy();
    /** Binds the mesh's VAO for rendering */
    void bind();
    /** Unbinds the VAO */
    void unbind();
    /** @return the number of indices in the element buffer */
    int indexCount();
}
