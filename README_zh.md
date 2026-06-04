# foolsEngine

**[English](README.md)** | **中文**

---

一个基于 Java 的自研 3D 游戏引擎，构建于 LWJGL 3 之上，支持 OpenGL 4.3+ 渲染、ECS（实体组件系统）架构、多光源阴影映射与实例化渲染。

## 技术栈

| 层级 | 技术 |
|---|---|
| 语言 | Java 17+ |
| 图形 API | OpenGL 4.3+ (LWJGL 3.4.1) |
| 窗口系统 | GLFW (LWJGL) |
| 数学库 | JOML 1.10.5 |
| 图片加载 | STBImage (LWJGL-STB) |
| 调试 UI | Dear ImGui (imgui-java 1.92.0, compile-only 可选依赖) |
| 日志 | 内置 Logger (util/Logger) |
| 构建工具 | Gradle (Kotlin DSL) |

## 环境要求

- JDK 17 或更高版本
- LWJGL 本地库（已内置在 `lwjgl/` 目录，也可从 Maven Central 获取）

## 构建

### 本地依赖（默认）

LWJGL 3.3.6 JAR 包已内置在 `lwjgl/` 目录。默认平台为 `windows`。  
在 `gradle.properties` 中指定其他平台：

```properties
lwjglNatives=linux          # windows | linux | macos | macos-arm64
```

```bash
./gradlew.bat compileJava
```

### 在线 Maven 依赖（备选）

切换为从 Maven Central 拉取，无需预先下载 JAR：

```properties
lwjglUseMaven=true
lwjglNatives=windows
```

```bash
./gradlew.bat compileJava
```


## 运行测试

```bash
# 基础渲染测试 — 旋转的龙模型
./gradlew.bat runTestBackend

# 输入测试 — 第一人称相机，WASD 移动 + 鼠标视角
./gradlew.bat runTestInputBackend

# 光照测试 — 方向光/点光源/聚光灯 + 阴影映射
./gradlew.bat runTestLightBackend

# 运行全部测试
./gradlew.bat runAllTests

# 构建包含全部依赖的 fat JAR
./gradlew.bat fatJar
```

### TestLightBackend 操作说明

| 按键 | 功能 |
|---|---|
| WASD / 空格 / Shift | 移动相机 |
| 鼠标 | 旋转视角 |
| P | 生成方向光（随机颜色） |
| O | 生成点光源 |
| I | 生成聚光灯 |
| `,` | 生成投射阴影的方向光 |
| N | 生成投射阴影的聚光灯 |
| L | 清除所有光源 |
| J / K | 增加 / 降低环境光 |
| C | 切换 ImGui 调试面板 |
| ESC | 退出 |

## 关键 API

```java
// 窗口
Window win = ...;
win.setCursorMode(CursorMode.DISABLED); // NORMAL, HIDDEN, DISABLED

// 输入（通过 InputManager）
InputManager input = foolsEngine.serviceFactory.createInputManager(win);
input.bind(input.getKeyboard(), FoolsEngineKeyCode.W, action);
if (input.isActionPressed(action)) { ... }

// 渲染
RenderScene scene = new RenderScene();
scene.setCamera(camera);
scene.setLighting(lightEnv);
scene.submit(new RenderCommand(mesh, material, transform));

frame.init();
frame.render(scene);  // 阴影通道从 lightEnv 自动检测

// 阴影 — 由 LightEnvironment 持有，而非 RenderFrame
LightEnvironment lightEnv = new LightEnvironment();
lightEnv.setAmbient(0.08f, 0.08f, 0.08f);
lightEnv.enableShadows(shadowArray, depthMaterial, maxLayers);

Light dirLight = lightEnv.enableDirLightShadow(baseLight, mainCamera);
Light spotLight = lightEnv.enableSpotLightShadow(baseLight, nearPlane);
lightEnv.add(dirLight);

lightEnv.clear();     // 清除光源 + 重置阴影层
lightEnv.destroy();   // 销毁阴影资源

// ImGui（可选 — 需自行添加 imgui-java 到 classpath）
ImGuiContext ctx = new ImGuiContext();
ctx.init(win.getID(), "#version 330");
ImGuiRenderer renderer = new ImGuiRenderer(ctx);
ImGuiDebugOverlay overlay = new ImGuiDebugOverlay();

// 在渲染循环中：
renderer.beginFrame();
overlay.render(scene, deltaTime, renderTimeMs, cameraPos, yaw, pitch, drawCalls);
renderer.endFrame();
```

