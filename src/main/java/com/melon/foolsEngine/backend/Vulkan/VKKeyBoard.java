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

import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputDevice;
import com.melon.foolsEngine.api.rendering.render.GraphicsContext;
import com.melon.foolsEngine.api.windows.Window;
import org.joml.Vector2f;
import org.lwjgl.system.Callback;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

class VKKeyBoard implements InputDevice<Window> {
    private final Map<Integer, Boolean> keyboard = new HashMap<>();

    @Override
    public boolean getButton(FoolsEngineKeyCode id) {
        return keyboard.getOrDefault(id.getId(), false);
    }

    @Override
    public float getAxis1D(FoolsEngineKeyCode id) {
        return 0;
    }

    @Override
    public float getAxis1DDelta(FoolsEngineKeyCode id) {
        return 0;
    }

    @Override
    public Vector2f getAxis2D(FoolsEngineKeyCode id) {
        return new Vector2f(0, 0);
    }

    @Override
    public Vector2f getAxis2DDelta(FoolsEngineKeyCode id) {
        return new Vector2f(0, 0);
    }

    @Override
    public void beginFrame() {
    }

    @Override
    public void endFrame() {
    }

    @Override
    public void flushDeltas() {
    }

    private Callback cb_key;

    @Override
    public void attachEnvironment(Window env) {
        if (!(env instanceof GraphicsContext ctx)) {
            throw new IllegalArgumentException("Vulkan Environment must implement GraphicsContext");
        }
        long window = ctx.nativeHandle();
        cb_key = glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            switch (action) {
                case GLFW_RELEASE -> keyboard.put(key, false);
                case GLFW_PRESS -> keyboard.put(key, true);
                case GLFW_REPEAT -> {}
            }
        });
    }

    @Override
    public void detachEnvironment() {
        cb_key.free();
        cb_key = null;
    }
}
