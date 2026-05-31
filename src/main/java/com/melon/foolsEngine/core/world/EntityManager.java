package com.melon.foolsEngine.core.world;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;
import com.melon.foolsEngine.core.FoolsEngine;
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

        return id;
    }

    public void destroyEntity(int entity) {
        int lastIndex = livingEntityCount - 1;
        Instance.componentManager.clearComponentFromSet(entity, lastIndex);
        signatures[entity] = signatures[lastIndex];
        signatures[lastIndex] = null;
        livingEntityCount = lastIndex;
        updateSignature(entity,null);
    }

    public Signature getSignature(int entity) {
        return signatures[entity];
    }


    public <T extends Component> void bindComponent(int entityID, T component) {
        SparseSet<T> set = getSet(component.getClass());
        set.createComponent(entityID, component);
        updateSignature(entityID, component);
    }


    public <T extends Component> void setComponent(int entityID, T component) {
        SparseSet<T> set = getSet(component.getClass());
        set.setComponent(entityID, component);
        updateSignature(entityID, component);
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
