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

package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

public abstract class InternalFactoryStub implements APIFactory {

    static{
            try {
                Class.forName("com.melon.foolsEngine.backend.OpenGL.GLInternalFactory");
            } catch (ClassNotFoundException e) {
                throw new InternalError(e);
            }
    }

    protected abstract WindowsManager windowsManager();
    protected abstract RenderFrame renderFrame();
    protected abstract ShaderProgram shaderProgram();
    protected abstract Texture texture();
    protected abstract Mesh mesh();
    protected abstract RenderTarget renderTarget(int width, int height, int type);
    protected abstract <E> InputManager inputManager(E env);
    protected abstract TextureManager textureManager(int width, int height, int maxLayers);

    protected RenderTarget renderTarget(int width, int height, int type, int layers) {
        return renderTarget(width, height, type);
    }

    private static InternalFactoryStub OpenGLINSTANCE;
    private static InternalFactoryStub VulkanINSTANCE;

    public static APIFactory OpenGLINSTANCE() {
        if (OpenGLINSTANCE == null)
            throw new IllegalStateException("Factory not initialized");
        return OpenGLINSTANCE;
    }

    public static APIFactory VulkanINSTANCE() {
        if (VulkanINSTANCE == null)
            throw new IllegalStateException("Factory not initialized");
        return VulkanINSTANCE;
    }

    protected static void InjectVulkan(InternalFactoryStub instance) {
        if (VulkanINSTANCE != null)
            throw new IllegalStateException("Factory already initialized");
        VulkanINSTANCE = instance;
    }

    protected static void InjectOpenGL(InternalFactoryStub instance) {
        if (OpenGLINSTANCE != null)
            throw new IllegalStateException("Factory already initialized");
        OpenGLINSTANCE = instance;
    }

    @Override
    public WindowsManager getWindowsManager(){
        return windowsManager();
    }
    @Override
    public RenderFrame getRenderFrame(){
        return renderFrame();
    }
    @Override
    public ShaderProgram getShaderProgram(){
        return shaderProgram();
    }
    @Override
    public Texture getTexture(){
        return texture();
    }
    @Override
    public Mesh getMesh(){
        return mesh();
    }
    @Override
    public RenderTarget createRenderTarget(int width, int height, int type) {
        return renderTarget(width, height, type);
    }

    @Override
    public RenderTarget createRenderTarget(int width, int height, int type, int layers) {
        return renderTarget(width, height, type, layers);
    }

    @Override
    public <E> InputManager createInputManager(E env) {
        return inputManager(env);
    }

    @Override
    public TextureManager createTextureManager(int width, int height, int maxLayers) {
        return textureManager(width, height, maxLayers);
    }
}