## 项目结构

```
src/main/java/com/melon/foolsEngine/
  api/                          # 公共 API（接口与资源类型）
    input/                      # 输入抽象层（InputManager, Action, 设备接口）
    rendering/
      render/                   # RenderFrame, RenderTarget, RenderThreadPool
      resource/                 # Mesh, Texture, Material, Camera, Light, Shadow
      shader/                   # ShaderProgram 接口
    windows/                    # Window, WindowsManager 接口
    APIFactory.java             # 后端工厂接口
    InternalFactoryStub.java    # 后端注入单例

  backend/
    OpenGL/                     # OpenGL/GLFW 后端实现
      GLRenderFrame.java        # 核心渲染器：合批、实例化、阴影通道
      GLMesh.java               # VAO/VBO/EBO + 实例化属性
      GLShaderProgram.java      # Shader 编译、链接、uniform 绑定
      GLTexture.java            # STBImage → GPU 纹理
      GLFrameBuffer.java        # FBO（颜色+深度、用于阴影的深度数组）
      GLWindow.java             # GLFW 窗口封装
      GLFWWindowsManager.java   # 窗口生命周期管理
      GLFWKeyBoard.java         # GLFW 键盘输入
      GLFWMouse.java            # GLFW 鼠标输入
      GLInternalFactory.java    # 后端 DI 注册

  core/
    FoolsEngine.java            # 引擎入口点
    ECS/                        # 实体组件系统
      basicComponents/          # Transform, CameraComponent, Renderable, Light
      entity/EntityFactory.java
      system/                   # CameraCollector, RenderableCollector, LightCollector
    events/                     # 事件总线
    world/                      # Entity/Component/System 管理器, ServiceFactory

   util/                         # Projection, CursorMode, ObjLoader, Signature, SparseSet 等
     Logger.java                 # 内置日志（TRACE → ERROR）
     LogLevel.java               # 日志严重级别枚举
     ImGuiHelper.java            # 可选的 ImGui 输入转发（无 ImGui 时静默空操作）
     imgui/                      # ImGuiContext, ImGuiRenderer, ImGuiDebugOverlay（compile-only）

src/main/resources/shader/
  vsh/main_vsh.glsl             # 主顶点着色器（实例化）
  fsh/main_fsh.glsl             # Phong 光照（16 光源）+ 阴影 PCF
  vsh/depth_vsh.glsl            # 阴影贴图顶点着色器
  fsh/depth_fsh.glsl            # 阴影贴图片段着色器（空实现）
```

## 架构

### 渲染管线

```
frame.render(RenderScene)
    │
    ├─ 阴影通道（逐个投射阴影的光源）
    │     sm = scene.getLighting().getShadowManager()  // 从 LightEnvironment 获取
    │     ctx = sm.prepareShadow(light, mainCamera)
    │     renderCommands(commands, ctx.target, ctx.depthMaterial, ctx.layer)
    │
    └─ 颜色通道
          glClear → renderCommands(commands, null, null, -1)
          （实例化绘制，按 Mesh+Material 合批）
```

- **反转 Z 缓冲**：`glDepthFunc(GL_GREATER)`、`glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)`——在远距离场景下获得优异的深度精度。
- **实例化绘制**：共享同一 Mesh+Material 的对象被合并为单次 `glDrawElementsInstanced` 调用。
- **阴影映射**：使用 2D 纹理数组（`GL_TEXTURE_2D_ARRAY`）作为阴影贴图图集，片元着色器中以 5×5 PCF 采样。`ShadowManager` 由 `LightEnvironment` 持有，负责层分配（含空闲列表复用）、阴影相机更新和 light-space 矩阵同步；渲染器执行绘制。

### ImGui 集成

- **惰性加载**：核心引擎中所有 ImGui 调用均通过 `ImGuiHelper`，该方法通过 `Class.forName("imgui.ImGui")` 检测 imgui-java 是否在 classpath 上。缺失时，所有转发方法变为空操作——`ImGuiInternal`（实际持有 imgui import 的内部类）永远不会被 JVM 加载。
- **构建**：imgui-java 在 `build.gradle.kts` 中为 `compileOnly`——需要 ImGui 的调用方必须自行添加依赖。测试代码使用 `testImplementation`。
- **输入转发**：键盘修饰键（Ctrl/Shift/Alt/Super）、鼠标按键/位置/滚轮以及光标模式变更均通过现有 GLFW 回调转发至 ImGui——不安装独立的 ImGui 回调。

