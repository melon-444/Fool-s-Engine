package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL43.*;

class GLShaderProgram implements ShaderProgram {
    private int programID;
    private int vertexShaderID;
    private int fragmentShaderID;

    private final Map<String, Integer> uniformLocations = new HashMap<>();

    public GLShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        load(Path.of(vertexShaderSource), Path.of(fragmentShaderSource));
    }

    public GLShaderProgram() {
    }

    @Override
    public void load(Path vertexShaderPath, Path fragmentShaderPath) {
        StringBuilder vertexShaderSource = new StringBuilder();
        StringBuilder fragmentShaderSource = new StringBuilder();

        try (BufferedReader vertexShaderReader = new BufferedReader(new FileReader(vertexShaderPath.toFile()));
             BufferedReader fragmentShaderReader = new BufferedReader(new FileReader(fragmentShaderPath.toFile()))) {
            String vertexShader;
            String fragmentShader;
            while ((vertexShader = vertexShaderReader.readLine()) != null) {
                vertexShaderSource.append(vertexShader).append("\n");
            }
            while ((fragmentShader = fragmentShaderReader.readLine()) != null) {
                fragmentShaderSource.append(fragmentShader).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to load shader file:\n " + e.getMessage());
            System.exit(-1);
        }
        vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderID, vertexShaderSource);
        glCompileShader(vertexShaderID);
        checkCompileStatus(vertexShaderID,GL_VERTEX_SHADER);

        fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderID, fragmentShaderSource);
        glCompileShader(fragmentShaderID);
        checkCompileStatus(fragmentShaderID,GL_FRAGMENT_SHADER);

    }

    private void checkCompileStatus(int shaderID,int type) {
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Failed to compile" + (type == GL_VERTEX_SHADER ? " vert" : " frag") + " shader: " + glGetShaderInfoLog(shaderID));
            System.exit(-1);
        }
    }

    @Override
    public void bind() {
        this.programID = glCreateProgram();
        glAttachShader(programID, vertexShaderID);
        glAttachShader(programID, fragmentShaderID);
        //link vert and frag
        glLinkProgram(programID);
        //validate
        glValidateProgram(programID);
        glUseProgram(programID);
    }

    @Override
    public void unbind() {
        glUseProgram(0);
    }

    @Override
    public void destroy() {
        glDeleteProgram(programID);
        this.programID = 0;
        glDeleteShader(vertexShaderID);
        glDeleteShader(fragmentShaderID);
    }

    private int getUniformLocation(String name) {
        if(uniformLocations.containsKey(name))
            return uniformLocations.get(name);
        else{
            int location = glGetUniformLocation(programID, name);
            uniformLocations.put(name, location);
            return location;
        }
    }

    @Override
    public void setInt(String name, int value) {
        glUniform1i(getUniformLocation(name), value);
    }

    @Override
    public void setFloat(String name, float value) {
        glUniform1f(getUniformLocation(name), value);
    }

    @Override
    public void setVec2(String name, float x, float y) {
        glUniform2f(getUniformLocation(name), x, y);
    }

    @Override
    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(getUniformLocation(name), x, y, z);
    }

    @Override
    public void setVec4(String name, float x, float y, float z, float w) {
        glUniform4f(getUniformLocation(name), x, y, z, w);
    }

    @Override
    public void setMat4(String name, float[] mat) {
        /*
        Column major order
        If transpose is GL_FALSE, each matrix is assumed to be supplied in column major order.
        If transpose is GL_TRUE, each matrix is assumed to be supplied in row major order.
         */
        glUniformMatrix4fv(getUniformLocation(name), false, mat);
    }
}
