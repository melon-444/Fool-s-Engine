<p ><img src="./assets/branding/fools-engine-icon.png" alt="Logo" width="200"><img src="./assets/branding/fools-engine-title.png" alt="Logo" width="650"></p>

**English** | **[中文](README_zh.md)**

---

A custom 3D game engine in Java, built on LWJGL 3 with OpenGL 4.3+ rendering, Entity-Component-System (ECS) architecture, multi-light shadow mapping, and instanced rendering.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Graphics | OpenGL 4.3+ (LWJGL 3.4.2) / Vulkan 1.3 (stub, LWJGL 3.4.2) |
| Windowing | GLFW (LWJGL) |
| Math | JOML 1.10.5 |
| Image loading | STBImage (LWJGL-STB) |
| Debug UI | Dear ImGui (imgui-java 1.92.0, compile-only) |
| Logging | built-in Logger (util/Logger) |
| Build | Gradle (Kotlin DSL) |

## Prerequisites

- JDK 17 or higher
- LWJGL native libraries (included locally at `lwjgl/`, or fetched from Maven Central)

## Build

### Local dependencies (default)

LWJGL 3.4.2 JARs are bundled in the `lwjgl/` directory. The default platform is `windows`.  
Specify a different platform via `gradle.properties`:

```properties
lwjglNatives=linux          # windows | linux | macos | macos-arm64
```

```bash
./gradlew.bat compileJava
```

### Online Maven dependencies (alternative)

Use Maven Central instead of local JARs — no file downloads needed:

```properties
lwjglUseMaven=true
lwjglNatives=windows
```

```bash
./gradlew.bat compileJava
```


## Run Tests

```bash
# Basic rendering test - rotating dragon model
./gradlew.bat runTestBackend

# Input test - first-person camera with WASD + mouse look
./gradlew.bat runTestInputBackend

# Lighting test - directional/point/spot lights with shadow mapping
./gradlew.bat runTestLightBackend

# Run all tests
./gradlew.bat runAllTests

# ECS + ShaderPass integration test
./gradlew.bat runTesECSRenderFlow

# Build fat JAR with all dependencies
./gradlew.bat fatJar
```

### TestLightBackend Controls

| Key | Action |
|---|---|
| WASD / Space / Shift | Move camera |
| Mouse | Look around |
| P | Spawn directional light (random color) |
| O | Spawn point light |
| I | Spawn spot light |
| `,` | Spawn shadow-casting directional light |
| N | Spawn shadow-casting spot light |
| L | Clear all lights |
| J / K | Increase / decrease ambient |
| C | Toggle ImGui debug overlay |
| ESC | Exit |

## Key API

```java
// Window
Window win = ...;
win.setCursorMode(CursorMode.DISABLED); // NORMAL, HIDDEN, DISABLED

// Input (through InputManager)
InputManager input = foolsEngine.serviceFactory.createInputManager(win);
input.bind(input.getKeyboard(), FoolsEngineKeyCode.W, action);
if (input.isActionPressed(action)) { ... }

// Rendering — pass-based pipeline
RenderScene scene = new RenderScene();
scene.setCamera(camera);
scene.setLighting(lightEnv);
scene.submit(new RenderCommand(mesh, material, transform));
// Passes are configured via ECS entities (auto-shadow from LightEnvironment)
frame.init();
frame.render(scene);

// Custom render passes (post-processing, custom effects)
ShaderPass bloom = ShaderPass.postProcess(bloomShader)
    .output(bloomRT)
    .input(colorRT, "sceneColor")
    .uniform("intensity", 1.5f);
scene.submitPass(bloom);

// EventBus — annotation-driven pub/sub
// @EventBus(id="SystemBus") on FoolsEngine auto-creates the bus
EventBus.get("SystemBus").emit(new LightAdded(entityId, light));

// Shadows — owned by LightEnvironment, not by RenderFrame
LightEnvironment lightEnv = new LightEnvironment();
lightEnv.setAmbient(0.08f, 0.08f, 0.08f);
lightEnv.enableShadows(shadowArray, depthMaterial, maxLayers);

Light dirLight = lightEnv.enableDirLightShadow(baseLight, mainCamera);
Light spotLight = lightEnv.enableSpotLightShadow(baseLight, nearPlane);
lightEnv.add(dirLight);

lightEnv.clear();     // clears lights + resets shadow layers
lightEnv.destroy();   // destroys shadow resources

// ImGui (optional — add imgui-java to your classpath)
ImGuiContext ctx = new ImGuiContext();
ctx.init(win.getID(), "#version 330");
ImGuiRenderer renderer = new ImGuiRenderer(ctx);
ImGuiDebugOverlay overlay = new ImGuiDebugOverlay();

// In your render loop:
renderer.beginFrame();
overlay.render(scene, deltaTime, renderTimeMs, cameraPos, yaw, pitch, drawCalls);
renderer.endFrame();
```

