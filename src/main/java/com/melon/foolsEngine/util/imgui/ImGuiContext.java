package com.melon.foolsEngine.util.imgui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.glfw.ImGuiImplGlfw;
import imgui.gl3.ImGuiImplGl3;

public class ImGuiContext {

    private ImGuiImplGlfw glfw;
    private ImGuiImplGl3 gl3;
    private boolean initialized;

    public void init(long windowHandle, String glslVersion) {
        if (initialized) {
            return;
        }

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);

        glfw = new ImGuiImplGlfw();
        glfw.initForOpenGL(windowHandle, false);

        gl3 = new ImGuiImplGl3();
        gl3.init(glslVersion);

        initialized = true;
    }

    public void destroy() {
        if (!initialized) {
            return;
        }
        if (gl3 != null) {
            gl3.shutdown();
        }
        if (glfw != null) {
            glfw.shutdown();
        }
        ImGui.destroyContext();
        initialized = false;
    }

    public ImGuiImplGlfw getGlfw() {
        return glfw;
    }

    public ImGuiImplGl3 getGl3() {
        return gl3;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
