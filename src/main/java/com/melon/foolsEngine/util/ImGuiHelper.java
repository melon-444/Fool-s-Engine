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

package com.melon.foolsEngine.util;

import imgui.flag.ImGuiKey;

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

    public static void addInputCharacter(int codepoint) {
        if (AVAILABLE) ImGuiInternal.addInputCharacter(codepoint);
    }

    public static void setKeyBackspace(boolean down) { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.Backspace, down); }
    public static void setKeyDelete(boolean down)    { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.Delete, down); }
    public static void setKeyEnter(boolean down)     { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.Enter, down); }
    public static void setKeyTab(boolean down)       { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.Tab, down); }
    public static void setKeyEscape(boolean down)    { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.Escape, down); }
    public static void setKeyLeft(boolean down)      { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.LeftArrow, down); }
    public static void setKeyRight(boolean down)     { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.RightArrow, down); }
    public static void setKeyUp(boolean down)        { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.UpArrow, down); }
    public static void setKeyDown(boolean down)      { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.DownArrow, down); }
    public static void setKeyHome(boolean down)      { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.Home, down); }
    public static void setKeyEnd(boolean down)       { if (AVAILABLE) ImGuiInternal.setKeyEvent(imgui.flag.ImGuiKey.End, down); }
    public static void setKeySpace(boolean down)     { if (AVAILABLE) ImGuiInternal.setKeyEvent(ImGuiKey.Space, down);}
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

        static void addInputCharacter(int codepoint) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().addInputCharacter(codepoint);
        }

        static void setKeyEvent(int imKey, boolean down) {
            if (imgui.ImGui.getCurrentContext().ptr != 0)
                imgui.ImGui.getIO().addKeyEvent(imKey, down);
        }
    }
}
