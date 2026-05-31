package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.windows.Window;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

class GLWindow implements Window {

    private final long id;
    private final List<Window> children = new ArrayList<>();
    private Window parent = null;
    private String title = "";
    private boolean isFullScreen = false;
    private boolean resizeable = true;
    private boolean isVisible = false;
    private int width = 0;
    private int height = 0;


    public void setIntervalMode(int intervalMode) {
        this.intervalMode = intervalMode;
    }

    private int intervalMode = 0;

    public GLWindow(long id,String title,int intervalMode,boolean resizeable,boolean isFullScreen,boolean isVisible,int width,int height) {
        this.id = id;
        this.title = title;
        this.intervalMode = intervalMode;
        this.resizeable = resizeable;
        this.width = width;
        this.height = height;
        this.isFullScreen = isFullScreen;
        this.isVisible = isVisible;
        if(isVisible) show();
        else hide();
        setResizable(resizeable);
        setSize(width, height);
        setFullscreen(isFullScreen);
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
        if(recursive)
            for(Window window : children) {
                window.destroy(true);
            }
        if(parent != null)
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
        if(fullscreen){
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(monitor);
            if (vidMode != null) {
                //setSize(vidMode.width(), vidMode.height());
                setSize(vidMode.width(), vidMode.height());
                glfwSetWindowMonitor(id, monitor, 0, 0, vidMode.width(), vidMode.height(), vidMode.refreshRate());
                isFullScreen = true;
                return;
            }
            throw new RuntimeException("Monitor don't have a usable video mode.");
        }
        else
            {
                setSize(width, height);
                glfwSetWindowMonitor(id, NULL, 100, 100, width, height, GLFW_DONT_CARE);
                isFullScreen = false;
            }

    }

    @Override
    public void setSize(int width, int height) {
        if(!resizeable)
            return;
        this.width = width;
        this.height = height;
        glfwSetWindowSize(id, width, height);
        //TODO：修改VeiwPort的设置位置
        glViewport(0, 0, width, height);
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
    public void update() {
        glfwMakeContextCurrent(id);
        glfwSwapInterval(intervalMode);
        glfwSwapBuffers(this.id);
        glfwPollEvents();
    }

    @Override
    public Window getParent() {
        return parent;
    }

    @Override
    public void setParent(Window parent) {
        this.parent = parent;
        if(this.parent != null && !this.parent.getChildren().contains(this))
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
}
