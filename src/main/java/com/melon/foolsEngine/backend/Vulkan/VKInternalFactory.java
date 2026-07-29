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

import com.melon.foolsEngine.api.InternalFactoryStub;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;

public class VKInternalFactory extends InternalFactoryStub {

    private final VKRenderFrame renderFrame = new VKRenderFrame();

    private VKInternalFactory() {}

    static {
        InternalFactoryStub.InjectVulkan(new VKInternalFactory());
    }

    @Override
    protected WindowsManager windowsManager() {
        return new VKWindowsManager(renderFrame);
    }

    @Override
    protected RenderFrame renderFrame() {
        return renderFrame;
    }

    @Override
    protected ShaderProgram shaderProgram() {
        return new VKShaderProgram();
    }

    @Override
    protected Texture texture() {
        return new VKTexture();
    }

    @Override
    protected Mesh mesh() {
        return new VKMesh();
    }

    @Override
    protected RenderTarget renderTarget(int width, int height, int type) {
        VKFrameBuffer fbo = new VKFrameBuffer();
        fbo.init(width, height, type);
        return fbo;
    }

    @Override
    protected RenderTarget renderTarget(int width, int height, int type, int layers) {
        VKFrameBuffer fbo = new VKFrameBuffer();
        fbo.init(width, height, type, layers);
        return fbo;
    }

    @Override
    protected <E> InputManager inputManager(E env) {
        if (env instanceof com.melon.foolsEngine.api.windows.Window window) {
            InputManager im = new InputManager();
            VKKeyBoard kb = new VKKeyBoard();
            VKMouse mouse = new VKMouse();
            kb.attachEnvironment(window);
            mouse.attachEnvironment(window);
            im.registerKeyboard(kb);
            im.registerMouse(mouse);
            return im;
        }
        throw new IllegalArgumentException("Vulkan implementation needs Window instance instead of " + env.getClass().getName());
    }

    @Override
    protected TextureManager textureManager(int width, int height, int maxLayers) {
        return new VKTextureManager(width, height, maxLayers);
    }
}
