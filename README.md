# foolsEngine

**English** | **[中文](README_zh.md)**

---

A custom 3D game engine in Java, built on LWJGL 3 with OpenGL 4.3+ rendering, Entity-Component-System (ECS) architecture, multi-light shadow mapping, and instanced rendering.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17+ |
| Graphics | OpenGL 4.3+ (LWJGL 3.3.6) |
| Windowing | GLFW (LWJGL) |
| Math | JOML 1.10.5 |
| Image loading | STBImage (LWJGL-STB) |
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

# Compile with tests
./gradlew.bat compileJava compileTestJava
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
| ESC | Exit |

## Key API

```java
// Window
Window win = ...;
win.setCursorMode(CursorMode.DISABLED); // NORMAL, HIDDEN, DISABLED

// Rendering
RenderScene scene = new RenderScene();
scene.setCamera(camera);
scene.setLighting(lightEnv);
scene.submit(new RenderCommand(mesh, material, transform));

frame.init();
frame.setShadowManager(shadowManager);
frame.render(scene);

// Shadows
ShadowManager sm = new ShadowManager(shadowArray, depthMaterial, maxLayers);
Light dirLight = sm.enableDirLightShadow(baseLight, mainCamera);
Light spotLight = sm.enableSpotLightShadow(baseLight, nearPlane);
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

  util/                         # SparseSet, Signature, Projection, CursorMode, ObjLoader, etc.

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
    │     ctx = shadowManager.prepareShadow(light, mainCamera)
    │     renderCommands(commands, ctx.target, ctx.depthMaterial, ctx.layer)
    │
    └─ Color Pass
          glClear → renderCommands(commands, null, null, -1)
          (instanced draw, batched by Mesh+Material)
```

- **Reversed-Z**: `glDepthFunc(GL_GREATER)`, `glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)` — superior depth precision at far distances.
- **Instanced drawing**: Objects sharing the same Mesh+Material are batched into a single `glDrawElementsInstanced` call.
- **Shadow mapping**: 2D texture array atlas with 5×5 PCF sampling in the fragment shader. `ShadowManager` handles layer allocation and shadow camera preparation; the renderer executes the draw.

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

### Dependency Direction (post-refactor)

```
RenderFrame  ──→  ShadowManager  ──→  ShadowPassContext      (no rendering)
GLFWMouse    ──→  InputDevice<Window>                          (input only)
Window.setCursorMode()                                         (cursor control)
```

- **RenderFrame** owns rendering execution (shadow pass loop + color pass).
- **ShadowManager** manages shadow resources (layer allocation, ShadowInfo creation, light-space matrix sync). Returns `ShadowPassContext` — never calls draw commands.
- **GLFWMouse** no longer controls cursor mode — cursor behavior is managed by `Window.setCursorMode(CursorMode)`.

## Design Decisions

- **Record types** for immutable data bundles (`ShadowInfo`, `ShadowPassContext`, `RenderCommand`, `MeshData`).
- **`@Deprecated`** old API instead of immediate removal (`setCamera`, `submit`, `applyLightEnvironment` on `RenderFrame`). New code uses `frame.render(RenderScene)`.
- **No shader modifications** during refactoring — rendering behavior preserved exactly.
- **Reverse-Z** depth range: near=1.0, far=0.0.

## Known Limitations

- Spot light shadow cameras are built once at creation; moving a spot light does not update its shadow camera.
- `Light.buildDirLightShadowCam()` mixes light definition with shadow camera computation — a future `ShadowCameraBuilder` would be cleaner.
- `renderCommands()` in `GLRenderFrame` is a 200-line private method mixing batching, texture binding, shader params, and draw calls — should be split into composable Pass executors.
- Vulkan backend is stubbed but not implemented.
- `LightCollector` ECS system is a stub.
- Tests are manual integration `JavaExec` tasks, not JUnit.

## License

Proprietary — internal development.
