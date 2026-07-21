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

package com.melon.foolsEngine.core.world;


import com.melon.foolsEngine.api.APIFactory;
import com.melon.foolsEngine.api.InternalFactoryStub;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;


/**
 * Responsible for every service instance which needs to use underlying code.
 */
public class ServiceFactory implements APIFactory {


    public static final int OPENGL_BACKEND = 0;
    public static final int VULKAN_BACKEND = 1;

    private static int BackEndType = OPENGL_BACKEND;

    public static void setBackEndType(int type) {
        BackEndType = type;
    }

    @Override
    public WindowsManager getWindowsManager() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getWindowsManager();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getWindowsManager();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public RenderFrame getRenderFrame() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getRenderFrame();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getRenderFrame();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public ShaderProgram getShaderProgram() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getShaderProgram();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getShaderProgram();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public Texture getTexture() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getTexture();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getTexture();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public Mesh getMesh() {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().getMesh();
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().getMesh();
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, int type) {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().createRenderTarget(width, height, type);
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().createRenderTarget(width, height, type);
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, int type, int layers) {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().createRenderTarget(width, height, type, layers);
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().createRenderTarget(width, height, type, layers);
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public <E> InputManager createInputManager(E env) {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().createInputManager(env);
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().createInputManager(env);
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }

    @Override
    public TextureManager createTextureManager(int width, int height, int maxLayers) {
        return switch (BackEndType) {
            case OPENGL_BACKEND -> InternalFactoryStub.OpenGLINSTANCE().createTextureManager(width, height, maxLayers);
            case VULKAN_BACKEND -> InternalFactoryStub.VulkanINSTANCE().createTextureManager(width, height, maxLayers);
            default -> throw new RuntimeException("Unsupported backend type");
        };
    }
}

