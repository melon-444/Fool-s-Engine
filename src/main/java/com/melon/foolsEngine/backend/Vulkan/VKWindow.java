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

import com.melon.foolsEngine.api.rendering.render.GraphicsContext;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.util.CursorMode;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

class VKWindow implements Window, GraphicsContext {

    private final long id;
    private final List<Window> children = new ArrayList<>();
    private Window parent = null;
    private String title = "";
    private boolean isFullScreen = false;
    private boolean resizeable = true;
    private boolean isVisible = false;
    private int width = 0;
    private int height = 0;
    private CursorMode cursorMode = CursorMode.NORMAL;
    private int intervalMode = 0;
    private long surface = 0;

    VKWindow(long id, String title, int intervalMode, boolean resizeable, boolean isFullScreen, boolean isVisible, int width, int height) {
        this.id = id;
        this.title = title;
        this.intervalMode = intervalMode;
        this.resizeable = resizeable;
        this.width = width;
        this.height = height;
        this.isFullScreen = isFullScreen;
        this.isVisible = isVisible;
        if (isVisible) show();
        else hide();
        setResizable(resizeable);
        setSize(width, height);
        setFullscreen(isFullScreen);
    }

    public long createSurface(VkInstance instance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            int result = GLFWVulkan.glfwCreateWindowSurface(instance, id, null, pSurface);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface: VkResult " + result);
            }
            surface = pSurface.get(0);
            return surface;
        }
    }

    public long getSurface() {
        return surface;
    }

    @Override
    public long getID() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        glfwSetWindowTitle(id, title);
        this.title = title;
    }

    @Override
    public void destroy(boolean recursive) {
        glfwDestroyWindow(id);
        if (recursive)
            for (Window window : children) {
                window.destroy(true);
            }
        if (parent != null)
            parent.getChildren().remove(this);
    }

    @Override
    public void show() {
        glfwShowWindow(id);
        this.isVisible = true;
    }

    @Override
    public void hide() {
        glfwHideWindow(id);
        this.isVisible = false;
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public boolean isResizable() {
        return resizeable;
    }

    @Override
    public void setResizable(boolean resizable) {
        this.resizeable = resizable;
    }

    @Override
    public boolean isFullscreen() {
        return isFullScreen;
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        if (fullscreen) {
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(monitor);
            if (vidMode != null) {
                setSize(vidMode.width(), vidMode.height());
                glfwSetWindowMonitor(id, monitor, 0, 0, vidMode.width(), vidMode.height(), vidMode.refreshRate());
                isFullScreen = true;
                return;
            }
            throw new RuntimeException("Monitor don't have a usable video mode.");
        } else {
            setSize(width, height);
            glfwSetWindowMonitor(id, NULL, 100, 100, width, height, GLFW_DONT_CARE);
            isFullScreen = false;
        }
    }

    @Override
    public void setSize(int width, int height) {
        if (!resizeable)
            return;
        this.width = width;
        this.height = height;
        glfwSetWindowSize(id, width, height);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void makeCurrent() {
    }

    @Override
    public void releaseCurrent() {
    }

    @Override
    public void swapBuffers() {
    }

    @Override
    public void pollEvents() {
        glfwPollEvents();
    }

    @Override
    public void update() {
        glfwPollEvents();
    }

    @Override
    public Window getParent() {
        return parent;
    }

    @Override
    public void setParent(Window parent) {
        this.parent = parent;
        if (this.parent != null && !this.parent.getChildren().contains(this))
            parent.addChild(this);
    }

    @Override
    public void removeParent() {
        this.parent.removeChild(this);
        this.parent = null;
    }

    @Override
    public List<Window> getChildren() {
        return children;
    }

    @Override
    public void addChild(Window child) {
        children.add(child);
        child.setParent(this);
    }

    @Override
    public void removeChild(Window child) {
        children.remove(child);
        child.setParent(null);
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(id);
    }

    @Override
    public long nativeHandle() {
        return id;
    }

    @Override
    public void setCursorMode(CursorMode mode) {
        this.cursorMode = mode;
        int glfwMode = switch (mode) {
            case NORMAL -> GLFW_CURSOR_NORMAL;
            case HIDDEN -> GLFW_CURSOR_HIDDEN;
            case DISABLED -> GLFW_CURSOR_DISABLED;
        };
        glfwSetInputMode(id, GLFW_CURSOR, glfwMode);
    }

    @Override
    public CursorMode getCursorMode() {
        return cursorMode;
    }

    void setIntervalMode(int intervalMode) {
        this.intervalMode = intervalMode;
    }
}
