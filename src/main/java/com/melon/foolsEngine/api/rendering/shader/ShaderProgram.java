package com.melon.foolsEngine.api.rendering.shader;

import java.nio.file.Path;

public interface ShaderProgram {
    void load(Path vertexShaderPath, Path fragmentShaderPath);
    public void bind();
    public void unbind();
    public void destroy();

    void setInt(String name, int value);
    void setFloat(String name, float value);

    void setVec2(String name, float x, float y);
    void setVec3(String name, float x, float y, float z);
    void setVec4(String name, float x, float y, float z, float w);

    void setMat4(String name, float[] mat);
}
