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
