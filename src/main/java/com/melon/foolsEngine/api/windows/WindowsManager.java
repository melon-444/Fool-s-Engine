package com.melon.foolsEngine.api.windows;

import java.util.List;

/**
 * Manages window creation and lifecycle.
 * Obtain an instance via {@code FoolsEngine.serviceFactory.getWindowsManager()}.
 */
public interface WindowsManager {
    /** Creates a new window with default settings */
    public Window createWindow();
    /** Updates the window's vsync mode (0 = off, 1 = on) */
    public void updateWindow(Window window, int vsyncMode);
    /** @return a snapshot of all currently existing windows */
    public List<Window> existWindows();
    /**
     * Destroys a window and releases its native resources.
     * @param recursive if true, also destroys all children
     */
    public void destroyWindow(Window window, boolean recursive);
    /** Terminates the window manager and all GLFW resources */
    public void managerTerminate();
}
