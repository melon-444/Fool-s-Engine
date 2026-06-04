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
