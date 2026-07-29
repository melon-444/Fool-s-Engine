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
package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;

import static org.lwjgl.vulkan.VK13.*;

class VKShaderProgram implements ShaderProgram {

    private VkDevice device;
    private long vertModule;
    private long fragModule;
    private boolean compiled;

    VKShaderProgram() {}

    void setDevice(VkDevice device) {
        this.device = device;
    }

    long getVertModule() { return vertModule; }
    long getFragModule() { return fragModule; }

    @Override
    public void load(Path vertexShaderPath, Path fragmentShaderPath) {
        compile(readFile(vertexShaderPath), readFile(fragmentShaderPath));
    }

    @Override
    public void load(String vertexResource, String fragmentResource) {
        compile(readResource(vertexResource), readResource(fragmentResource));
    }

    private void compile(String vertSrc, String fragSrc) {
        if (compiled || device == null) return;

        vertModule = compileSPIRV(vertSrc, "shader.vert",
                org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader);
        fragModule = compileSPIRV(fragSrc, "shader.frag",
                org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader);
        compiled = true;
    }

    private long compileSPIRV(String source, String filename, int shaderKind) {
        long compiler = org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize();
        if (compiler == MemoryUtil.NULL) throw new RuntimeException("shaderc init failed");

        long options = org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_initialize();
        org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_target_env(options,
                org.lwjgl.util.shaderc.Shaderc.shaderc_target_env_vulkan,
                org.lwjgl.util.shaderc.Shaderc.shaderc_env_version_vulkan_1_3);
        org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_set_optimization_level(options,
                org.lwjgl.util.shaderc.Shaderc.shaderc_optimization_level_performance);

        long result = org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv(
                compiler, source, shaderKind, filename, "main", options);

        int status = org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status(result);
        if (status != org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success) {
            String err = org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message(result);
            org.lwjgl.util.shaderc.Shaderc.shaderc_result_release(result);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_release(options);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release(compiler);
            throw new RuntimeException("SPIR-V compile failed: " + filename + "\n" + err);
        }

        ByteBuffer spirv = org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes(result);
        int codeSize = spirv.remaining();
        ByteBuffer code = MemoryUtil.memAlloc(codeSize);
        MemoryUtil.memCopy(spirv, code);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(code);

            LongBuffer pModule = stack.mallocLong(1);
            int vkResult = vkCreateShaderModule(device, createInfo, null, pModule);
            MemoryUtil.memFree(code);

            org.lwjgl.util.shaderc.Shaderc.shaderc_result_release(result);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_release(options);
            org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release(compiler);

            if (vkResult != VK_SUCCESS) throw new RuntimeException("vkCreateShaderModule failed");
            return pModule.get(0);
        }
    }

    @Override
    public void bind() {}
    @Override
    public void unbind() {}

    @Override
    public void destroy() {
        if (vertModule != 0) {
            vkDestroyShaderModule(device, vertModule, null);
            vertModule = 0;
        }
        if (fragModule != 0) {
            vkDestroyShaderModule(device, fragModule, null);
            fragModule = 0;
        }
    }

    @Override public void setInt(String name, int value) {}
    @Override public void setFloat(String name, float value) {}
    @Override public void setVec2(String name, float x, float y) {}
    @Override public void setVec3(String name, float x, float y, float z) {}
    @Override public void setVec4(String name, float x, float y, float z, float w) {}
    @Override public void setMat4(String name, float[] mat) {}

    private static String readFile(Path path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader: " + path, e);
        }
        return sb.toString();
    }

    private static String readResource(String path) {
        StringBuilder sb = new StringBuilder();
        InputStream in = VKShaderProgram.class.getResourceAsStream(path);
        if (in == null) in = ClassLoader.getSystemResourceAsStream(path.replaceFirst("^/", ""));
        if (in == null) throw new RuntimeException("Shader resource not found: " + path);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader: " + path, e);
        }
        return sb.toString();
    }
}
