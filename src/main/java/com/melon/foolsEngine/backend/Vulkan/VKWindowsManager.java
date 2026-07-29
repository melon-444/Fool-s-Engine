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
package com.melon.foolsEngine.backend.Vulkan;

import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import org.lwjgl.glfw.GLFWVulkan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

class VKWindowsManager implements WindowsManager {

    private final ArrayList<Window> windows = new ArrayList<>();
    private final HashMap<Long, int[]> fullscreenWindowSize = new HashMap<>();
    private final VKRenderFrame renderFrame;

    VKWindowsManager(VKRenderFrame renderFrame) {
        this.renderFrame = renderFrame;
    }

    @Override
    public Window createWindow() {
        return createWindow("", 1, 1, 0, true, false, false);
    }

    public Window createWindow(String title, int width, int height, int vsyncMode, boolean resizeable, boolean fullscreen, boolean isVisible) {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }

        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new IllegalStateException("Vulkan is not supported on this system");
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        long window_pointer = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window_pointer == NULL) {
            throw new RuntimeException("Failed to create window");
        }

        VKWindow win = new VKWindow(window_pointer, title, vsyncMode, resizeable, fullscreen, isVisible, width, height);

        glfwSetFramebufferSizeCallback(window_pointer, (window, w, h) -> {
            win.setSize(w, h);
        });

        windows.add(win);
        renderFrame.setWindow(win);
        return win;
    }

    @Override
    public void updateWindow(Window window, int vsyncMode) {
        if (window instanceof VKWindow win) {
            win.setIntervalMode(vsyncMode);
            win.update();
        } else window.update();
    }

    @Override
    public List<Window> existWindows() {
        return windows;
    }

    @Override
    public void destroyWindow(Window window, boolean recursive) {
        window.destroy(recursive);
        windows.remove(window);
    }

    @Override
    public void managerTerminate() {
        glfwTerminate();
    }
}
