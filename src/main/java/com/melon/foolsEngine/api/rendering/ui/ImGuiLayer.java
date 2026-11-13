package com.melon.foolsEngine.api.rendering.ui;

/** Optional Dear ImGui integration. Use on the render thread with the window context current. */
public interface ImGuiLayer extends AutoCloseable {
    void beginFrame();

    void endFrame();

    @Override
    void close();
}
