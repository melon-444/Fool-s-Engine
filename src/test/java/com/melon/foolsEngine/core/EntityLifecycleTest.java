package com.melon.foolsEngine.core;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;
import com.melon.foolsEngine.core.ECS.system.ServerSystem;
import com.melon.foolsEngine.core.events.EventBus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityLifecycleTest {
    public static final class Marker extends Component {}

    public static final class MarkerSystem extends ServerSystem<Void> {
        public MarkerSystem(FoolsEngine engine) {
            super(engine, null);
            requiredComponents.add(Marker.class);
        }
    }

    public static final class AllEntities extends ServerSystem<Void> {
        public AllEntities(FoolsEngine engine) {
            super(engine, null);
        }
    }

    @Test
    void registrationAndCompactionKeepComponentAndSystemMembershipConsistent() {
        var bus = EventBus.get("SystemBus");
        if (bus == null) bus = EventBus.create("SystemBus");
        var engine = new FoolsEngine(8, 32, 1, 1, true, null);
        try {
            var entities = engine.entityManager;
            var systems = engine.systemManager;
            assertThrows(IllegalArgumentException.class, () -> entities.destroyEntity(0));
            engine.componentManager.registerComponent(Marker.class);
            int first = entities.createEntity();
            int middle = entities.createEntity();
            int last = entities.createEntity();
            var marker = new Marker();
            entities.bindComponent(last, marker);

            var markerSystem = systems.registerSystem(MarkerSystem.class);
            var allEntities = systems.registerSystem(AllEntities.class);
            assertEquals(Set.of(last), markerSystem.entities);
            assertEquals(Set.of(first, middle, last), allEntities.entities);
            assertThrows(IllegalStateException.class, () -> systems.registerSystem(MarkerSystem.class));
            assertSame(markerSystem, systems.getRegisteredSystems().get(MarkerSystem.class));
            assertThrows(IllegalArgumentException.class, () -> entities.bindComponent(last, new Marker()));
            var components = engine.componentManager.getComponentSparseSet(Marker.class);
            assertEquals(1, components.getSize());
            assertSame(marker, entities.getComponent(last, Marker.class));

            entities.destroyEntity(middle);
            assertEquals(Set.of(middle), markerSystem.entities);
            assertEquals(Set.of(first, middle), allEntities.entities);
            assertSame(marker, entities.getComponent(middle, Marker.class));
            assertEquals(1, components.getSize());
            assertThrows(IllegalArgumentException.class, () -> entities.destroyEntity(last));
            assertThrows(IllegalArgumentException.class, () -> entities.destroyEntity(-1));
            entities.destroyEntity(middle);
            assertTrue(markerSystem.entities.isEmpty());
            assertEquals(Set.of(first), allEntities.entities);
            assertEquals(0, components.getSize());
        } finally {
            bus.removeListener(engine.systemScheduler);
            engine.systemScheduler.shutdown();
        }
    }
}
