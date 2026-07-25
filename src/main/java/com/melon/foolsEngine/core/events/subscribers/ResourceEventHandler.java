package com.melon.foolsEngine.core.events.subscribers;

import com.melon.foolsEngine.core.annotation.EventBusSubscriber;
import com.melon.foolsEngine.core.annotation.SubscribeEvent;
import com.melon.foolsEngine.core.events.builtInEvents.*;
import com.melon.foolsEngine.util.logger.Logger;

@EventBusSubscriber(id = "SystemBus")
public final class ResourceEventHandler {

    private static final Logger LOG = new Logger("ResourceEvents");

    private ResourceEventHandler() {}

    @SubscribeEvent
    public static void onTextureLoaded(TextureLoadedEvent event) {
        LOG.trace("tex-loaded  id=%s", event.texture);
    }

    @SubscribeEvent
    public static void onTextureDestroyed(TextureDestroyedEvent event) {
        LOG.trace("tex-destroy id=%s", event.texture);
    }

    @SubscribeEvent
    public static void onShaderLoaded(ShaderLoadedEvent event) {
        LOG.trace("shader-loaded  id=%s", event.shader);
    }

    @SubscribeEvent
    public static void onShaderDestroyed(ShaderDestroyedEvent event) {
        LOG.trace("shader-destroy id=%s", event.shader);
    }

    @SubscribeEvent
    public static void onMeshUploaded(MeshUploadedEvent event) {
        LOG.trace("mesh-upload  id=%s", event.mesh);
    }

    @SubscribeEvent
    public static void onMeshDestroyed(MeshDestroyedEvent event) {
        LOG.trace("mesh-destroy id=%s", event.mesh);
    }
}
