package com.melon.foolsEngine.util;

/**
 * Optional bridge that forwards input events to Dear ImGui when imgui-java is on the classpath.
 * All forwarding becomes a no-op when the imgui dependency is absent — no class-loading side effects.
 */
public final class ImGuiHelper {

    private static final boolean AVAILABLE;
    static {
        boolean available = false;
        try {
            Class.forName("imgui.ImGui");
            available = true;
        } catch (ClassNotFoundException ignored) {
        }
        AVAILABLE = available;
    }

    private ImGuiHelper() {
    }

    public static void setKeyShift(boolean down) {
        if (AVAILABLE) ImGuiInternal.setKeyShift(down);
    }

    public static void setKeyCtrl(boolean down) {
        if (AVAILABLE) ImGuiInternal.setKeyCtrl(down);
    }

    public static void setKeyAlt(boolean down) {
        if (AVAILABLE) ImGuiInternal.setKeyAlt(down);
    }

    public static void setKeySuper(boolean down) {
        if (AVAILABLE) ImGuiInternal.setKeySuper(down);
    }

    public static void setMousePress(int button, boolean pressed) {
        if (AVAILABLE) ImGuiInternal.setMousePress(button, pressed);
    }

    public static void setMouseRepeat(int button) {
        if (AVAILABLE) ImGuiInternal.setMouseRepeat(button);
    }

    public static void setMouseWheel(float y) {
        if (AVAILABLE) ImGuiInternal.setMouseWheel(y);
    }

    public static void setMouseWheelH(float x) {
        if (AVAILABLE) ImGuiInternal.setMouseWheelH(x);
    }

    public static void setMousePos(float x, float y) {
        if (AVAILABLE) ImGuiInternal.setMousePos(x, y);
    }

    public static void setMouseDelta(float dx, float dy) {
        if (AVAILABLE) ImGuiInternal.setMouseDelta(dx, dy);
    }

    public static void setWantCaptureMouse(boolean want) {
        if (AVAILABLE) ImGuiInternal.setWantCaptureMouse(want);
    }

    private static final class ImGuiInternal {
        static void setKeyShift(boolean down) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setKeyShift(down);
        }

        static void setKeyCtrl(boolean down) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setKeyCtrl(down);
        }

        static void setKeyAlt(boolean down) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setKeyAlt(down);
        }

        static void setKeySuper(boolean down) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setKeySuper(down);
        }

        static void setMousePress(int button, boolean pressed) {
            if (imgui.ImGui.getCurrentContext().ptr != 0) {
                imgui.ImGui.getIO().setMouseClicked(button, pressed);
                imgui.ImGui.getIO().setMouseDown(button, pressed);
            }
        }

        static void setMouseRepeat(int button) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setMouseDown(button, true);
        }

        static void setMouseWheel(float y) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setMouseWheel(y);
        }

        static void setMouseWheelH(float x) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setMouseWheelH(x);
        }

        static void setMousePos(float x, float y) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setMousePos(x, y);
        }

        static void setMouseDelta(float dx, float dy) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setMouseDelta(dx, dy);
        }

        static void setWantCaptureMouse(boolean want) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().setWantCaptureMouse(want);
        }
    }
}
