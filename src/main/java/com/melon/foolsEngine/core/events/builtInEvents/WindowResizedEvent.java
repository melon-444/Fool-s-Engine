package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.events.Event;

/** Fired when the window framebuffer size changes. */
public class WindowResizedEvent extends Event {
    public final int width;
    public final int height;

    public WindowResizedEvent(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
