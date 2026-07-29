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

import static org.junit.jupiter.api.Assertions.*;

import com.melon.foolsEngine.core.annotation.InstanceBusSubscriber;
import com.melon.foolsEngine.core.annotation.SubscribeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventBusTest {

    @BeforeEach
    void setup() {
        if (EventBus.get("SystemBus") == null) EventBus.create("SystemBus");
    }

    @InstanceBusSubscriber
    static class TestListener {
        int count;
        @SubscribeEvent
        public void on(TestEvent e) { count++; }
    }

    static class TestEvent extends Event {}

    @Test
    void registerAndDispatch() {
        TestListener sub = new TestListener();
        EventBus.addListener(sub);
        EventBus.get("SystemBus").emitNow(new TestEvent());
        assertEquals(1, sub.count);
    }

    @Test
    void addListenerIdempotent() {
        TestListener sub = new TestListener();
        EventBus.addListener(sub);
        EventBus.addListener(sub);
        EventBus.get("SystemBus").emitNow(new TestEvent());
        assertEquals(1, sub.count);
    }

    @Test
    void emitAndProcess() {
        TestListener sub = new TestListener();
        EventBus.addListener(sub);
        EventBus bus = EventBus.get("SystemBus");
        bus.emit(new TestEvent());
        bus.process();
        assertEquals(1, sub.count);
    }

    @Test
    void createAndGet() {
        String id = "b-" + System.nanoTime();
        EventBus.create(id);
        assertNotNull(EventBus.get(id));
    }

    @Test
    void duplicateIdThrows() {
        String id = "dup-" + System.nanoTime();
        EventBus.create(id);
        assertThrows(IllegalStateException.class, () -> EventBus.create(id));
    }
}
