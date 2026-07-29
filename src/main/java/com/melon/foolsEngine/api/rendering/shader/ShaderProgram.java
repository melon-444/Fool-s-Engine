// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

    /**
     * Loads, compiles, and links vertex and fragment shaders from classpath resources.
     * Works both in project and inside a fat JAR.
     * @param vertexResource classpath path, e.g. "/shader/vsh/main_vsh.glsl"
     * @param fragmentResource classpath path, e.g. "/shader/fsh/main_fsh.glsl"
     */
    void load(String vertexResource, String fragmentResource);

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