## Project Structure

```
src/main/java/com/melon/foolsEngine/
  api/                          # Public API (interfaces & resource types)
    input/                      # Input abstraction (InputManager, Action, devices)
    rendering/
      render/                   # RenderFrame, RenderTarget, RenderThreadPool
      resource/                 # Mesh, Texture, Material, Camera, Light, Shadow
      shader/                   # ShaderProgram interface
    windows/                    # Window, WindowsManager interfaces
    APIFactory.java             # Backend factory interface
    InternalFactoryStub.java    # Backend injection singleton

  backend/
    OpenGL/                     # OpenGL/GLFW implementation
      GLRenderFrame.java        # Core renderer: pass execution, batching, instancing
      GLMesh.java               # VAO/VBO/EBO + instanced attributes
      GLShaderProgram.java      # Shader compile, link, uniform binding
      GLTexture.java            # STBImage → GPU texture
      GLFrameBuffer.java        # FBO (color + depth, depth array for shadows)
      GLWindow.java             # GLFW window wrapper
      GLFWWindowsManager.java   # Window lifecycle management
      GLFWKeyBoard.java         # GLFW keyboard input
      GLFWMouse.java            # GLFW mouse input
      GLInternalFactory.java    # Backend DI registration
      ImGuiHelper.java          # Optional GLFW input forwarding
      imgui/                    # ImGui context, renderer and debug overlay (compile-only)
    Vulkan/                     # Vulkan backend (functional stub)
      VKRenderFrame.java        # Full pipeline: instance/device/swapchain/pipeline/render
      VKMesh.java               # VkBuffer vertex/index management
      VKShaderProgram.java      # Runtime GLSL→SPIR-V via shaderc
      VKWindow.java             # GLFW window + VkSurfaceKHR creation
      VKWindowsManager.java     # GLFW_NO_API window lifecycle
      VKKeyBoard.java           # Vulkan GLFW keyboard input
      VKMouse.java              # Vulkan GLFW mouse input
      VKInternalFactory.java    # Backend DI registration

  core/
    FoolsEngine.java            # Engine entry point (@EventBus("SystemBus"))
    ECS/                        # Entity-Component-System
      basicComponents/          # Transform, CameraComponent, Renderable, Light, RenderPass, etc.
      entity/EntityFactory.java
      system/                   # LightEnvCollector, CameraCollector, LightCollector, RenderableCollector, RenderPassCollector, etc.
    events/                     # EventBus + builtInEvents (EntityCreated, LightAdded, PreRender, etc.)
    annotation/                 # @EventBus, @EventBusSubscriber, @SubscribeEvent, @OnlyIn
    world/                      # Entity/Component/System managers, APIFactory, SystemScheduler

   util/                         # Projection, CursorMode, ObjLoader, Signature, SparseSet, etc.
     Logger.java                 # Built-in logging (TRACE → ERROR)
     LogLevel.java               # Log severity enum

src/main/resources/shader/
  vsh/main_vsh.glsl             # Main vertex shader (instanced)
  fsh/main_fsh.glsl             # Phong lighting (16 lights) + shadow PCF
  vsh/depth_vsh.glsl            # Shadow map vertex shader
  fsh/depth_fsh.glsl            # Shadow map fragment shader (empty)
```

## Architecture

### Rendering Pipeline

```
frame.render(RenderScene)
    │
    ├─ Pass list (from scene.getPasses(), populated by ECS systems)
    │     for each ShaderPass:
    │       executePass → renderCommands + uniforms + inputs
    │
    │   Shadow passes auto-generated by RenderPassCollector
    │   Color pass from user-defined RenderPassComponent
    │   Post-process passes via ShaderPass.fullscreen()
    │
    └─ Screen: glClear → present
```

