package com.melon.foolsEngine.core;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.core.ECS.entity.EntityFactory;
import com.melon.foolsEngine.core.world.*;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FoolsEngine {
    public final EntityManager entityManager;
    public final EntityFactory entityFactory;
    public final ComponentManager componentManager;
    public final SystemManager systemManager;
    public final EntityFactory factory;
    public final SystemScheduler scheduler;
    public final RenderFrame frame;
    public final ServiceFactory serviceFactory;

    public final int MAX_ENTITIES;
    public final int MAX_COMPONENTS;

    public final boolean REVERSE_Z = true;
    //always true

    public int width;
    public int height;

    public float aspect;

    public float FOV;
    public float Z_NEAR = 0.01f;
    public float Z_FAR = 1E10f;

    public Window mainWindow;

    private final Thread renderThread;
    private final Thread mainGameThread;
    private final ThreadPoolExecutor workerGroup;


    private FoolsEngine(int maxEntities, int maxComponents,int WindowWidth,int WindowHeight) {
        this.serviceFactory = new ServiceFactory();
        this.MAX_ENTITIES = maxEntities;
        this.MAX_COMPONENTS = maxComponents;
        this.entityManager = new EntityManager(this);
        this.componentManager = new ComponentManager(this);
        this.systemManager = new  SystemManager(this);
        this.factory = new EntityFactory(this);
        this.scheduler = new SystemScheduler(this);
        this.frame = serviceFactory.getRenderFrame();
        this.entityFactory = new EntityFactory(this);
        this.width = WindowWidth;
        this.height = WindowHeight;

        mainWindow = serviceFactory.getWindowsManager().createWindow();
        mainWindow.setSize(WindowWidth, WindowHeight);

        renderThread = new Thread(() -> {});
        mainGameThread = Thread.currentThread();
        workerGroup = new ThreadPoolExecutor(MAX_ENTITIES, MAX_ENTITIES, 100L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public static FoolsEngine create(int maxEntities, int maxComponents,int width, int height) {
        FoolsEngine theEngine = new FoolsEngine(maxEntities, maxComponents, width, height);
        theEngine.updateSettings();
        return theEngine;
    }

    public void updateSettings(){
        //TODO:完善设置更新系统
        aspect = (float) width / (float) height;
        FOV = 40.0f;
    }

}
