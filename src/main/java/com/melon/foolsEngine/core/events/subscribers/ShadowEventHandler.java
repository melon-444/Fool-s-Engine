package com.melon.foolsEngine.core.events.subscribers;

import com.melon.foolsEngine.core.annotation.EventBusSubscriber;
import com.melon.foolsEngine.core.annotation.SubscribeEvent;
import com.melon.foolsEngine.core.events.builtInEvents.*;
import com.melon.foolsEngine.util.logger.Logger;

@EventBusSubscriber(id = "SystemBus")
public final class ShadowEventHandler {

    private static final Logger LOG = new Logger("ShadowEvents");

    private ShadowEventHandler() {}

    @SubscribeEvent
    public static void onLayerAllocated(ShadowLayerAllocatedEvent event) {
        LOG.trace("layer-alloc layer=%d", event.layer);
    }

    @SubscribeEvent
    public static void onLayerReleased(ShadowLayerReleasedEvent event) {
        LOG.trace("layer-release layer=%d", event.layer);
    }

    @SubscribeEvent
    public static void onPassPrepared(ShadowPassPreparedEvent event) {
        LOG.trace("pass-prepare light=%s layer=%d", event.light, event.context.layer());
    }
}
