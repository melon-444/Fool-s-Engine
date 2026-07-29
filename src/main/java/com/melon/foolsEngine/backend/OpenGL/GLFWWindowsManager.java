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
package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.WindowResizedEvent;
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
            EventBus bus = EventBus.get("SystemBus");
            if (bus != null) bus.emit(new WindowResizedEvent(w, h));
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
