package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import org.lwjgl.opengl.GL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

//import org.lwjgl.glfw.GLFWErrorCallback; //Debug

/**
 * You need complete some necessary procedure below to create a window
 * createWindow() -> id
 * setWindowSize(id,wid,hei)
 * showWindow(id)
 */
class GLFWWindowsManager implements WindowsManager {

    private final ArrayList<Window> windows = new ArrayList<>();
    private final HashMap<Long, int[]> fullscreenWindowSize = new HashMap<>();

    @Override
    public Window createWindow() {
        return createWindow("",1,1,0,true,false,false);
    }

    public Window createWindow(String title,int width,int height,int vsyncMode,boolean resizeable,boolean fullscreen,boolean isVisible) {
        //GLFWErrorCallback.createPrint(System.err).set(); //Debug code
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW init failed");
        }
        //glfw settings
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        //create window
        //monitor null for not full screen,
        //share null for nothing

        long window_pointer = glfwCreateWindow(width,height,title, NULL,NULL);
        if (window_pointer == NULL) {
            throw new RuntimeException("Failed to create window");
        }
        glfwMakeContextCurrent(window_pointer);
        //GL init
        GL.createCapabilities();

        Window win = new GLWindow(window_pointer,title,vsyncMode,resizeable,fullscreen,isVisible,width,height);

        glfwSetFramebufferSizeCallback(window_pointer, (window, w, h) -> {
            win.setSize(w, h);
        });

        windows.add(win);
        return win;
    }

    @Override
    public void updateWindow(Window window, int vsyncMode) {
        /*update swap interval
         * 0 for no sync
         * 1 for Vsync
         * 2 and above for rated Vsync (as divisor) */
        if(window instanceof GLWindow win) {
            win.setIntervalMode(vsyncMode);
            win.update();
        }
        else window.update();
    }

    @Override
    public List<Window> existWindows() {
        return windows;
    }

    @Override
    public void destroyWindow(Window window,boolean recursive) {
        window.destroy(recursive);
        windows.remove(window);
    }

    @Override
    public void managerTerminate(){
        glfwTerminate();
    }


}
