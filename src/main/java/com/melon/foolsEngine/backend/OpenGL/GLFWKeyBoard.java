package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputDevice;
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

    private Callback cb_key;
    private Callback cb_char;

    @Override
    public void attachEnvironment(Window env) {
        if (!(env instanceof GLWindow))
            throw new IllegalStateException("not a GLFW Window");
        cb_key = glfwSetKeyCallback(env.getID(), (window, key, scancode, action, mods) -> {
            switch (action) {
                case GLFW_RELEASE:
                    keyboard.put(key, false);
                    break;
                case GLFW_PRESS:
                    keyboard.put(key, true);
                    break;
                case GLFW_REPEAT:
                    break;
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
        });
        cb_char = glfwSetCharCallback(env.getID(), (window, codepoint) -> {
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
