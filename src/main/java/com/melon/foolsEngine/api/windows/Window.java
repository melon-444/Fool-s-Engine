package com.melon.foolsEngine.api.windows;

import java.util.List;

public interface Window {
    public long getID();
    public String getTitle();
    public void setTitle(String title);
    public void destroy(boolean recursive);
    public void show();
    public void hide();
    public boolean isVisible();
    public boolean isResizable();
    public void setResizable(boolean resizable);
    public boolean isFullscreen();
    public void setFullscreen(boolean fullscreen);
    public void setSize(int width, int height);
    public int getWidth();
    public int getHeight();
    public void update();
    public Window getParent();
    public void setParent(Window parent);
    public void removeParent();
    public List<Window> getChildren();
    public void addChild(Window child);
    public void removeChild(Window child);
    public boolean shouldClose();
}
