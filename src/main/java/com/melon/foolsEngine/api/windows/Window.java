package com.melon.foolsEngine.api.windows;

import com.melon.foolsEngine.util.CursorMode;

import java.util.List;

/**
 * Represents a native window managed by the engine.
 * Supports lifecycle management, sizing, fullscreen toggling, parent-child hierarchy, and cursor mode control.
 */
public interface Window {
    /** @return the native window handle (GLFW window ID) */
    public long getID();
    /** @return the current window title */
    public String getTitle();
    /** Sets the window title bar text */
    public void setTitle(String title);
    /**
     * Destroys this window and releases native resources.
     * @param recursive if true, also destroys all child windows
     */
    public void destroy(boolean recursive);
    /** Makes the window visible */
    public void show();
    /** Hides the window */
    public void hide();
    /** @return whether the window is currently visible */
    public boolean isVisible();
    /** @return whether the window is resizable by the user */
    public boolean isResizable();
    /** Sets whether the user can resize the window */
    public void setResizable(boolean resizable);
    /** @return whether the window is in fullscreen mode */
    public boolean isFullscreen();
    /** Toggles fullscreen mode */
    public void setFullscreen(boolean fullscreen);
    /** Sets the window client area size in pixels */
    public void setSize(int width, int height);
    /** @return the current client area width */
    public int getWidth();
    /** @return the current client area height */
    public int getHeight();
    /** Swaps buffers and polls events for this frame */
    public void update();
    /** @return the parent window, or null */
    public Window getParent();
    /** Sets the parent window (for child window management) */
    public void setParent(Window parent);
    /** Removes the parent relationship */
    public void removeParent();
    /** @return an immutable snapshot of child windows */
    public List<Window> getChildren();
    /** Adds a child window */
    public void addChild(Window child);
    /** Removes a child window */
    public void removeChild(Window child);
    /** @return true if the window has been requested to close */
    public boolean shouldClose();
    /**
     * Sets the cursor mode for this window.
     * @param mode NORMAL (visible), HIDDEN (invisible but constrained), or DISABLED (unlimited virtual cursor)
     */
    public void setCursorMode(CursorMode mode);
    /** @return the current cursor mode */
    public CursorMode getCursorMode();
}
