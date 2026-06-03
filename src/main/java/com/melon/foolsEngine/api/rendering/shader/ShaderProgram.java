package com.melon.foolsEngine.api.rendering.shader;

import java.nio.file.Path;

/**
 * A compiled and linked GPU shader program.
 * Obtain an instance via {@link com.melon.foolsEngine.core.world.ServiceFactory#getShaderProgram()}.
 */
public interface ShaderProgram {
    /**
     * Loads, compiles, and links vertex and fragment shaders from source files.
     * @param vertexShaderPath path to the vertex shader source
     * @param fragmentShaderPath path to the fragment shader source
     */
    void load(Path vertexShaderPath, Path fragmentShaderPath);
    /** Binds this shader program for rendering */
    void bind();
    /** Unbinds the shader program */
    void unbind();
    /** Releases all GPU resources associated with this program */
    void destroy();

    void setInt(String name, int value);
    void setFloat(String name, float value);

    void setVec2(String name, float x, float y);
    void setVec3(String name, float x, float y, float z);
    void setVec4(String name, float x, float y, float z, float w);

    void setMat4(String name, float[] mat);
}
