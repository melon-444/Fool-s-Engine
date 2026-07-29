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
package com.melon.foolsEngine.core.ECS.entity;

import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.pipeline.ShaderPass;
import com.melon.foolsEngine.core.ECS.basicComponents.CameraComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.LightComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.LightEnvComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderableComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderPassComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.ProjectionType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class EntityFactory {
    private final FoolsEngine Instance;

    public EntityFactory(FoolsEngine engine) {
        this.Instance = engine;
    }

    /** Create a model entity with Transform + Renderable. Rotation defaults to identity, scale to (1,1,1). */
    public int createModelEntity(Mesh mesh, Material material, Vector3f position) {
        return createModelEntity(mesh, material, position, null, null);
    }

    /** Create a model entity with Transform + Renderable. Rotation defaults to identity. */
    public int createModelEntity(Mesh mesh, Material material, Vector3f position, Vector3f scale) {
        return createModelEntity(mesh, material, position, null, scale);
    }

    /** Create a model entity with Transform + Renderable. */
    public int createModelEntity(Mesh mesh, Material material, Vector3f position, Quaternionf rotation, Vector3f scale) {
        final int entityID = Instance.entityManager.createEntity();

        if (scale == null) scale = new Vector3f(1, 1, 1);
        if (rotation == null) rotation = new Quaternionf();
        if (position == null) position = new Vector3f();

        Instance.entityManager.bindComponent(entityID, new TransformComponent(position, rotation, scale));

        if (mesh != null || material != null) {
            Instance.entityManager.bindComponent(entityID, new RenderableComponent(mesh, material));
        }

        return entityID;
    }

    /** Create a light entity with the given ECS Light component. */
    public int createLightEntity(LightComponent lightComp) {
        final int entityID = Instance.entityManager.createEntity();
        Instance.entityManager.bindComponent(entityID, lightComp);
        Instance.LOGGER.debug("Light Created,ID:%d,%s", entityID, lightComp);
        return entityID;
    }

    /**
     * Create an active camera entity and return its Transform for per-frame updates.
     * Consider it as a dot with a vector in the world(Input the normal transform to the return instead of its invert)
     * The Transform is bound to the entity — mutate it directly, then call markDirty().
     */
    public TransformComponent createCamera(Vector3f position) {
        final int entityID = Instance.entityManager.createEntity();
        TransformComponent transform = new TransformComponent(position);
        Instance.entityManager.bindComponent(entityID, transform);
        Instance.entityManager.bindComponent(entityID, new CameraComponent(Instance.FOV, Instance.Z_NEAR));
        return transform;
    }

    /** Create an orthographic camera entity. orthoSize is the vertical half-extent in world units. */
    public TransformComponent createOrthoCamera(Vector3f position, float orthoSize, float near, float far) {
        final int entityID = Instance.entityManager.createEntity();
        TransformComponent transform = new TransformComponent(position);
        Instance.entityManager.bindComponent(entityID, transform);
        Instance.entityManager.bindComponent(entityID,
                new CameraComponent(near, far, ProjectionType.ORTHOGRAPHIC, orthoSize));
        return transform;
    }

    /** Create a light environment entity and return its ID. Get the env via ComponentManager. */
    public int createLightEnvironment() {
        final int entityID = Instance.entityManager.createEntity();
        Instance.entityManager.bindComponent(entityID, new LightEnvComponent());
        return entityID;
    }

    /** Create an entity that adds a {@link ShaderPass} to the rendering pipeline. */
    public int createShaderPass(int order, ShaderPass pass) {
        final int entityID = Instance.entityManager.createEntity();
        Instance.entityManager.bindComponent(entityID, new RenderPassComponent(order, pass));
        return entityID;
    }
}
