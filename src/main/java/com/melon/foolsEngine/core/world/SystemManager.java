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
import com.melon.foolsEngine.core.ECS.system.System;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.Signature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SystemManager {

    public HashMap<Class<? extends System>, System> getRegisteredSystems() {
        return systems;
    }

    private final HashMap<Class<? extends System>, System> systems = new HashMap<>();

    private final HashMap<Class<? extends Component>, Set<Class<? extends System>>> receiveComponent = new HashMap<>();

    public HashMap<Class<? extends System>, Signature> getSystemSignatures() {
        return signatures;
    }

    private final HashMap<Class<? extends System>, Signature> signatures = new HashMap<>();

    private final FoolsEngine Instance;

    public SystemManager(FoolsEngine engineInstance) {
        Instance = engineInstance;
    }

    public <T extends System> T registerSystem(Class<T> systemClass) {

        try {

            T system = systemClass.getDeclaredConstructor(FoolsEngine.class).newInstance(Instance);

            systems.put(systemClass, system);
            for (var klass : system.getRequiredComponents()) {
                if (!receiveComponent.containsKey(klass)) {
                    Set<Class<? extends System>> components = new HashSet<>();
                    components.add(systemClass);
                    receiveComponent.put(klass, components);
                } else {
                    receiveComponent.get(klass).add(systemClass);
                }
            }

            setSignature(system.getClass(), system.genSignatureFromRequired(Instance.componentManager, Instance.MAX_COMPONENTS));
            Signature[] entities = Instance.entityManager.getEntitySignatures();
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] != null) {
                        entitySignatureChanged(i, entities[i]);
                }
            }

            if(Instance.scheduler!=null)Instance.scheduler.checkRegisteredSystem();

            return system;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends System> void setSignature(Class<T> systemClass, Signature signature) {
        signatures.put(systemClass, signature);
    }

    public void entitySignatureChanged(int entity, Signature entitySignature) {

        for (var systemClasses : systems.keySet()) {
            System system = systems.get(systemClasses);
            Signature systemSig = signatures.get(systemClasses);
            if (systemSig.includes(entitySignature))
                system.entities.add(entity);
            else
                system.entities.remove(entity);
        }

    }
}
