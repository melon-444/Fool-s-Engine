package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputDevice;
import com.melon.foolsEngine.api.windows.Window;
import imgui.ImGui;
import org.joml.Vector2f;
import org.lwjgl.system.Callback;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_REPEAT;

class GLFWMouse implements InputDevice<Window> {

    private float scrollY = 0.0f;
    private float scrollX = 0.0f;
    private float scrollDeltaY = 0.0f;
    private float scrollDeltaX = 0.0f;
    private float screenY = 0.0f;
    private float screenX = 0.0f;
    private float screenDeltaY = 0.0f;
    private float screenDeltaX = 0.0f;
    private boolean firstMouse = true;
    private Window env;

    private final Map<Integer,Boolean> mouseButton = new HashMap<>();
    private final Map<Integer,Float> mouseWheel = new HashMap<>();
    private final Map<Integer,Float> mouseWheelDel = new HashMap<>();
    private final Map<Integer,Vector2f> mousePosition = new HashMap<>();
    private final Map<Integer,Vector2f> mousePositionDel = new HashMap<>();

    @Override
    public boolean getButton(FoolsEngineKeyCode id) {
        return mouseButton.getOrDefault(id.getId(), false);
    }

    @Override
    public float getAxis1D(FoolsEngineKeyCode id) {
        return mouseWheel.getOrDefault(id.getId(),0.0f);
    }

    @Override
    public float getAxis1DDelta(FoolsEngineKeyCode id) {
        return mouseWheelDel.getOrDefault(id.getId(),0.0f);
    }

    @Override
    public Vector2f getAxis2D(FoolsEngineKeyCode id) {
        return mousePosition.getOrDefault(id.getId(),new Vector2f(0,0));
    }

    @Override
    public Vector2f getAxis2DDelta(FoolsEngineKeyCode id) {
        return  mousePositionDel.getOrDefault(id.getId(),new Vector2f(0,0));
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

    private Callback cb_button;
    private Callback cb_wheel;
    private Callback cb_position;

    @Override
    public void attachEnvironment(Window env) {
        this.env = env;
        if(!(env instanceof GLWindow))
            throw new IllegalStateException("not a GLFW Window");
        cb_button = glfwSetMouseButtonCallback(env.getID(), (window,  button,  action,  mods)->{
            switch (action) {
                case GLFW_RELEASE:
                    mouseButton.put(button, false);
                    if (ImGui.getCurrentContext().ptr != 0) {
                        ImGui.getIO().setMouseClicked(button, false);
                        ImGui.getIO().setMouseDown(button, false);
                    }
                    break;
                case GLFW_PRESS:
                    mouseButton.put(button, true);
                    if (ImGui.getCurrentContext().ptr != 0) {
                        ImGui.getIO().setMouseClicked(button, true);
                        ImGui.getIO().setMouseDown(button, true);
                    }
                    break;
                case GLFW_REPEAT:
                    if (ImGui.getCurrentContext().ptr != 0)
                        ImGui.getIO().setMouseDown(button,true);
                    break;
            }
        });
        cb_wheel = glfwSetScrollCallback(env.getID(),(window,xoffset,yoffset)->{
            scrollDeltaY = (float)yoffset - scrollY;
            scrollDeltaX = (float)xoffset - scrollX;
            scrollY = (float)yoffset;
            scrollX = (float)xoffset;
            mouseWheel.put(GLFW_MOUSE_BUTTON_MIDDLE,scrollY);
            mouseWheelDel.put(GLFW_MOUSE_BUTTON_MIDDLE,scrollDeltaY);
            if (ImGui.getCurrentContext().ptr != 0) {
                ImGui.getIO().setMouseWheel(scrollY);
                ImGui.getIO().setMouseWheelH(scrollX);
            }
        });
        cb_position = glfwSetCursorPosCallback(env.getID(), (window,  xpos,  ypos)->{
                float newX = (float)xpos;
                float newY = (float)ypos;

                if (firstMouse) {
                    screenX = newX;
                    screenY = newY;
                    firstMouse = false;
                    mousePosition.put(GLFW_CURSOR, new Vector2f(screenX, screenY));
                    if (ImGui.getCurrentContext().ptr != 0)
                        ImGui.getIO().setMousePos(screenX,screenY);
                    return;
                }

                screenDeltaX = newX - screenX;
                screenDeltaY = newY - screenY;
                screenX = newX;
                screenY = newY;
                if (ImGui.getCurrentContext().ptr != 0) {
                    ImGui.getIO().setMousePos(screenX,screenY);
                    ImGui.getIO().setMouseDelta(screenDeltaX,screenDeltaY);
                }

                if (Math.abs(screenDeltaX) > 200 || Math.abs(screenDeltaY) > 200) {
                    mousePosition.put(GLFW_CURSOR, new Vector2f(screenX, screenY));
                    return;
                }

                if(!mousePosition.containsKey(GLFW_CURSOR))
                    mousePosition.put(GLFW_CURSOR,new Vector2f(screenX,screenY));
                else
                    mousePosition.get(GLFW_CURSOR).set(screenX,screenY);

                if(!mousePositionDel.containsKey(GLFW_CURSOR))
                    mousePositionDel.put(GLFW_CURSOR,new Vector2f(screenDeltaX,screenDeltaY));
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
        env = null;
        firstMouse = true;
    }

}
