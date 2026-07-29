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

import com.melon.foolsEngine.core.ECS.basicComponents.*;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.ComponentRemovedEvent;
import com.melon.foolsEngine.util.Signature;
import com.melon.foolsEngine.util.SparseSet;

import java.util.HashMap;

public class ComponentManager {
    private final HashMap<Class<? extends Component>, Signature> componentSignature = new HashMap<>();

    private final HashMap<Class<? extends Component>, SparseSet<? extends Component>> componentSparseSetMap = new HashMap<>();

    private int nextComponentType = 0;

    private final FoolsEngine Instance;

    public ComponentManager(FoolsEngine engineInstance) {

        this.Instance = engineInstance;
        //register basic components
        registerComponent(TransformComponent.class);
        registerComponent(CameraComponent.class);
        registerComponent(LightComponent.class);
        registerComponent(RenderableComponent.class);
        registerComponent(LightEnvComponent.class);
        registerComponent(TextureManagerComponent.class);
        registerComponent(MaterialComponent.class);
        registerComponent(RenderContextComponent.class);
        registerComponent(RenderPassComponent.class);
    }

    public void registerComponent(Class<? extends Component> componentClass) {
        //check if component exists
        if(checkComponentType(componentClass)) return;

        Signature signature = new Signature(Instance.MAX_COMPONENTS);
        signature.set(nextComponentType);

        componentSignature.put(componentClass, signature);

        componentSparseSetMap.put(componentClass, new SparseSet<>(Instance.MAX_ENTITIES, componentClass));

        nextComponentType++;
    }

    private boolean checkComponentType(Class<? extends Component> componentClass) {
        return componentSignature.containsKey(componentClass);
    }

    public Signature getComponentSignature(Class<? extends Component> componentClass) {
        return componentSignature.get(componentClass);
    }

    /**
     * Swaps the component data of the last entity into the destroyed entity's slot,
     * then properly deletes the last entity's entry. Respects cases where either
     * entity may not have the component type.
     *
     * @param entityID the destroyed entity
     * @param lastIndex the last entity being compacted
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void clearComponentFromSet(int entityID, int lastIndex) {
        EventBus bus = EventBus.get("SystemBus");

        if (entityID == lastIndex) {
            for (var set : componentSparseSetMap.values()) {
                if (set.exists(entityID)) {
                    if(bus != null) bus.emit(new ComponentRemovedEvent(entityID,set.get(entityID)));
                    set.deleteComponent(entityID);
                }
            }
            return;
        }
        for (var set : componentSparseSetMap.values()) {
            SparseSet raw = set;
            boolean a = raw.exists(entityID);
            boolean b = raw.exists(lastIndex);
            Instance.LOGGER.trace("raw.exists(entityID=%d):%b, raw.exists(lastIndex=%d):%b",entityID,a,lastIndex,b);
            if (a && b) {
                if(bus != null) bus.emit(new ComponentRemovedEvent(entityID,set.get(entityID)));
                raw.setComponent(entityID, raw.getComponent(lastIndex));
                raw.deleteComponent(lastIndex);
            } else if (a) {
                if(bus != null) bus.emit(new ComponentRemovedEvent(entityID,set.get(entityID)));
                raw.deleteComponent(entityID);
            } else if (b) {
                raw.createComponent(entityID, raw.getComponent(lastIndex));
                raw.deleteComponent(lastIndex);
            }
        }
    }

    public <T extends Component> SparseSet<T> getComponentSparseSet(Class<T> componentClass) {
        @SuppressWarnings("unchecked")
        SparseSet<T> tSparseSet = (SparseSet<T>) componentSparseSetMap.get(componentClass);
        return tSparseSet;
    }

    public HashMap<Class<? extends Component>, SparseSet<? extends Component>> getComponentMap() {
        return componentSparseSetMap;
    }
}