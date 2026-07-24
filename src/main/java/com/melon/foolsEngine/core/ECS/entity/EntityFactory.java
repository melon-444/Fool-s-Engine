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
import com.melon.foolsEngine.core.ECS.basicComponents.CameraComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.Light;
import com.melon.foolsEngine.core.ECS.basicComponents.Renderable;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
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

        Instance.entityManager.bindComponent(entityID, new Transform(position, rotation, scale));

        if (mesh != null || material != null) {
            Instance.entityManager.bindComponent(entityID, new Renderable(mesh, material));
        }

        return entityID;
    }

    /** Create a light entity with the given ECS Light component. */
    public int createLightEntity(Light light) {
        final int entityID = Instance.entityManager.createEntity();
        Instance.entityManager.bindComponent(entityID, new Transform(light.position != null ? light.position : new Vector3f()));
        Instance.entityManager.bindComponent(entityID, light);
        return entityID;
    }

    /**
     * Create an active camera entity and return its Transform for per-frame updates.
     * Consider it as a dot with a vector in the world(Input the normal transform to the return instead of its invert)
     * The Transform is bound to the entity — mutate it directly, then call markDirty().
     */
    public Transform createCamera(Vector3f position) {
        final int entityID = Instance.entityManager.createEntity();
        Transform transform = new Transform(position);
        Instance.entityManager.bindComponent(entityID, transform);
        Instance.entityManager.bindComponent(entityID, new CameraComponent(Instance.FOV, Instance.Z_NEAR));
        return transform;
    }

    /**
     * @deprecated Use {@link #createCamera(Vector3f)} for ECS-managed cameras.
     */
    @Deprecated
    public int createCamera(float yawDeg, float pitchDeg, Vector3f position) {
        return createCamera(yawDeg, pitchDeg, position, true);
    }

    @Deprecated
    public int createCamera(float yawDeg, float pitchDeg, Vector3f position, boolean activate) {
        final int entityID = Instance.entityManager.createEntity();
        Quaternionf orientation = orientationFromYawPitch(yawDeg, pitchDeg);
        Instance.entityManager.bindComponent(entityID, new CameraComponent(Instance.FOV, Instance.Z_NEAR));
        Instance.entityManager.bindComponent(entityID, new Transform(position, orientation));
        return entityID;
    }

    private Quaternionf orientationFromYawPitch(float yawDeg, float pitchDeg) {
        Quaternionf orientation = new Quaternionf();
        orientation.rotateY((float) Math.toRadians(yawDeg));
        Vector3f right = new Vector3f(1, 0, 0);
        orientation.transform(right);
        orientation.rotateAxis((float) Math.toRadians(pitchDeg), right.x, right.y, right.z);
        return orientation;
    }
}
