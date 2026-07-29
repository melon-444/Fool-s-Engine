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
package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputDevice;
import com.melon.foolsEngine.api.rendering.render.GraphicsContext;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.util.ImGuiHelper;
import org.joml.Vector2f;
import org.lwjgl.system.Callback;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

class GLFWKeyBoard implements InputDevice<Window> {
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
    private Callback cb_char;

    @Override
    public void attachEnvironment(Window env) {
        if (!(env instanceof GraphicsContext ctx)) {
            throw new IllegalArgumentException("OpenGL Environment must implement GraphicsContext");
        }
        long window = ctx.nativeHandle();
        cb_key = glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            switch (action) {
                case GLFW_RELEASE -> keyboard.put(key, false);
                case GLFW_PRESS -> keyboard.put(key, true);
                case GLFW_REPEAT -> {}
            }
            if (key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_RIGHT_SHIFT) {
                ImGuiHelper.setKeyShift(action != GLFW_RELEASE);
            }
            if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL) {
                ImGuiHelper.setKeyCtrl(action != GLFW_RELEASE);
            }
            if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT) {
                ImGuiHelper.setKeyAlt(action != GLFW_RELEASE);
            }
            if (key == GLFW_KEY_LEFT_SUPER || key == GLFW_KEY_RIGHT_SUPER) {
                ImGuiHelper.setKeySuper(action != GLFW_RELEASE);
            }
            if (action == GLFW_PRESS || action == GLFW_RELEASE) {
                boolean down = action == GLFW_PRESS;
                switch (key) {
                    case GLFW_KEY_BACKSPACE: ImGuiHelper.setKeyBackspace(down); break;
                    case GLFW_KEY_DELETE:    ImGuiHelper.setKeyDelete(down);    break;
                    case GLFW_KEY_ENTER:     ImGuiHelper.setKeyEnter(down);     break;
                    case GLFW_KEY_ESCAPE:    ImGuiHelper.setKeyEscape(down);    break;
                    case GLFW_KEY_TAB:       ImGuiHelper.setKeyTab(down);       break;
                    case GLFW_KEY_LEFT:      ImGuiHelper.setKeyLeft(down);      break;
                    case GLFW_KEY_RIGHT:     ImGuiHelper.setKeyRight(down);     break;
                    case GLFW_KEY_UP:        ImGuiHelper.setKeyUp(down);        break;
                    case GLFW_KEY_DOWN:      ImGuiHelper.setKeyDown(down);      break;
                    case GLFW_KEY_HOME:      ImGuiHelper.setKeyHome(down);      break;
                    case GLFW_KEY_END:       ImGuiHelper.setKeyEnd(down);       break;
                    case GLFW_KEY_SPACE:     ImGuiHelper.setKeySpace(down);     break;
                }
            }
        });
        cb_char = glfwSetCharCallback(window, (win, codepoint) -> {
            ImGuiHelper.addInputCharacter(codepoint);
        });
    }

    @Override
    public void detachEnvironment() {
        cb_key.free();
        cb_key = null;
        if (cb_char != null) {
            cb_char.free();
            cb_char = null;
        }
    }
}
