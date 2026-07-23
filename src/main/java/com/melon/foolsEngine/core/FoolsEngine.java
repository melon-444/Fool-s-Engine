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

package com.melon.foolsEngine.core;

import com.melon.foolsEngine.api.rendering.render.GraphicsContext;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.core.ECS.entity.EntityFactory;
import com.melon.foolsEngine.core.ECS.system.CameraCollector;
import com.melon.foolsEngine.core.ECS.system.LightCollector;
import com.melon.foolsEngine.core.ECS.system.RenderableCollector;
import com.melon.foolsEngine.core.world.*;
import com.melon.foolsEngine.util.logger.Logger;

public class FoolsEngine {
    public final EntityManager entityManager;
    public final EntityFactory entityFactory;
    public final ComponentManager componentManager;
    public final SystemManager systemManager;
    public final EntityFactory factory;
    public final RenderFrame frame;
    public final ServiceFactory serviceFactory;
    public final SystemScheduler systemScheduler;
    public final boolean isServer;


    public final Logger LOGGER = new Logger("SYSTEM");

    public final int MAX_ENTITIES;
    public final int MAX_COMPONENTS;

    public final boolean REVERSE_Z = true;

    public int width;
    public int height;
    public float aspect;

    public float FOV;
    public float Z_NEAR = 0.01f;
    public float Z_FAR = 1E10f;

    public Window mainWindow;

    FoolsEngine(int maxEntities, int maxComponents, int windowWidth, int windowHeight, boolean isServer) {
        this.isServer = isServer;
        this.serviceFactory = new ServiceFactory();
        this.MAX_ENTITIES = maxEntities;
        this.MAX_COMPONENTS = maxComponents;
        this.entityManager = new EntityManager(this);
        this.componentManager = new ComponentManager(this);
        this.systemManager = new SystemManager(this);
        this.factory = new EntityFactory(this);
        this.frame = isServer ? null : serviceFactory.getRenderFrame();
        this.entityFactory = new EntityFactory(this);
        this.width = windowWidth;
        this.height = windowHeight;


        if (!isServer) {
            mainWindow = serviceFactory.getWindowsManager().createWindow();
            mainWindow.setSize(windowWidth, windowHeight);
            systemManager.registerSystem(CameraCollector.class);
            systemManager.registerSystem(LightCollector.class);
            systemManager.registerSystem(RenderableCollector.class);
            frame.init();
            this.systemScheduler = new SystemScheduler(frame, (GraphicsContext) mainWindow);
        } else {
            this.systemScheduler = new SystemScheduler();
        }
    }

    public void updateSettings() {
        aspect = (float) width / (float) height;
        FOV = 40.0f;
    }
}
