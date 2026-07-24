package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;

import java.nio.file.Path;

class VKShaderProgram implements ShaderProgram {

    @Override
    public void load(Path vertexShaderPath, Path fragmentShaderPath) {
        throw new UnsupportedOperationException("Vulkan backend: use SPIR-V shaders via VKShaderProgram.loadSPIRV()");
    }

    @Override
    public void load(String vertexResource, String fragmentResource) {
        throw new UnsupportedOperationException("Vulkan backend: use SPIR-V shaders via VKShaderProgram.loadSPIRV()");
    }

    @Override
    public void bind() {
        throw new UnsupportedOperationException("Vulkan backend: bind via VkPipeline");
    }

    @Override
    public void unbind() {
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Vulkan backend destroy not yet implemented");
    }

    @Override
    public void setInt(String name, int value) {
    }

    @Override
    public void setFloat(String name, float value) {
    }

    @Override
    public void setVec2(String name, float x, float y) {
    }

    @Override
    public void setVec3(String name, float x, float y, float z) {
    }

    @Override
    public void setVec4(String name, float x, float y, float z, float w) {
    }

    @Override
    public void setMat4(String name, float[] mat) {
    }
}
