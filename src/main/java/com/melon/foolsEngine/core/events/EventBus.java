package com.melon.foolsEngine.core.events;

import java.util.*;
import java.util.function.Consumer;

public class EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();
    private final Queue<Event> eventQueue0 = new ArrayDeque<>();
    private final Queue<Event> eventQueue1 = new ArrayDeque<>();
    private boolean frameQueueCurrent = false;//false = 0 true = 1

    public <T extends Event> void subscribe(Class<T> type, Consumer<T> handler) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>())
                .add(handler);
    }

    public void emit(Event event) {
        if(!frameQueueCurrent)
            eventQueue0.add(event);
        else
            eventQueue1.add(event);
    }

    public void process() {
        if(!frameQueueCurrent)
            while (!eventQueue0.isEmpty()) {
            Event event = eventQueue0.poll();
            dispatch(event);
        }
        else
            while (!eventQueue1.isEmpty()) {
            Event event = eventQueue1.poll();
            dispatch(event);
        }
        frameQueueCurrent = !frameQueueCurrent;
    }


    private <T extends Event> void dispatch(T event) {
        Class<?> type = event.getClass();
        while(type!=null){
            List<Consumer<?>> handlers = listeners.get(type);
            for (Consumer<?> handler : handlers) {
                @SuppressWarnings("unchecked")
                Consumer<T> hand = ((Consumer<T>) handler);
                hand.accept(event);
            }
            type=type.getSuperclass();
        }
    }
}