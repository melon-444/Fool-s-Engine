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

package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.world.ComponentManager;
import com.melon.foolsEngine.util.Signature;
import com.melon.foolsEngine.util.SparseSet;

import java.util.HashSet;
import java.util.Set;

public abstract class System<Context> {

    public Set<Integer> entities = new HashSet<>();
    protected Set<Class<? extends Component>> requiredComponents = new HashSet<>();
    protected final FoolsEngine INSTANCE;
    protected Context context;

    public System(FoolsEngine engine) {
        this(engine,null);
    }
    public System(FoolsEngine engine,Context context) {
        this.INSTANCE = engine;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    @SuppressWarnings("unchecked")
    protected <T extends Component> SparseSet<T> getSparseSet(Class<T> clazz) {
        return (SparseSet<T>) INSTANCE.componentManager.getComponentMap().get(clazz);
    }

    public Signature genSignatureFromRequired(ComponentManager componentManager, int maxComponents) {
        Signature sig = new Signature(maxComponents);
        for (Class<? extends Component> componentType : requiredComponents) {
            sig.mix(componentManager.getComponentSignature(componentType));
        }
        return sig;
    }

    public Set<Class<? extends Component>> getRequiredComponents() {
        return requiredComponents;
    }

    /**
     * Execution priority — lower runs first. Default 0.
     * In order to choose the execute sequence in SystemScheduler.
     * Override to declare ordering: e.g. SceneCollector=0 → PassCollector=1 → HUD=100.
     * @return the priority level, lower first.
     */
    public int priority() {
        return 0;
    }

    /**
     * Pinned systems cannot be unregistered. Core collectors return true.
     */
    public boolean isPinned() {
        return false;
    }

    public abstract void update(float dt, Context ctx);
}
