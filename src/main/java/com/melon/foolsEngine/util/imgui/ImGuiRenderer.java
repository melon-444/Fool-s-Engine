package com.melon.foolsEngine.util.imgui;

import imgui.ImGui;
import imgui.glfw.ImGuiImplGlfw;
import imgui.gl3.ImGuiImplGl3;

public class ImGuiRenderer {

    private final ImGuiImplGlfw glfw;
    private final ImGuiImplGl3 gl3;

    public ImGuiRenderer(ImGuiContext context) {
        this.glfw = context.getGlfw();
        this.gl3 = context.getGl3();
    }

    public void beginFrame() {
        glfw.newFrame();
        gl3.newFrame();
        ImGui.newFrame();
    }

    public void endFrame() {
        ImGui.render();
        gl3.renderDrawData(ImGui.getDrawData());
    }
}
