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

import com.melon.foolsEngine.util.Distribution;
import com.melon.foolsEngine.core.annotation.EventBus;
import com.melon.foolsEngine.core.annotation.EventBusSubscriber;
import com.melon.foolsEngine.core.annotation.OnlyIn;
import com.melon.foolsEngine.util.logger.LogLevel;
import com.melon.foolsEngine.util.logger.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class EngineBoot {

    private static final Logger LOG = new Logger("Boot");

    private EngineBoot() {
    }
    public static FoolsEngine create(int maxEntities, int maxComponents, int width, int height, boolean isServer){
        return create(maxEntities, maxComponents, width, height, isServer,LogLevel.INFO);
    }

    public static FoolsEngine create(int maxEntities, int maxComponents, int width, int height, boolean isServer, LogLevel level) {
        LOG.info("Engine starting | mode=%s maxEntities=%d", isServer ? "SERVER" : "CLIENT", maxEntities);
        ValidatingLoader loader = new ValidatingLoader(Thread.currentThread().getContextClassLoader(),isServer);
        Thread.currentThread().setContextClassLoader(loader);
        try {
            loader.loadClass("com.melon.foolsEngine.core.FoolsEngine", true);
        }catch(ClassNotFoundException e) {
            LOG.error("FoolsEngine class not found: %s", e.getMessage());
            throw new InternalError("FoolsEngine class not found");
        }
        FoolsEngine engine = new FoolsEngine(maxEntities, maxComponents, width, height, isServer);
        engine.updateSettings();
        LOG.info("Engine ready | FOV=%.1f aspect=%.2f zNear=%.4f", engine.FOV, engine.aspect, engine.Z_NEAR);
        return engine;
    }

    public static void validateSystems(FoolsEngine engine) {
        for (var system : engine.systemManager.getRegisteredSystems().values()) {
            validateClass(system.getClass(), engine.isServer);
        }
    }

    private static void validateClass(Class<?> clazz, boolean isServer) {
        OnlyIn ann = clazz.getAnnotation(OnlyIn.class);
        if (ann == null) {
            return;
        }
        if (isServer && ann.value() == Distribution.Client) {
            throw new IllegalStateException(
                    "Client-only class registered on server: " + clazz.getName());
        }
        //Client has Built-In server while dedicated server has no Built-In client.
    }

    static final class ValidatingLoader extends ClassLoader {
        private final boolean isServer;

        ValidatingLoader(ClassLoader parent, boolean isServer) {
            super(parent);
            this.isServer = isServer;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            try {
                c = getParent().loadClass(name);
            } catch (ClassNotFoundException e) {
                c = findClass(name);
            }

            validateClass(c, isServer);
            processEventBusAnnotations(c);

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }

        private void processEventBusAnnotations(Class<?> c) {
            EventBus busAnn = c.getAnnotation(EventBus.class);
            if (busAnn != null) {
                com.melon.foolsEngine.core.events.EventBus.create(busAnn.id());
            }

            EventBusSubscriber subAnn = c.getAnnotation(EventBusSubscriber.class);
            if (subAnn != null) {
                com.melon.foolsEngine.core.events.EventBus bus =
                        com.melon.foolsEngine.core.events.EventBus.get(subAnn.id());
                if (bus != null) {
                    bus.registerStaticSubscribers(c);
                }
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String path = name.replace('.', '/') + ".class";
            try (InputStream in = getParent().getResourceAsStream(path)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[4096];
                int n;
                while ((n = in.read(tmp)) != -1) {
                    buf.write(tmp, 0, n);
                }
                byte[] bytes = buf.toByteArray();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
