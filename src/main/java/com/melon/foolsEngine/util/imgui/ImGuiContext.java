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
