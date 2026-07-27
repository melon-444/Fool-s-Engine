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
import com.melon.foolsEngine.core.ECS.system.ClientSystem;
import com.melon.foolsEngine.core.ECS.system.ServerSystem;
import com.melon.foolsEngine.core.ECS.system.System;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.ShadowPassPreparedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.SystemRegisteredEvent;
import com.melon.foolsEngine.util.Signature;
import com.melon.foolsEngine.util.logger.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SystemManager {

    private final Logger LOG = new Logger("SysMgr");

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

    public <T extends System<?>> void registerSystem(Class<T> systemClass) {
        try {
            LOG.debug("Registering system %s", systemClass.getSimpleName());
            T system = null;
            if (ClientSystem.class.isAssignableFrom(systemClass))
                system = systemClass.getDeclaredConstructor(FoolsEngine.class).newInstance(Instance);
            else if (ServerSystem.class.isAssignableFrom(systemClass))
                system = systemClass.getDeclaredConstructor(FoolsEngine.class, Object.class).newInstance(Instance, null);

            systems.put(systemClass, system);
            LOG.debug("Registered system: %s", systemClass.getSimpleName());
            Set<Class<? extends Component>> reqComps = system.getRequiredComponents();
            for (Class<? extends Component> klass : reqComps) {
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

            EventBus bus = EventBus.get("SystemBus");
            if (bus != null) bus.emit(new SystemRegisteredEvent(this));

        } catch (Exception e) {
            LOG.error("Failed to register system %s: %s", systemClass.getSimpleName(), e.toString());
            throw new RuntimeException(e);
        }
    }

    private <T extends System> void setSignature(Class<T> systemClass, Signature signature) {
        signatures.put(systemClass, signature);
    }

    public void entitySignatureChanged(int entity, Signature entitySignature) {
        if (entitySignature==null) throw new NullPointerException("entitySignature is null");
        for (var systemClasses : systems.keySet()) {
            System system = systems.get(systemClasses);
            Signature systemSig = signatures.get(systemClasses);
            if (entitySignature.matches(systemSig))
                system.entities.add(entity);
            else
                system.entities.remove(entity);
        }

    }
}
