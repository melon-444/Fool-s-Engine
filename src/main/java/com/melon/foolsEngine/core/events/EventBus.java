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

package com.melon.foolsEngine.core.events;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Double-buffered event bus with hierarchical dispatch and annotation-aware registration.
 * <p>
 * Each bus has a unique {@link #busId()}, stored in a static registry keyed by id.
 * Bus instances are created by {@link #create(String)} (or via the {@code @EventBus}
 * annotation at class-load time).
 * <p>
 * Events are queued per frame via {@link #emit(Event)} and dispatched in batch
 * by {@link #process()}. Listeners are registered as annotated methods on subscriber
 * classes — static methods via {@link #registerStaticSubscribers(Class)} (auto for
 * {@code @EventBusSubscriber} classes), instance methods via {@link #addListener(Object)}.
 */
public class EventBus {

    private static final Map<String, EventBus> BUSES = new ConcurrentHashMap<>();

    /** Returns the bus registered under {@code id}, or null. */
    public static EventBus get(String id) {
        return BUSES.get(id);
    }

    /**
     * Creates and registers a new bus under {@code id}.
     * @throws IllegalStateException if a bus with this id already exists.
     */
    public static EventBus create(String id) {
        if (BUSES.containsKey(id))
            throw new IllegalStateException("Duplicate EventBus id: " + id);
        EventBus bus = new EventBus(id);
        BUSES.put(id, bus);
        return bus;
    }

    // ── Instance ──

    private final String busId;
    private final Map<Class<?>, Map<Method, Object>> listeners = new ConcurrentHashMap<>();

    private final Queue<Event> queue0 = new ArrayDeque<>();
    private final Queue<Event> queue1 = new ArrayDeque<>();
    private boolean front;

    EventBus(String busId) {
        this.busId = busId;
    }

    public String busId() { return busId; }

    // ── Registration ──

    /**
     * Scans {@code clazz} for static {@code @SubscribeEvent} methods and registers them.
     * Called automatically for {@code @EventBusSubscriber} classes during class loading.
     */
    public void registerStaticSubscribers(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getAnnotation(com.melon.foolsEngine.core.annotation.SubscribeEvent.class) == null) continue;
            if (!Modifier.isStatic(m.getModifiers())) continue;
            validateSubscribeMethod(m);
            registerMethod(m, null);
        }
    }

    /** Registers all instance {@code @SubscribeEvent} methods on {@code subscriber}. */
    public void addListener(Object subscriber) {
        for (Method m : subscriber.getClass().getDeclaredMethods()) {
            if (m.getAnnotation(com.melon.foolsEngine.core.annotation.SubscribeEvent.class) == null) continue;
            if (Modifier.isStatic(m.getModifiers())) continue;
            validateSubscribeMethod(m);
            registerMethod(m, subscriber);
        }
    }

    /** Removes all annotated-method listeners belonging to {@code subscriber}. */
    public void removeListener(Object subscriber) {
        for (Map<Method, Object> map : listeners.values()) {
            map.values().removeIf(v -> v == subscriber);
        }
    }

    private void validateSubscribeMethod(Method m) {
        if (!Modifier.isPublic(m.getModifiers()))
            throw new IllegalArgumentException("@SubscribeEvent method must be public: " + m);
        if (m.getReturnType() != void.class)
            throw new IllegalArgumentException("@SubscribeEvent method must return void: " + m);
        Class<?>[] params = m.getParameterTypes();
        if (params.length != 1)
            throw new IllegalArgumentException("@SubscribeEvent method must have exactly 1 parameter: " + m);
        if (!Event.class.isAssignableFrom(params[0]))
            throw new IllegalArgumentException("@SubscribeEvent parameter must extend Event: " + m);
    }

    private void registerMethod(Method m, Object target) {
        Class<?> eventType = m.getParameterTypes()[0];
        listeners.computeIfAbsent(eventType, k -> new LinkedHashMap<>()).put(m, target);
    }

    // ── Frame lifecycle ──

    /** Queues an event for dispatch in the next {@link #process()} call. */
    public void emit(Event event) {
        (front ? queue1 : queue0).add(event);
    }

    /** Dispatches all queued events to registered listeners. */
    public void process() {
        Queue<Event> active = front ? queue0 : queue1;
        while (!active.isEmpty()) {
            dispatch(active.poll());
        }
        front = !front;
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void dispatch(T event) {
        Class<?> type = event.getClass();
        while (type != null) {
            Map<Method, Object> map = listeners.get(type);
            if (map != null) {
                for (var entry : map.entrySet()) {
                    try {
                        entry.getKey().invoke(entry.getValue(), event);
                    } catch (Exception e) {
                        throw new RuntimeException("Event dispatch failed for " + entry.getKey(), e);
                    }
                }
            }
            type = type.getSuperclass();
        }
    }
}
