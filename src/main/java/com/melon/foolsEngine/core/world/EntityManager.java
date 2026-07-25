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

package com.melon.foolsEngine.core.world;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.ComponentAddedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.EntityCreatedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.EntityDestroyedEvent;
import com.melon.foolsEngine.util.Signature;
import com.melon.foolsEngine.util.SparseSet;

import java.util.Arrays;

public class EntityManager {

    private final FoolsEngine Instance;

    private int livingEntityCount = 0;

    public Signature[] getEntitySignatures() {
        if(livingEntityCount == 0) return new Signature[0];
        return Arrays.copyOfRange(signatures, 0, livingEntityCount-1);
    }

    private final Signature[] signatures;

    public EntityManager(FoolsEngine engineInstance) {
        this.Instance = engineInstance;
        signatures = new Signature[Instance.MAX_ENTITIES];
    }

    public int createEntity() {

        if(livingEntityCount >= Instance.MAX_ENTITIES)
            throw new RuntimeException("Too many entities");

        int id = livingEntityCount++;

        signatures[id] = new Signature(Instance.MAX_COMPONENTS);

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new EntityCreatedEvent(id));

        return id;
    }

    public void destroyEntity(int entity) {
        int lastIndex = livingEntityCount - 1;
        if (entity < lastIndex) {
            Instance.componentManager.clearComponentFromSet(entity, lastIndex);
            signatures[entity] = signatures[lastIndex];
            Instance.systemManager.entitySignatureChanged(entity, new Signature(Instance.MAX_COMPONENTS));
            signatures[lastIndex] = null;
        } else {
            Instance.componentManager.clearComponentFromSet(entity, entity);
            Instance.systemManager.entitySignatureChanged(entity, new Signature(Instance.MAX_COMPONENTS));
            signatures[entity] = null;
        }
        livingEntityCount = lastIndex;

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new EntityDestroyedEvent(entity));
    }

    public Signature getSignature(int entity) {
        return signatures[entity];
    }


    public <T extends Component> void bindComponent(int entityID, T component) {
        SparseSet<T> set = getSet(component.getClass());
        set.createComponent(entityID, component);
        updateSignature(entityID, component);

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new ComponentAddedEvent(entityID, component));
    }


    public <T extends Component> void setComponent(int entityID, T component) {
        SparseSet<T> set = getSet(component.getClass());
        set.setComponent(entityID, component);
        updateSignature(entityID, component);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(int entityID,Class<T> componentClass) {
        return (T) getSet(componentClass).get(entityID);
    }

    private <T extends Component> void updateSignature(int entityID, T component) {
        if(component != null)
            signatures[entityID] = signatures[entityID].mix(Instance.componentManager.getComponentSignature(component.getClass()));
        Instance.systemManager.entitySignatureChanged(entityID, signatures[entityID]);
    }


    @SuppressWarnings("unchecked")
    private <T extends Component> SparseSet<T> getSet(Class<? extends Component> type) {
        return (SparseSet<T>) Instance.componentManager.getComponentSparseSet(type);
    }
}
