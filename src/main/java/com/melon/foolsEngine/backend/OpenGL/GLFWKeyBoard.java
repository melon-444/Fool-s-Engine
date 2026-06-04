package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputDevice;
import com.melon.foolsEngine.api.windows.Window;
import imgui.ImGui;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.system.Callback;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

class GLFWKeyBoard implements InputDevice<Window>{
    private final Map<Integer,Boolean> keyboard = new HashMap<>();

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
        return new Vector2f(0,0);
    }

    @Override
    public Vector2f getAxis2DDelta(FoolsEngineKeyCode id) {
        return new Vector2f(0,0);
    }

    @Override
    public void beginFrame() {
    }

    @Override
    public void endFrame() {
    }

    private Callback cb;

    @Override
    public void attachEnvironment(Window env) {
        if(!(env instanceof GLWindow))
            throw new IllegalStateException("not a GLFW Window");
        cb = glfwSetKeyCallback(env.getID(), (window,  key,  scancode,  action,  mods)->{
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
                ImGui.getIO().setKeyShift(action != GLFW_RELEASE);
            }
            if (key == GLFW_KEY_LEFT_CONTROL || key == GLFW_KEY_RIGHT_CONTROL) {
                ImGui.getIO().setKeyCtrl(action != GLFW_RELEASE);
            }
            if (key == GLFW_KEY_LEFT_ALT || key == GLFW_KEY_RIGHT_ALT) {
                ImGui.getIO().setKeyAlt(action != GLFW_RELEASE);
            }
            if (key == GLFW_KEY_LEFT_SUPER || key == GLFW_KEY_RIGHT_SUPER) {
                ImGui.getIO().setKeySuper(action != GLFW_RELEASE);
            }
        });
    }

    @Override
    public void detachEnvironment() {
        cb.free();
        cb = null;
    }


}
