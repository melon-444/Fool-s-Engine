package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.ShaderDestroyedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.ShaderLoadedEvent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
            String line;
            while ((line = vertexShaderReader.readLine()) != null) {
                vertexShaderSource.append(line).append("\n");
            }
            while ((line = fragmentShaderReader.readLine()) != null) {
                fragmentShaderSource.append(line).append("\n");
            }
        } catch (IOException e) {
            System.err.println("Failed to load shader file:\n " + e.getMessage());
            System.exit(-1);
        }
        compileAndLink(vertexShaderSource, fragmentShaderSource);
    }

    @Override
    public void load(String vertexResource, String fragmentResource) {
        StringBuilder vSrc = new StringBuilder();
        StringBuilder fSrc = new StringBuilder();
        try {
            readResource(vertexResource, vSrc);
            readResource(fragmentResource, fSrc);
        } catch (IOException e) {
            System.err.println("Failed to load shader resource: " + e.getMessage());
            System.exit(-1);
        }
        compileAndLink(vSrc, fSrc);
    }

    private void readResource(String resourcePath, StringBuilder out) throws IOException {
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            in = ClassLoader.getSystemResourceAsStream(resourcePath.replaceFirst("^/", ""));
        }
        if (in == null) {
            throw new IOException("Shader resource not found: " + resourcePath);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
    }

    private void compileAndLink(StringBuilder vSrc, StringBuilder fSrc) {
        vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShaderID, vSrc);
        glCompileShader(vertexShaderID);
        checkCompileStatus(vertexShaderID, GL_VERTEX_SHADER);

        fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShaderID, fSrc);
        glCompileShader(fragmentShaderID);
        checkCompileStatus(fragmentShaderID, GL_FRAGMENT_SHADER);

        uniformLocations.clear();
        programID = glCreateProgram();
        glAttachShader(programID, vertexShaderID);
        glAttachShader(programID, fragmentShaderID);
        glLinkProgram(programID);
        glValidateProgram(programID);

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new ShaderLoadedEvent(this));
    }

    private void checkCompileStatus(int shaderID,int type) {
        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println("Failed to compile" + (type == GL_VERTEX_SHADER ? " vert" : " frag") + " shader: " + glGetShaderInfoLog(shaderID));
            System.exit(-1);
        }
    }

    @Override
    public void bind() {
        glUseProgram(programID);
    }

    @Override
    public void unbind() {
        glUseProgram(0);
    }

    @Override
    public void destroy() {
        uniformLocations.clear();
        glDeleteProgram(programID);
        this.programID = 0;
        glDeleteShader(vertexShaderID);
        glDeleteShader(fragmentShaderID);

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new ShaderDestroyedEvent(this));
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