- **Reversed-Z**: `glDepthFunc(GL_GREATER)`, `glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)` — superior depth precision at far distances.
- **Instanced drawing**: Objects sharing the same Mesh+Material are batched into a single `glDrawElementsInstanced` call.
- **Shadow mapping**: 2D texture array atlas with 5x5 PCF sampling in the fragment shader. `ShadowManager` is owned by `LightEnvironment` and handles layer allocation (with a free-list for reuse), shadow camera updates, and light-space matrix sync; the renderer executes the draw.
- **Configurable passes**: `ShaderPass` + `RenderScene.submitPass()` allows arbitrary pass ordering; `RenderPassCollector` auto-generates shadow passes before user passes.

### Built-in UI

Register a stable pointer target with `scene.ui().hitRegion(id, bounds)` or set `hitId` and `hitBounds` on a `UiComponent`. Call `scheduler.updateUiPointer(x, y, primaryDown, focused)` after polling events, using top-left framebuffer pixels. Read `hit()`, `isHovered(id)`, `isPressed(id)` and `isClicked(id)` from `scheduler.uiInteraction()`. Hits follow the last rendered frame. Use `blockPointer(bounds)` for panel backgrounds. In `TesECSRenderFlow`, press B to open the whiteboard, click its upper-right button to close it, and use a visible cursor to select a model's status frame.

Submit a rectangle or image to the current frame with `scene.ui()`. Coordinates and sizes are framebuffer pixels with the origin at the top left. Higher layers draw on top, with submission order breaking ties. OpenGL draws UI after the scene passes by default. Use `scene.submitPass(ShaderPass.ui())` to place it explicitly in the pass list.

```java
var scene = engine.systemScheduler.getScene();
var red = new UiDrawList.Color(0.85f, 0.1f, 0.15f, 1);
scene.ui().rectangle(16, 16, 120, 10, red, 0);
scene.ui().submit(new UiDrawList.Quad(-32, -14, 64, 6, red, 1)
        .at(new UiDrawList.WorldAnchor(0, 2, -4)));
```

Submit again each frame. For persistent ECS UI, attach a `UiComponent` to an entity. The built-in `UiCollector` uses the same draw list. Set `followTransform` to follow the entity's `TransformComponent`, optionally with a local offset such as `offsetY = 2` for an overhead health bar. World anchors follow the scene camera and aspect mode. Their quads keep a fixed pixel size and draw over scene geometry.

Menus can acquire and close independent `systemScheduler.pauseLocalSimulation()` and `systemScheduler.captureGameplayInput()` scopes. Local pause skips fixed-step simulation without accumulating time to replay. UI, window events and rendering keep running. For multiplayer, capture gameplay input alone and have gameplay code check `isGameplayInputCaptured()` before acting. Shut down the scheduler before destroying the window.

### ImGui Integration

Create an optional ImGui layer with `engine.serviceFactory.createImGuiLayer(window)`. Call `beginFrame()`, issue Dear ImGui widget calls, and call `endFrame()` while the window's graphics context is current. Close the layer before destroying the window. The engine API exposes the frame lifecycle, while widgets still use imgui-java.

- **Lazy-loading**: All core engine ImGui calls go through `ImGuiHelper`, which detects via `Class.forName("imgui.ImGui")` whether imgui-java is on the classpath. When absent, every forwarding method becomes a no-op — `ImGuiInternal` (the actual imgui import holder) is never loaded by the JVM.
- **Build**: imgui-java is `compileOnly` in `build.gradle.kts` — consumers who want ImGui must add the dependency themselves. Tests use `testImplementation`.
- **Input forwarding**: Keyboard modifiers (Ctrl/Shift/Alt/Super), mouse buttons/position/wheel, and cursor mode changes are forwarded to ImGui through existing GLFW callbacks — no separate ImGui callbacks are installed.

### ECS

| Layer | Responsibility |
|---|---|
| Entity | Integer ID |
| Component | Plain data (Transform, CameraComponent, Renderable, Light) |
| `SparseSet<T>` | Cache-friendly packed component storage (O(1) add/remove) |
| `Signature` | Bitmask for entity-component matching |
| System | Per-frame logic over matched entities (CameraCollector → scene.setCamera, RenderableCollector → scene.submit) |
| `SystemScheduler` | Sequential system update per frame (sorted by `priority()`) |

### EventBus

