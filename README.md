# foolsEngine

**English** | **[中文](README_zh.md)**

---

A custom 3D game engine in Java, built on LWJGL 3 with OpenGL 4.3+ rendering, Entity-Component-System (ECS) architecture, multi-light shadow mapping, and instanced rendering.

## Tech Stack

| Layer | Technology |
|---|---|---|
| Language | Java 17+ |
| Graphics | OpenGL 4.3+ (LWJGL 3.4.1) |
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

LWJGL 3.3.6 JARs are bundled in the `lwjgl/` directory. The default platform is `windows`.  
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

// Rendering
RenderScene scene = new RenderScene();
scene.setCamera(camera);
scene.setLighting(lightEnv);
scene.submit(new RenderCommand(mesh, material, transform));

frame.init();
frame.render(scene);  // shadow pass auto-detected from lightEnv

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
      GLRenderFrame.java        # Core renderer: batching, instancing, shadow pass
      GLMesh.java               # VAO/VBO/EBO + instanced attributes
      GLShaderProgram.java      # Shader compile, link, uniform binding
      GLTexture.java            # STBImage → GPU texture
      GLFrameBuffer.java        # FBO (color + depth, depth array for shadows)
      GLWindow.java             # GLFW window wrapper
      GLFWWindowsManager.java   # Window lifecycle management
      GLFWKeyBoard.java         # GLFW keyboard input
      GLFWMouse.java            # GLFW mouse input
      GLInternalFactory.java    # Backend DI registration

  core/
    FoolsEngine.java            # Engine entry point
    ECS/                        # Entity-Component-System
      basicComponents/          # Transform, CameraComponent, Renderable, Light
      entity/EntityFactory.java
      system/                   # CameraCollector, RenderableCollector, LightCollector
    events/                     # EventBus
    world/                      # Entity/Component/System managers, ServiceFactory

   util/                         # Projection, CursorMode, ObjLoader, Signature, SparseSet, etc.
     Logger.java                 # Built-in logging (TRACE → ERROR)
     LogLevel.java               # Log severity enum
     ImGuiHelper.java            # Optional ImGui input forwarding (no-op when imgui absent)
     imgui/                      # ImGuiContext, ImGuiRenderer, ImGuiDebugOverlay (compile-only)

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
    ├─ Shadow Pass (per shadow-casting light)
    │     sm = scene.getLighting().getShadowManager()  // from LightEnvironment
    │     ctx = sm.prepareShadow(light, mainCamera)
    │     renderCommands(commands, ctx.target, ctx.depthMaterial, ctx.layer)
    │
    └─ Color Pass
          glClear → renderCommands(commands, null, null, -1)
          (instanced draw, batched by Mesh+Material)
```

- **Reversed-Z**: `glDepthFunc(GL_GREATER)`, `glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)` — superior depth precision at far distances.
- **Instanced drawing**: Objects sharing the same Mesh+Material are batched into a single `glDrawElementsInstanced` call.
- **Shadow mapping**: 2D texture array atlas with 5x5 PCF sampling in the fragment shader. `ShadowManager` is owned by `LightEnvironment` and handles layer allocation (with a free-list for reuse), shadow camera updates, and light-space matrix sync; the renderer executes the draw.

### ImGui Integration

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
| System | Per-frame logic over matched entities (CameraCollector → frame.setCamera, RenderableCollector → frame.submit) |
| `SystemScheduler` | Sequential system update per frame |

### Backend Abstraction

```
ServiceFactory → InternalFactoryStub → GLInternalFactory (OpenGL)
                                      → Vulkan stub (future)
```

`APIFactory` defines the contract. Each backend registers itself via static injection. Currently only OpenGL is functional.

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

## Known Limitations

- `renderCommands()` in `GLRenderFrame` is a 200-line private method mixing batching, texture binding, shader params, and draw calls — should be split into composable Pass executors.
- Vulkan backend is stubbed but not implemented.
- `LightCollector` ECS system is a stub.
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
