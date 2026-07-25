package com.melon.foolsEngine.core.events.subscribers;

import com.melon.foolsEngine.core.annotation.EventBusSubscriber;
import com.melon.foolsEngine.core.annotation.SubscribeEvent;
import com.melon.foolsEngine.core.events.builtInEvents.WindowResizedEvent;
import com.melon.foolsEngine.core.world.ViewportState;

@EventBusSubscriber(id = "SystemBus")
public final class WindowEventHandler {

    private WindowEventHandler() {}

    @SubscribeEvent
    public static void onWindowResized(WindowResizedEvent event) {
        ViewportState.viewportW = event.width;
        ViewportState.viewportH = event.height;
    }
}