- **Annotation-driven**: `@EventBus(id="SystemBus")` on a class auto-creates a named bus; `@EventBusSubscriber` auto-registers static `@SubscribeEvent` methods at class-load time.
- **Double-buffered dispatch**: `emit(Event)` queues per frame, `process()` dispatches to all matching listeners with class-hierarchy walk.
- **Auto-discovery**: `EngineBoot.ValidatingLoader` intercepts class loading to trigger `@EventBus` / `@EventBusSubscriber` processing.
- **Built-in events**: `EntityCreated/Destroyed`, `ComponentAdded/Removed`, `MainCameraChanged`, `LightAdded/Removed`, `PreRender/PostRender`.

### Backend Abstraction

```
APIFactory ← InternalFactoryStub → GLInternalFactory (OpenGL)
                                      → VKInternalFactory (Vulkan)
```

`APIFactory` defines the contract. Each backend registers itself via static injection.

### Dependency Direction

```
LightEnvironment ──owns──→ ShadowManager ──→ ShadowPassContext
RenderFrame ──reads──→ LightEnvironment.getShadowManager()   (per frame)
GLFWMouse / GLFWKeyBoard / GLWindow ──→ ImGuiHelper          (optional forwarding)
ImGuiHelper ──guard──→ ImGuiInternal                         (only loaded if imgui present)
```

- **LightEnvironment** owns `ShadowManager` — `enableShadows()`, `enableDirLightShadow()`, `enableSpotLightShadow()` are all delegated. `remove()` auto-releases the shadow layer via `shadowManager.releaseLayer()`. `clear()` automatically resets all layers.
- **RenderFrame** no longer holds a `ShadowManager` reference. It reads it from the scene's `LightEnvironment` each frame via `getShadowManager()`.
- **GLFWMouse/GLFWKeyBoard/GLWindow** never directly import `imgui.ImGui`. All forwarding goes through `ImGuiHelper`, which safely no-ops when imgui-java is absent.

## Design Decisions

- **Record types** for immutable data bundles (`ShadowInfo`, `ShadowPassContext`, `RenderCommand`, `MeshData`).
- **`@Deprecated`** old API instead of immediate removal (`setCamera`, `submit`, `applyLightEnvironment`, `setShadowManager` on `RenderFrame`). New code uses `frame.render(RenderScene)`.
- **Reverse-Z** depth range: near=1.0, far=0.0.
- **LightEnvironment owns ShadowManager**: shadow layers are per-light allocations. `clear()` auto-resets layers, preventing the user from forgetting `shadowManager.reset()`.
- **ImGuiHelper lazy-loading**: Core engine never statically imports `imgui.ImGui`. A `Class.forName` guard + private inner class ensures the JVM never loads ImGui classes unless imgui-java is on the classpath.
- **InputManager via Factory**: `createInputManager(win)` with generic `<E>` — api/test layers never reference concrete `GLFWKeyBoard`/`GLFWMouse` types.
- **One action, one key**: `ActionMapping.bind` moves an action off its previous key (single source of truth; O(1) lookup both ways; no orphaned bindings). To drive one behaviour from several keys, register one `Action` per key and OR them (`isActionDown(a) || isActionDown(b)`).
- **Exceptions over `System.exit`**: engine failures throw subclasses of `com.melon.foolsEngine.exception.FoolsEngineException` (`ShaderException`, `ResourceException`, `InitializationException`) so callers can catch, log, and recover.
- **System priority over registration order**: `priority()` method decouples execution ordering from `registerSystem` call sequence.
- **Passes via Scene, not Frame**: `scene.submitPass()` with double-buffer swap — passes are per-frame like commands, not persistent state.
- **Annotation-driven EventBus**: `@EventBus`/`@EventBusSubscriber`/`@SubscribeEvent` — zero-lambda, method-only, auto-discovered at class-load time.
- **CameraComponent.isMainCam**: boolean flag replaces active+mutex pattern — decouples main view camera from side cameras without modifying other entities.

## Known Limitations

- `renderCommands()` in `GLRenderFrame` is a 200-line private method mixing batching, texture binding, shader params, and draw calls — should be split into composable Pass executors.
- Vulkan backend renders a triangle; full scene integration (ECS commands, shadows, textures) not yet implemented.
- Tests are manual integration `JavaExec` tasks, not JUnit.
- `Light.buildDirLightShadowCam()` is `@Deprecated` — the logic now lives in `ShadowManager.updateDirShadowCamera()`, but the old method body is retained for backward compatibility.

## License

foolsEngine — A custom 3D game engine in Java
Copyright (C) 2026  melon_444

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License in [LICENSE](LICENSE) for more details.
