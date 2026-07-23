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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class SparseSet<Component extends com.melon.foolsEngine.core.ECS.basicComponents.Component> implements Iterable<Component> {

    private final int[] sparseArray;
    private final int[] dense_entity;
    private final Component[] dense_component;
    private int size;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @SuppressWarnings("unchecked")
    public SparseSet(int size, Class<Component> componentType) {
        if (size <= 0) throw new IllegalArgumentException("Size must be positive");
        sparseArray = new int[size];
        dense_entity = new int[size];
        Object tmp = Array.newInstance(componentType, size);
        dense_component = (Component[]) tmp;

        Arrays.fill(sparseArray, -1);
        Arrays.fill(dense_entity, -1);
        Arrays.fill(dense_component, null);
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Component get(int entityID) {
        lock.readLock().lock();
        try {
            if (!existsUnsafe(entityID)) return null;
            return dense_component[sparseArray[entityID]];
        } finally {
            lock.readLock().unlock();
        }
    }

    public Component getComponent(int entityID) {
        return get(entityID);
    }

    public int getEntity(int componentIndex) {
        lock.readLock().lock();
        try {
            return dense_entity[componentIndex];
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists(int entityID) {
        lock.readLock().lock();
        try {
            return existsUnsafe(entityID);
        } finally {
            lock.readLock().unlock();
        }
    }

    private boolean existsUnsafe(int entityID) {
        int index = sparseArray[entityID];
        return index != -1 && dense_entity[index] == entityID;
    }

    public List<Component> snapshot() {
        lock.readLock().lock();
        try {
            List<Component> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(dense_component[i]);
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setComponent(int entityID, Component component) {
        lock.writeLock().lock();
        try {
            if (existsUnsafe(entityID))
                dense_component[sparseArray[entityID]] = component;
            else
                throw new IllegalArgumentException("Entity " + entityID + " does not exist");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void createComponent(int entityID, Component component) {
        lock.writeLock().lock();
        try {
            sparseArray[entityID] = size;
            dense_entity[size] = entityID;
            dense_component[size] = component;
            size++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteComponent(int entityID) {
        lock.writeLock().lock();
        try {
            if (!existsUnsafe(entityID)) return;

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
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Component[] getComponentArray() {
        return dense_component;
    }

    @Override
    public Iterator<Component> iterator() {
        return new Iterator<>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Component next() {
                return dense_component[index++];
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
