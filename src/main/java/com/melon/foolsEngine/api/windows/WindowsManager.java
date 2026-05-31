package com.melon.foolsEngine.api.windows;

import java.util.List;

public interface WindowsManager {
    public Window createWindow();
    public void updateWindow(Window window,int vsyncMode);
    public List<Window> existWindows();
    public void destroyWindow(Window window,boolean recursive);
    public void managerTerminate();
}
