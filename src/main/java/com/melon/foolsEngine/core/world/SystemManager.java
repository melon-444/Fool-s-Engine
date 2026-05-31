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
