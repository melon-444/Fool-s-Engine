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

import com.melon.foolsEngine.core.annotation.InstanceBusSubscriber;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
        Objects.requireNonNull(id, "id");

        EventBus bus = new EventBus(id);
        EventBus existing = BUSES.putIfAbsent(id, bus);

        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate EventBus id: " + id
            );
        }
        return bus;
    }

    // ── Instance ──

    private final String busId;
    private final Map<Class<?>, CopyOnWriteArrayList<RegisteredListener>> listeners =
            new ConcurrentHashMap<>();
    private final Set<IdentityKey> registeredInstances =
            ConcurrentHashMap.newKeySet();

    private final Queue<Event> queue0 = new ArrayDeque<>();
    private final Queue<Event> queue1 = new ArrayDeque<>();

    private final Object queueLock = new Object();
    private final Object processLock = new Object();
    /** Represents state of queue1 */
    private boolean Q1Write = false;

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

    /**
     * Registers all instance {@code @SubscribeEvent} methods on
     * {@code subscriber} (includes extended @SubscribeEvent methods)
     * onto the bus specified by the class's {@code @InstanceBusSubscriber} annotation.
     * Un-annotated classes default to {@code SystemBus}.
     * Idempotent: calling with the same instance again is a no-op.
     */
    public static void addListener(Object subscriber) {
        InstanceBusSubscriber ibs = subscriber.getClass().getAnnotation(InstanceBusSubscriber.class);
        String busId = ibs != null ? ibs.id() : "SystemBus";
        EventBus bus = get(busId);
        if (bus == null)
            throw new IllegalStateException("Bus not found: " + busId);
        bus.addListenerImpl(subscriber);
    }

    private void addListenerImpl(Object subscriber) {
        List<Method> methods = Arrays.stream(
                        subscriber.getClass().getMethods()
                )
                .filter(method ->
                        method.isAnnotationPresent(com.melon.foolsEngine.core.annotation.SubscribeEvent.class))
                .filter(method ->
                        !Modifier.isStatic(method.getModifiers()))
                .peek(this::validateSubscribeMethod)
                .toList();

        synchronized (this) {
            IdentityKey key = new IdentityKey(subscriber);

            if (!registeredInstances.add(key)) {
                return;
            }

            for (Method method : methods) {
                registerMethod(method, subscriber);
            }
        }
    }

    /** Removes all annotated-method listeners belonging to {@code subscriber}. */
    public void removeListener(Object subscriber) {
        Objects.requireNonNull(subscriber, "subscriber");

        synchronized (this) {
            registeredInstances.remove(new IdentityKey(subscriber));
            for (var listenerList : listeners.values()) {
                listenerList.removeIf(
                        listener -> listener.target() == subscriber
                );
            }
            listeners.entrySet().removeIf(
                    entry -> entry.getValue().isEmpty()
            );
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
        listeners.computeIfAbsent(
                eventType,
                ignored -> new CopyOnWriteArrayList<>()
        ).add(new RegisteredListener(m, target));
    }

    // ── Frame lifecycle ──

    /** Queues an event for dispatch in the next {@link #process()} call. */
    public void emit(Event event) {
        Objects.requireNonNull(event, "event");
        synchronized (queueLock) {
            (Q1Write ? queue1 : queue0).add(event);
        }
    }

    /** Dispatches this event instantly. */
    public void emitNow(Event event) {
        dispatch(event);
    }

    /** Dispatches all queued events to registered listeners. */
    public void process() {
        final Queue<Event> active;
        synchronized (queueLock) {
            active = Q1Write ? queue1 : queue0;
            Q1Write =!Q1Write;
        }
        while (!active.isEmpty()) {
            dispatch(active.poll());
        }

    }

    /**Dispatches events in all two queues untile all queues are empty*/
    public void flush(int maxRounds) {
        for (int round = 0; round < maxRounds; round++) {
            if (queue0.isEmpty() && queue1.isEmpty()) return;
            process();
        }
        throw new IllegalStateException(
                "EventBus flush exceeded " + maxRounds
                        + " rounds; listeners may be emitting recursively"
        );
    }
    
    private <T extends Event> void dispatch(T event) {
        Class<?> type = event.getClass();
        while (type != null) {
            CopyOnWriteArrayList<RegisteredListener> registeredListeners = listeners.get(type);
            if (registeredListeners != null) {
                for (var entry : registeredListeners) {
                    try {
                        entry.method.invoke(entry.target, event);
                    } catch (Exception e) {
                        throw new RuntimeException("Event dispatch failed for " + entry.method, e);
                    }
                }
            }
            type = type.getSuperclass();
        }
    }
    private record RegisteredListener(Method method, Object target) {}

    private static final class IdentityKey {

        private final Object value;
        private final int hash;

        private IdentityKey(Object value) {
            this.value = value;
            this.hash = System.identityHashCode(value);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IdentityKey key
                    && value == key.value;
        }
    }
}
