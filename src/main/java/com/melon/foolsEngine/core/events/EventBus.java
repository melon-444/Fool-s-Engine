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