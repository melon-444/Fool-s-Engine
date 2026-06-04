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

package com.melon.foolsEngine.util;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

public class SparseSet<Component extends com.melon.foolsEngine.core.ECS.basicComponents.Component> implements Iterable<Component> {
    private final int[] sparseArray,
          dense_entity;
    private final Component[] dense_component;
    private int size = 0;

    @SuppressWarnings("unchecked")
    public SparseSet(int size,Class<Component> componentType) {
        if(size <= 0) throw new IllegalArgumentException("Size must be positive");
        sparseArray = new int[size];
        dense_entity = new int[size];
        Object tmp =  Array.newInstance(componentType, size);

        dense_component = (Component[])tmp;

        //init -1 refers to null
        Arrays.fill(sparseArray,-1);
        Arrays.fill(dense_entity,-1);
        Arrays.fill(dense_component,null);
    }

    public int getSize(){
        return size;
    }

    private void setComponentUnchecked(int entityID, Component component) {
        dense_component[sparseArray[entityID]] =  component;
    }

    public void setComponent(int entityID, Component component) {
        if(exists(entityID))
            setComponentUnchecked(entityID,component);
        else
            throw new IllegalArgumentException("Entity "+entityID+" does not exist");
    }

    public Component getComponent(int entityID) {
        if(!exists(entityID)) return null;
        return dense_component[sparseArray[entityID]];
    }

    public int getEntity(int componentIndex) {
        return dense_entity[componentIndex];
    }

    public void createComponent(int entityID, Component component) {
        sparseArray[entityID] = size;
        dense_entity[size] = entityID;
        dense_component[size] = component;
        size++;
    }


    public void deleteComponent(int entityID) {
        if(!exists(entityID)) return;

        int compIndex = sparseArray[entityID];
        int lastIndex = size - 1;

        int lastEntity = dense_entity[lastIndex];

        dense_component[compIndex] = dense_component[lastIndex];
        dense_entity[compIndex] = lastEntity;

        sparseArray[lastEntity] = compIndex;

        dense_component[lastIndex] = null;
        dense_entity[lastIndex] = -1;
        sparseArray[entityID] = -1;

        size--;
    }

    private boolean componentExists(int entityID) {
        return dense_entity[sparseArray[entityID]] != -1;
    }

    public Component[] getComponentArray() {
        return dense_component;
    }

    private boolean exists(int entityID) {
        int index = sparseArray[entityID];
        return index != -1 && dense_entity[index] == entityID;
    }


    @Override
    public Iterator<Component> iterator() {
        return new Iterator<Component>() {

            int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Component next() {
                return dense_component[sparseArray[index++]];
            }

            @Override
            public void remove() {
                Iterator.super.remove();
            }

            @Override
            public void forEachRemaining(Consumer<? super Component> action) {
                Iterator.super.forEachRemaining(action);
            }


        };
    }
}
