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

class GLFWMouse implements InputDevice<Window> {

    private float scrollY;
    private float scrollX;
    private float scrollDeltaY;
    private float scrollDeltaX;
    private float screenY;
    private float screenX;
    private float screenDeltaY;
    private float screenDeltaX;
    private boolean firstMouse = true;

    private final Map<Integer, Boolean> mouseButton = new HashMap<>();
    private final Map<Integer, Float> mouseWheel = new HashMap<>();
    private final Map<Integer, Float> mouseWheelDel = new HashMap<>();
    private final Map<Integer, Vector2f> mousePosition = new HashMap<>();
    private final Map<Integer, Vector2f> mousePositionDel = new HashMap<>();

    @Override
    public boolean getButton(FoolsEngineKeyCode id) {
        return mouseButton.getOrDefault(id.getId(), false);
    }

    @Override
    public float getAxis1D(FoolsEngineKeyCode id) {
        return mouseWheel.getOrDefault(id.getId(), 0.0f);
    }

    @Override
    public float getAxis1DDelta(FoolsEngineKeyCode id) {
        return mouseWheelDel.getOrDefault(id.getId(), 0.0f);
    }

    @Override
    public Vector2f getAxis2D(FoolsEngineKeyCode id) {
        return mousePosition.getOrDefault(id.getId(), new Vector2f(0, 0));
    }

    @Override
    public Vector2f getAxis2DDelta(FoolsEngineKeyCode id) {
        return mousePositionDel.getOrDefault(id.getId(), new Vector2f(0, 0));
    }

    @Override
    public void beginFrame() {
    }

    @Override
    public void endFrame() {
        mouseButton.clear();
        mouseWheel.clear();
        mouseWheelDel.clear();
        mousePosition.clear();
        mousePositionDel.clear();
        scrollY = 0;
        scrollX = 0;
        scrollDeltaY = 0;
        scrollDeltaX = 0;
    }

    @Override
    public void flushDeltas() {
        mousePositionDel.clear();
        mousePosition.clear();
        firstMouse = true;
    }

    private Callback cb_button;
    private Callback cb_wheel;
    private Callback cb_position;

    @Override
    public void attachEnvironment(Window env) {
        if (!(env instanceof GraphicsContext ctx)) {
            throw new IllegalArgumentException("Environment must implement GraphicsContext");
        }
        long window = ctx.nativeHandle();
        cb_button = glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            switch (action) {
                case GLFW_RELEASE:
                    mouseButton.put(button, false);
                    ImGuiHelper.setMousePress(button, false);
                    break;
                case GLFW_PRESS:
                    mouseButton.put(button, true);
                    ImGuiHelper.setMousePress(button, true);
                    break;
                case GLFW_REPEAT:
                    ImGuiHelper.setMouseRepeat(button);
                    break;
            }
        });
        cb_wheel = glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            scrollDeltaY = (float) yoffset - scrollY;
            scrollDeltaX = (float) xoffset - scrollX;
            scrollY = (float) yoffset;
            scrollX = (float) xoffset;
            mouseWheel.put(GLFW_MOUSE_BUTTON_MIDDLE, scrollY);
            mouseWheelDel.put(GLFW_MOUSE_BUTTON_MIDDLE, scrollDeltaY);
            ImGuiHelper.setMouseWheel(scrollY);
            ImGuiHelper.setMouseWheelH(scrollX);
        });
        cb_position = glfwSetCursorPosCallback(window, (win, xpos, ypos) -> {
            float newX = (float) xpos;
            float newY = (float) ypos;

            if (firstMouse) {
                screenX = newX;
                screenY = newY;
                firstMouse = false;
                mousePosition.put(GLFW_CURSOR, new Vector2f(screenX, screenY));
                ImGuiHelper.setMousePos(screenX, screenY);
                return;
            }

            screenDeltaX = newX - screenX;
            screenDeltaY = newY - screenY;
            screenX = newX;
            screenY = newY;
            ImGuiHelper.setMousePos(screenX, screenY);
            ImGuiHelper.setMouseDelta(screenDeltaX, screenDeltaY);

            if (Math.abs(screenDeltaX) > 200 || Math.abs(screenDeltaY) > 200) {
                mousePosition.put(GLFW_CURSOR, new Vector2f(screenX, screenY));
                return;
            }

            if (!mousePosition.containsKey(GLFW_CURSOR))
                mousePosition.put(GLFW_CURSOR, new Vector2f(screenX, screenY));
            else
                mousePosition.get(GLFW_CURSOR).set(screenX, screenY);

            if (!mousePositionDel.containsKey(GLFW_CURSOR))
                mousePositionDel.put(GLFW_CURSOR, new Vector2f(screenDeltaX, screenDeltaY));
            else
                mousePositionDel.get(GLFW_CURSOR).add(screenDeltaX, screenDeltaY);
        });
    }

    @Override
    public void detachEnvironment() {
        cb_button.free();
        cb_wheel.free();
        cb_position.free();
        cb_button = null;
        cb_wheel = null;
        cb_position = null;
        firstMouse = true;
    }
}
