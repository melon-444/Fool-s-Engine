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
        registerComponent(Transform.class);
        registerComponent(CameraComponent.class);
        registerComponent(Light.class);
        registerComponent(Renderable.class);
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
     * Callback function
     *
     * @param entityID the destroyed entity
     */
    public void clearComponentFromSet(int entityID, int lastIndex) {
        for (var set : componentSparseSetMap.values()) {
            if(set.getComponent(entityID)!=null)
                copyHelper(set, entityID, lastIndex);
        }
    }

    private <T extends Component> void copyHelper(SparseSet<T> tSparseSet, int i, int j) {
        tSparseSet.setComponent(i, tSparseSet.getComponent(j));
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