### ECS

| 层级 | 职责 |
|---|---|
| Entity | 整数 ID |
| Component | 纯数据（Transform, CameraComponent, Renderable, Light） |
| `SparseSet<T>` | 缓存友好的紧凑组件存储（O(1) 增删） |
| `Signature` | 实体-组件匹配的位掩码 |
| System | 对匹配实体逐帧执行逻辑（CameraCollector → frame.setCamera，RenderableCollector → frame.submit） |
| `SystemScheduler` | 逐帧顺序执行各 System |

### 后端抽象

```
ServiceFactory → InternalFactoryStub → GLInternalFactory（OpenGL）
                                      → Vulkan 桩（预留）
```

`APIFactory` 定义了服务创建协议。各后端通过静态注入完成注册。目前仅 OpenGL 可用。

### 依赖方向

```
LightEnvironment ──持有──→ ShadowManager ──→ ShadowPassContext
RenderFrame ──读取──→ LightEnvironment.getShadowManager()   （每帧）
GLFWMouse / GLFWKeyBoard / GLWindow ──→ ImGuiHelper         （可选转发）
ImGuiHelper ──守卫──→ ImGuiInternal                         （仅 imgui 存在时加载）
```

- **LightEnvironment** 持有 `ShadowManager`——`enableShadows()`、`enableDirLightShadow()`、`enableSpotLightShadow()` 均为委托方法。`remove()` 通过 `shadowManager.releaseLayer()` 自动释放阴影层。`clear()` 自动重置所有层。
- **RenderFrame** 不再持有 `ShadowManager` 引用，每帧从场景的 `LightEnvironment` 通过 `getShadowManager()` 读取。
- **GLFWMouse/GLFWKeyBoard/GLWindow** 绝不直接导入 `imgui.ImGui`。所有转发通过 `ImGuiHelper`，imgui-java 缺失时安全空操作。

## 设计决策

- 对不可变数据包使用 **Record 类型**（`ShadowInfo`, `ShadowPassContext`, `RenderCommand`, `MeshData`）。
- 旧 API 标记 **`@Deprecated`** 而非立即删除（`RenderFrame` 上的 `setCamera`、`submit`、`applyLightEnvironment`、`setShadowManager`）。新代码应使用 `frame.render(RenderScene)`。
- **反转 Z 缓冲**深度范围：近平面 = 1.0，远平面 = 0.0。
- **LightEnvironment 持有 ShadowManager**：阴影层按光源分配，`clear()` 自动重置层，避免用户忘记调用 `shadowManager.reset()`。
- **ImGuiHelper 惰性加载**：核心引擎绝不静态导入 `imgui.ImGui`。通过 `Class.forName` 守卫 + 私有内部类保证 JVM 仅在 imgui-java 处于 classpath 时才加载 ImGui 相关类。
- **通过 Factory 创建 InputManager**：`createInputManager(win)` 使用泛型 `<E>`——api/test 层绝不引用具体的 `GLFWKeyBoard`/`GLFWMouse` 类型。

## 已知限制

- `GLRenderFrame` 中的 `renderCommands()` 是一个 200 行的 private 方法，混合了合批、纹理绑定、Shader 参数设置和 draw call——应拆分为可组合的 Pass 执行器。
- Vulkan 后端仅有桩代码，未实现。
- `LightCollector` ECS System 为桩代码。
- 测试为手动集成 `JavaExec` 任务，未使用 JUnit 框架。
- `Light.buildDirLightShadowCam()` 已标记 `@Deprecated`——逻辑已迁移至 `ShadowManager.updateDirShadowCamera()`，但旧方法体保留以保持向后兼容。

## 许可证

foolsEngine — 一个基于 Java 的自研 3D 游戏引擎
Copyright (C) 2026  melon_444

本程序为自由软件：您可以依据自由软件基金会发布的 GNU 通用公共许可证
（版本 3 或您选择的任何更新版本）的条款重新分发和/或修改本程序。

本程序的分发出于有用之目的，但**不提供任何担保**；甚至不提供对
**适销性**或**特定用途适用性**的默示担保。详情请参见 [LICENSE](LICENSE) 中的
GNU 通用公共许可证。
