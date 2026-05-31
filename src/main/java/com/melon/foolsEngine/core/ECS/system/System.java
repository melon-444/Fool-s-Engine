package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.world.ComponentManager;
import com.melon.foolsEngine.util.Signature;
import com.melon.foolsEngine.util.SparseSet;

import java.util.HashSet;
import java.util.Set;

public abstract class System {

    public Set<Integer> entities = new HashSet<>();
    protected Set<Class<? extends Component>> requiredComponents = new HashSet<>();
    protected final FoolsEngine INSTANCE;

    public System(FoolsEngine engine) {
        this.INSTANCE = engine;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Component> SparseSet<T> getSparseSet(Class<T> clazz) {
        return (SparseSet<T>) INSTANCE.componentManager.getComponentMap().get(clazz);
    };

    public Signature genSignatureFromRequired(ComponentManager componentManager, int maxComponents) {
        Signature sig = new Signature(maxComponents);
        for (Class<? extends Component> componentType : requiredComponents){
            sig.mix(componentManager.getComponentSignature(componentType));
        }
        return sig;
    }

    public Set<Class<? extends Component>> getRequiredComponents() {
        return  requiredComponents;
    }

    public abstract void update(long dt);

}