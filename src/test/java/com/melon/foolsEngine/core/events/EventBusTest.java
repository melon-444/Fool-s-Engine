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
