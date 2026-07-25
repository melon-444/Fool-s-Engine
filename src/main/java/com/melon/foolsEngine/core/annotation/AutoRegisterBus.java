package com.melon.foolsEngine.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class whose non-static {@link SubscribeEvent} methods should be
 * auto-registered on the named {@link com.melon.foolsEngine.core.events.EventBus}
 * when an instance is passed to {@code EventBus.addListener(instance)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoRegisterBus {
    /** The {@link com.melon.foolsEngine.core.events.EventBus} id to register on. */
    String id();
}
