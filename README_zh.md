# foolsEngine

**[English](README.md)** | **中文**

---

一个基于 Java 的自研 3D 游戏引擎，构建于 LWJGL 3 之上，支持 OpenGL 4.3+ 渲染、ECS（实体组件系统）架构、多光源阴影映射与实例化渲染。

## 技术栈

| 层级 | 技术 |
|---|---|
| 语言 | Java 17+ |
| 图形 API | OpenGL 4.3+ (LWJGL 3) |
| 窗口系统 | GLFW (LWJGL) |
| 数学库 | JOML 1.10.5 |
| 图片加载 | STBImage (LWJGL-STB) |
| 构建工具 | Gradle (Kotlin DSL) |

## 环境要求

- JDK 17 或更高版本
- Windows x64（LWJGL 本地库仅包含 Windows 版本）

## 构建

```bash
# 编译主代码
./gradlew.bat compileJava

# 编译主代码 + 测试代码
./gradlew.bat compileJava compileTestJava
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
| ESC | 退出 |

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

  util/                         # SparseSet, Signature, Projection, ObjLoader 等

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
    │     ctx = shadowManager.prepareShadow(light, mainCamera)
    │     renderCommands(commands, ctx.target, ctx.depthMaterial, ctx.layer)
    │
    └─ 颜色通道
          glClear → renderCommands(commands, null, null, -1)
          （实例化绘制，按 Mesh+Material 合批）
```

- **反转 Z 缓冲**：`glDepthFunc(GL_GREATER)`、`glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)`——在远距离场景下获得优异的深度精度。
- **实例化绘制**：共享同一 Mesh+Material 的对象被合并为单次 `glDrawElementsInstanced` 调用。
- **阴影映射**：使用 2D 纹理数组（`GL_TEXTURE_2D_ARRAY`）作为阴影贴图图集，片元着色器中以 5×5 PCF 采样。`ShadowManager` 负责层分配和阴影相机准备；渲染器执行绘制。

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

### 依赖方向（重构后）

```
RenderFrame → ShadowManager → ShadowPassContext
                                     ↑
                              （纯数据，不执行渲染）
```

- **RenderFrame** 拥有渲染执行权（阴影通道循环 + 颜色通道）。
- **ShadowManager** 管理阴影资源（层分配、ShadowInfo 创建、light-space 矩阵同步、阴影相机构建）。返回 `ShadowPassContext`——从不调用任何绘制命令。

## 设计决策

- 对不可变数据包使用 **Record 类型**（`ShadowInfo`, `ShadowPassContext`, `RenderCommand`, `MeshData`）。
- 旧 API 标记 **`@Deprecated`** 而非立即删除（`RenderFrame` 上的 `setCamera`、`submit`、`applyLightEnvironment`）。新代码应使用 `frame.render(RenderScene)`。
- 重构过程中**禁止修改 Shader**——渲染行为保持百分百一致。
- **反转 Z 缓冲**深度范围：近平面 = 1.0，远平面 = 0.0。

## 已知限制

- 聚光灯的阴影相机在创建时一次性构建；移动聚光灯光源不会更新其阴影相机。
- `Light.buildDirLightShadowCam()` 混合了光源定义与阴影相机计算——将来可由独立的 `ShadowCameraBuilder` 承担。
- `GLRenderFrame` 中的 `renderCommands()` 是一个 200 行的 private 方法，混合了合批、纹理绑定、Shader 参数设置和 draw call——应拆分为可组合的 Pass 执行器。
- Vulkan 后端仅有桩代码，未实现。
- `LightCollector` ECS System 为桩代码。
- 测试为手动集成 `JavaExec` 任务，未使用 JUnit 框架。
