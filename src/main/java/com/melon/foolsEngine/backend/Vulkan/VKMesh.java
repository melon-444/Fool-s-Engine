package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.MeshData;

class VKMesh implements Mesh {

    private boolean uploaded = false;

    @Override
    public void upload(MeshData data) {
        throw new UnsupportedOperationException("Vulkan backend mesh upload not yet implemented");
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Vulkan backend mesh destroy not yet implemented");
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException("Vulkan backend: bind via VkCommandBuffer");
    }

    @Override
    public void unbind() {
    }

    @Override
    public int indexCount() {
        return 0;
    }
}
