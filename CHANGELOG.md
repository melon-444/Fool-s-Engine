### 0.1.1 — 2026-07-24

#### ECS 系统层

- **SceneCollector 合并**：`CameraCollector`、`LightCollector`、`RenderableCollector` 三合一，统一在一次遍历中收集相机、光源（含阴影）、可渲染实体；旧收集系统标记 `@Deprecated`
- **签名匹配修复**：`SystemManager.entitySignatureChanged` 中 `systemSig.includes(entitySignature)` 语义反转——entity 缺少组件时被误匹配——改为 `entitySignature.matches(systemSig)`
- **Transform 封装**：`position`/`rotation`/`scale` 改为私有字段，通过 `getPosition()`/`getRotation()`/`getScale()` 和便捷 setter（`position()`/`rotation()`/`scale()`）访问，setter 自动 `markDirty()`
- **Transform 矩阵分解**：`setFromMatrix()` 新增 `decompose()`——从 4×4 模型矩阵自动提取平移、旋转（列归一化→四元数）、缩放，支持镜像修复
- **ClientSystem 去耦**：构造函数改为 `super(engine, null)`，不再依赖 `engine.systemScheduler.getScene()`；`RenderScene` 每帧通过 `update(dt, scene)` 参数传入
- **ECS Light 组件扩展**：新增 `shadowNear`、`intensity`、`castsShadow` 字段，支持自动化阴影光源收集
- **EntityFactory 完善**：`createLightEntity()` 额外绑定 `Transform` 组件，确保归入 SceneCollector 统一匹配

#### 渲染管线

- **背面剔除**：`glEnable(GL_CULL_FACE)` + `glCullFace(GL_BACK)`，零成本翻倍 fragment 吞吐
- **实例缓冲复用**：`instanceBuffer` float[] 只扩容不重分配，批量上传复用 `vpBuffer` float[16]
- **合批去记录化**：`groupCommands` 由 `BatchKey` record 改为 `long` hash key，每帧省掉 N×record 对象分配
- **GPU 上传优化**：`GLMesh.uploadInstanceData` 首帧 `glBufferData`，后续同容量 `glBufferSubData`，避免驱动 VBO 重分配
- **场景双缓冲清理**：`SystemScheduler.update()` swap 后对 `sceneBack.clear()`，避免 RenderCommand 逐帧累积

#### SystemScheduler

- **`additionalRenderTask` 注入点**：在 `frame.render()` 之后、`swapBuffers()` 之前执行外部 Renderable（ImGui、HUD 等），解决 ImGui 渲染到错误 buffer 被 `glClear` 覆盖的问题

#### 日志 & 错误处理

- **EngineBoot**：启动/就绪 INFO 日志（mode、entity capacity、FOV、aspect）
- **FoolsEngine**：构造全步骤 DEBUG 日志（Managers、Window、Systems、RenderFrame、Scheduler）
- **SystemManager**：系统注册成功 DEBUG 日志 + 失败 ERROR 含异常信息

#### Bug 修复

- **末尾实体销毁保留残留签名**：`EntityManager.destroyEntity` 销毁末尾实体时不调用 `entitySignatureChanged` 导致 entity ID 残留在系统集合中——SceneCollector 增量 diff 无法检测光源删除，新建同 ID 光源被跳过；修复为末尾实体传入空 `Signature` 触发系统移除
- **非末尾实体销毁签名空指针**：销毁倒数第二个实体时因前一个销毁已把末尾签名置 null，swap 到 null 触发 NPE；修复为统一走 `entitySignatureChanged` 显式调用链
- **背面剔除缺失**：`GLRenderFrame.init()` 未启用 `GL_CULL_FACE`，正反面双重 fragment 计算；添加 `glEnable(GL_CULL_FACE)` + `glCullFace(GL_BACK)`

#### 测试

- **TesECSRenderFlow**：适配新 API（`getPosition()`/`getRotation()`、`SceneCollector`、`additionalRenderTask`）

#### 引擎架构

- **EngineBoot 入口点**：工厂方法 `EngineBoot.create()` 替代 `FoolsEngine.create()`；内置 `ValidatingLoader` 自定义 ClassLoader 和 `validateSystems()` 注解校验
- **`FoolsEngine` 去工厂化**：改为纯引擎实例，构造函数接收 `isServer` 参数；自动注册 ClientSystem 并创建 `SystemScheduler`（客户端）或 headless 调度器（服务端）
- **`@OnlyIn` 运行时注解**：`@OnlyIn(Distribution.Client)` / `Dedicated_Server`，搭配 `EngineBoot.validateSystems()` 阻止服务端注册客户端专用系统

#### ECS 系统层

- **泛型 System<Context> 体系**：`System<Context>` → `ClientSystem<Context>`（渲染侧）/ `ServerSystem<Context>`（逻辑侧），调度器按 `instanceof` 分两组运行
- **ECS ↔ 渲染对接**：`CameraCollector`、`RenderableCollector`、`LightCollector` 均改为 `ClientSystem<RenderScene>`，通过 `scene.setCamera/submit/setLighting` 取代已弃用的 `frame.setCamera/submit`
- **LightCollector 实现**：ECS Light 组件 → API Light 对象转换，增量式增删跟踪，避免每帧重复添加
- **SparseSet 线程安全**：每个实例独立 `ReentrantReadWriteLock`；修正 iterator 中 `sparseArray[index]` → `dense_component[index]` 的取值 bug

#### 渲染 & 调度

- **SystemScheduler 重写**：
  - ServerSystem 60Hz 固定步长累加器，ClientSystem 每帧可变间隔
  - RenderScene 双缓冲（front/back），逻辑写 back → swap → 渲染读 front
  - 并行双线程：逻辑线程（主线程）+ 渲染线程（`RenderThreadPool.renderMain`，含未捕获异常处理器）
  - 无参构造 `new SystemScheduler()` = headless 模式（服务端），跳过渲染
- **GraphicsContext 接口**：`makeCurrent/releaseCurrent/swapBuffers/pollEvents/shouldClose/nativeHandle`，零外部依赖，`GLWindow` 实现，`GLFWKeyBoard/GLFWMouse` 内部消费
- **InputManager 去平台耦合**：`InputDevice<Window>` → `InputDevice<?>`，`getDevice(Class<T>)` 泛型查找

#### 阴影 & Shader

- **平行光远端退化**：片元着色器增加 `proj.z < 0` 守卫，远平面外片元直接 `shadow=1.0`（无影光照）
- **视锥体裁剪 padding 增大**：`spanZ × 0.2 → 0.4`，`adaptivePadXY × 0.5 → 0.15`，新增 `DIR_SHADOW_Z_NEAR_PAD`（30f）和 `DIR_SHADOW_Z_FAR_PAD`（30f）独立控制近/远平面余量

#### 工具

- **Logger 改实例化**：每模块创建独立 Logger 实例；格式 `[HH:mm:ss.SSS][LEVEL][name]<msg>`；无参构造通过 `StackWalker` 自动推导调用者缩写类名（如 `co.me.fo.co.FoolsEngine`）

#### 测试

- **TesECSRenderFlow**：新增基于 `TestLightBackend` 的 ECS + SystemScheduler 集成测试，演示 `scheduler.update()` 驱动全渲染管线

#### 清理

- `FoolsEngine` 移除 Factory 方法和 scheduler 字段
- `RenderableCollector`、`CameraCollector`、`LightCollector` 移除无意义 `@Deprecated`
- `SystemScheduler` 不再持有 `FoolsEngine` 引用
- `InputManager` 移除 `registerKeyboard/registerMouse` 的 `InputDevice<Window>` 平台耦合
- `FoolsEngineKeyCode` 枚举命名保持（独立于引擎实例）

### 0.1.0 — 2026-07-24

#### 引擎架构

- **EngineBoot 入口点**：工厂方法 `EngineBoot.create()` 替代 `FoolsEngine.create()`；内置 `ValidatingLoader` 自定义 ClassLoader 和 `validateSystems()` 注解校验
- **`FoolsEngine` 去工厂化**：改为纯引擎实例，构造函数接收 `isServer` 参数；自动注册 ClientSystem 并创建 `SystemScheduler`（客户端）或 headless 调度器（服务端）
- **`@OnlyIn` 运行时注解**：`@OnlyIn(Distribution.Client)` / `Dedicated_Server`，搭配 `EngineBoot.validateSystems()` 阻止服务端注册客户端专用系统

#### ECS 系统层

- **泛型 System<Context> 体系**：`System<Context>` → `ClientSystem<Context>`（渲染侧）/ `ServerSystem<Context>`（逻辑侧），调度器按 `instanceof` 分两组运行
- **ECS ↔ 渲染对接**：`CameraCollector`、`RenderableCollector`、`LightCollector` 均改为 `ClientSystem<RenderScene>`，通过 `scene.setCamera/submit/setLighting` 取代已弃用的 `frame.setCamera/submit`
- **LightCollector 实现**：ECS Light 组件 → API Light 对象转换，增量式增删跟踪，避免每帧重复添加
- **SparseSet 线程安全**：每个实例独立 `ReentrantReadWriteLock`；修正 iterator 中 `sparseArray[index]` → `dense_component[index]` 的取值 bug

#### 渲染 & 调度

- **SystemScheduler 重写**：
  - ServerSystem 60Hz 固定步长累加器，ClientSystem 每帧可变间隔
  - RenderScene 双缓冲（front/back），逻辑写 back → swap → 渲染读 front
  - 并行双线程：逻辑线程（主线程）+ 渲染线程（`RenderThreadPool.renderMain`，含未捕获异常处理器）
  - 无参构造 `new SystemScheduler()` = headless 模式（服务端），跳过渲染
- **GraphicsContext 接口**：`makeCurrent/releaseCurrent/swapBuffers/pollEvents/shouldClose/nativeHandle`，零外部依赖，`GLWindow` 实现，`GLFWKeyBoard/GLFWMouse` 内部消费
- **InputManager 去平台耦合**：`InputDevice<Window>` → `InputDevice<?>`，`getDevice(Class<T>)` 泛型查找

#### 阴影 & Shader

- **平行光远端退化**：片元着色器增加 `proj.z < 0` 守卫，远平面外片元直接 `shadow=1.0`（无影光照）
- **视锥体裁剪 padding 增大**：`spanZ × 0.2 → 0.4`，`adaptivePadXY × 0.5 → 0.15`，新增 `DIR_SHADOW_Z_NEAR_PAD`（30f）和 `DIR_SHADOW_Z_FAR_PAD`（30f）独立控制近/远平面余量

#### 工具

- **Logger 改实例化**：每模块创建独立 Logger 实例；格式 `[HH:mm:ss.SSS][LEVEL][name]<msg>`；无参构造通过 `StackWalker` 自动推导调用者缩写类名（如 `co.me.fo.co.FoolsEngine`）

#### 测试

- **TesECSRenderFlow**：新增基于 `TestLightBackend` 的 ECS + SystemScheduler 集成测试，演示 `scheduler.update()` 驱动全渲染管线

#### 清理

- `FoolsEngine` 移除 Factory 方法和 scheduler 字段
- `RenderableCollector`、`CameraCollector`、`LightCollector` 移除无意义 `@Deprecated`
- `SystemScheduler` 不再持有 `FoolsEngine` 引用
- `InputManager` 移除 `registerKeyboard/registerMouse` 的 `InputDevice<Window>` 平台耦合
- `FoolsEngineKeyCode` 枚举命名保持（独立于引擎实例）


## foolsEngine 更新日志

### 0.0.9 — 2026-07-21

---

#### Bug 修复

- **`InternalFactoryStub.VulkanINSTANCE()` 返回错误实例** — 复制粘贴错误导致返回 `OpenGLINSTANCE` 而非 `VulkanINSTANCE`，影响 `ServiceFactory` 中 10 处 Vulkan 后端分发
- **Shader PCF 阴影除数错误** — `main_fsh.glsl` 5×5=25 采样点除以 `9.0` 而非 `25.0`，阴影亮度过高
- **`GLTexture.upload()` 内存泄漏** — `MemoryUtil.memAlloc()` 分配的中间缓冲区未释放，每次纹理上传泄漏 off-heap 内存
- **`GLArrayTexture.getImage()` 编译错误** — `return manager.get` 引用不存在的字段，此前构建走缓存未暴露
- **`Camera.vp()` 缓存永不命中** — JOML `Matrix4f.equals()` 按引用比较，改为 `equals(Matrix4fc, float)` 按值比较

#### ShadowManager 方向光阴影算法重写

- **多层视锥深度采样** — `FRUSTUM_DEPTH_SAMPLES=4`，对 NDC 深度 4 层 × 4 角 = 16 个采样点（原 8 个仅近+远平面），解决高空俯瞰时视锥内部几何体被阴影裁切的问题
- **自适应 Padding** — XY padding 与 Z padding 按视锥实际跨度动态缩放（基准 10%/20%），替代写死的 ±15/±30 常量
- **消除中间世界空间 AABB** — NDC→世界→光空间一步完成，不再存储 `worldCorners[8]`

#### JOML 零分配热路径

- **`ShadowManager`** — 15+ 个缓冲区（ndcCorners, frustumCenter, invVP, lightView, tmpVec4 等）提升为类字段，每帧用 `.set()` 复用替代 `new`
- **`Camera.vp()`** — 3 个 `Matrix4f` 字段预先初始化，`set()` 替代 `new Matrix4f(...)`
- **`CameraCollector`** — `conjugate(new Quaternionf())` 改为复用字段

#### TextureManager 重设计

- **Free-list 池** — 构造时预填充所有可用层（1..maxLayers-1），移除 `nextLayer` 水位线
- **GPU 层清零** — `freeLayer()` 调用 `glTexSubImage3D` 上传零缓冲区，释放层在 GPU 上被透明黑色覆盖
- **纹理追踪** — `HashMap<Integer, GLArrayTexture>` 记录所有活跃纹理，`getTexture(int)` / `getTextures()` 实际可用
- **`GLArrayTexture` 存储 `LoadedImage`** — 上传时保留像素数据，`getImage()` 返回有效数据，`destroy()` 自动释放
- **覆盖安全** — 向占用层重新上传时自动关闭旧 `LoadedImage`
- **`LoadedImage`** — `close()` 重命名为 `free()`，完善 Javadoc

#### API 文档

- **`TextureManager` / `Texture` / `LoadedImage`** — 完整 Javadoc（类级说明、全部 @param/@return/@see）

#### 项目维护

- **OpenGL 上下文** — `GLFWWindowsManager` 请求 4.3 Core Profile，移除 `GLFW_OPENGL_FORWARD_COMPAT`（曾在 Windows 上导致 `GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT`）

### 0.0.8 — 2026-06-22

---

#### ImGui 字符输入支持

- **`glfwSetCharCallback` 回调** — `GLFWKeyBoard.attachEnvironment()` 安装字符回调，接收 Unicode 码点后转发至 ImGui
- **`ImGuiHelper.addInputCharacter(int)`** — 新增字符转发方法，内部调用 `ImGui.getIO().addInputCharacter(codepoint)`
- **修复 ImGui 文本框无法输入字符** — 此前仅转发修饰键（Shift/Ctrl/Alt/Super），缺失的 `glfwSetCharCallback` 导致 ImGui 文本控件收不到任何字符数据

### 0.0.7 — 2026-06-22

---

#### Input 多行为同键绑定 + NULL 哨兵键码

- **ActionMapping 重构** — 同一按键支持绑定多个 Action（内部 value 从 `Action` 改为 `List<Action>`），多个不冲突行为共享同一按键不再相互覆盖
- **反向映射自动解绑** — 新增 `Map<Action, FoolsEngineKeyCode>` 反向映射；`bind()` 绑定新按键时自动从该 Action 的旧按键列表中移除
- **FoolsEngineKeyCode.NULL** — 哨兵键码（id=-1），表示"未绑定"；`beginFrame()` 自动跳过 NULL 按键
- **InputManager.beginFrame() 双层遍历** — 按键 → Action 列表，每个 Action 独立处理输入状态

### 0.0.6 — 2026-06-17

---

#### 截屏 API

- **RenderFrame 新增 `screenShot` 方法** — 三个重载：
  - `screenShot(ByteBuffer dstBuf)` — 默认帧缓冲原始 RGBA 像素写入用户提供的 ByteBuffer
  - `screenShot(Path path)` — 默认帧缓冲内容保存为 PNG 文件（自动上下翻转）
  - `screenShot(RenderTarget target)` — 将默认帧缓冲通过 `glBlitFramebuffer` 拷贝至指定 TARGET_COLOR 类型的 RenderTarget
- **GLRenderFrame 实现** — `screenShot(ByteBuffer)` 直接 `glReadPixels`；`screenShot(Path)` 经 `glReadPixels` + CPU 垂直翻转后通过 STBImageWrite 写出 PNG；`screenShot(RenderTarget)` 使用 `glBlitFramebuffer(GL_LINEAR)` 从默认帧缓冲 blit 到目标

### 0.0.5 — 2026-06-04

---

#### 阴影系统重构

- **统一阴影相机更新** — `ShadowManager.updateDirShadowCamera()` 接管原 `Light.buildDirLightShadowCam()` 逻辑，新增 `updateSpotShadowCamera()`，`prepareShadow()` 简化为统一更新 + VP 矩阵同步
- **`Light.buildDirLightShadowCam()` 标记 @Deprecated** — 保留旧方法体向后兼容
- **ShadowManager 所有权移入 LightEnvironment** — `enableShadows()` 创建 ShadowManager，`enableDirLightShadow()`/`enableSpotLightShadow()` 委托创建，`clear()` 自动 `reset()`，`destroy()` 释放资源
- **`RenderFrame.setShadowManager()` 标记 @Deprecated** — 改为每帧从 `scene.getLighting().getShadowManager()` 动态获取
- **按实例释放阴影层** — ShadowManager 新增 free-list (`Set<Integer> releasedLayers`) + `releaseLayer(int)`，LightEnvironment.remove() 自动回收

#### ImGui 惰性加载

- **新增 `util/ImGuiHelper.java`** — Class.forName 守卫 + 私有 ImGuiInternal 内部类，imgui 缺失时全部转发方法空操作
- **GLFWKeyBoard / GLFWMouse / GLWindow** 改为通过 ImGuiHelper 转发，不再直接导入 `imgui.ImGui`
- **build.gradle.kts** — imgui-java 从 `api` 改为 `compileOnly`，测试层 `testImplementation`

#### Texture 纹理数组系统

- **Texture.java 扩展** — `belongsTo()`→null 和 `getLayer()`→-1 default 方法，旧 GLTexture 零改动
- **新增 `TextureManager.java` API 接口** — upload / getPlaceholder / releaseLayer / flushMipmaps / bind / destroy
- **新增 `GLTextureManager.java` 实现** — `glTexStorage3D` + `glTexSubImage3D`，free-list 层分配，mipmap dirty flag，1×1 白色占位符（Layer 0）
- **新增 `GLArrayTexture.java`** — 轻量 Texture 代理实现，绑定到 TextureManager 层
- **GLRenderFrame 集成** — renderCommands 中 `belongsTo()` 分叉（null → TextureBinder slot 0-7，非null → 固定 slot 9 + textureLayer uniform）；render() 帧首 flushMipmaps
- **main_fsh.glsl** — 新增 `sampler2DArray textureArray` + `int textureLayer` 分支
- **RenderScene** — 新增 `textureManager` 字段
- **Factory 链** — APIFactory / InternalFactoryStub / GLInternalFactory / ServiceFactory 全部注册 `createTextureManager()`

#### LoadMode / WrapMode — 纹理加载健壮性

- **新增 `util/LoadMode.java`** — `STRETCH`（nearest-neighbor 拉伸）/ `CROP_WRAP`（裁剪+WrapMode填充）/ `STRICT`（尺寸不匹配抛异常）
- **新增 `util/WrapMode.java`** — `CLAMP_TO_BORDER` / `REPEAT` / `MIRRORED_REPEAT` / `CLAMP_TO_EDGE`
- **GLTextureManager 完整 CPU 端尺寸处理** — STRETCH 按比例缩放，CROP_WRAP 过小按 WrapMode 平铺填充（透明黑/重复/镜像/边缘延伸），过大仅取左上角
- **API 增强** — upload 新增 `(Path)`、`(Path, LoadMode)`、`(Path, LoadMode, WrapMode)`、`(Path, int, LoadMode, WrapMode)` 重载，默认 STRETCH 向后兼容

#### 许可证

- **添加 GPL v3 LICENSE 文件**
- **65 个源文件添加版权头**（api/ 27 + util/ 21 + core/ 17）
- **README.md / README_zh.md** License 章节更新

#### 杂项

- **Logger + LogLevel** — 5 级内置日志（TRACE/DEBUG/INFO/WARN/ERROR）
- **InputState.clearSignalCache()** — 修复 isPressed 粘滞 bug
- **InputManager 工厂模式** — `<E> createInputManager(E env)` 泛型创建，api/test 层不引用具体后端类型
- **build.gradle.kts fatJar 任务**
- **TestLightBackend** — TextureManager 示例，隔列交替材质，ImGui 调试面板（C 键切换）

### 0.0.2 — 2026-06-03

---

- 添加 JavaDoc 注释到源文件
- 将光标模式管理职责从 InputDevice 移至 Window

### 0.0.1 — 2026-06-03

---

- 初始预览版本

## foolsEngine Changelog

### 0.1.1 — 2026-07-24

#### ECS Layer

- **SceneCollector merge** — `CameraCollector`, `LightCollector`, `RenderableCollector` merged into a single system collecting cameras, lights (including shadows), and renderables in one pass; old collectors marked `@Deprecated`
- **Signature matching fix** — `SystemManager.entitySignatureChanged` inverted check `systemSig.includes(entitySignature)` caused false matches when entity was missing components; fixed to `entitySignature.matches(systemSig)`
- **Transform encapsulation** — `position`/`rotation`/`scale` made private with getters (`getPosition()`/`getRotation()`/`getScale()`) and convenience setters (`position()`/`rotation()`/`scale()`) that auto-`markDirty()`
- **Transform matrix decomposition** — `setFromMatrix()` now calls `decompose()` to extract translation, rotation (column-normalized→quaternion), and scale from any 4×4 model matrix; mirroring support via negative scale detection
- **ClientSystem decoupling** — constructor changed to `super(engine, null)`, no longer depends on `engine.systemScheduler.getScene()`; `RenderScene` passed via `update(dt, scene)` parameter each frame
- **ECS Light component extended** — added `shadowNear`, `intensity`, `castsShadow` fields for automatic shadow light collection
- **EntityFactory** — `createLightEntity()` now also binds `Transform` component for unified SceneCollector matching

#### Rendering Pipeline

- **Backface culling** — `glEnable(GL_CULL_FACE)` + `glCullFace(GL_BACK)`, zero-cost doubling of fragment throughput
- **Instance buffer reuse** — `instanceBuffer` float[] only grows when capacity exceeded; `vpBuffer` float[16] reused per batch
- **GroupCommands de-recorded** — `BatchKey` record replaced with `long` hash key, saving N×record allocations per frame
- **GPU upload optimization** — `GLMesh.uploadInstanceData` uses `glBufferData` on first/resize, `glBufferSubData` on same size to avoid driver VBO reallocation
- **Scene double-buffer cleanup** — `sceneBack.clear()` after swap in `SystemScheduler.update()`, prevents RenderCommand accumulation across frames

#### SystemScheduler

- **`additionalRenderTask` hook** — optionally executes external Renderables (ImGui, HUD) between `frame.render()` and `swapBuffers()`, fixing ImGui rendering on wrong buffer causing `glClear` wipe

#### Logging & Error Handling

- **EngineBoot** — startup/ready INFO logs (mode, entity capacity, FOV, aspect)
- **FoolsEngine** — per-step DEBUG logs during construction (Managers, Window, Systems, RenderFrame, Scheduler)
- **SystemManager** — system registration DEBUG log + error details on failure

#### Bug Fixes

- **Last-entity destruction retaining stale signature** — `EntityManager.destroyEntity` didn't call `entitySignatureChanged` for last entity, leaving entity ID in system entity sets; SceneCollector diff couldn't detect light removal, causing new entities with same ID to be skipped. Fixed by passing empty `Signature` to trigger system removal
- **Non-last entity destroy NPE** — destroying second-to-last entity after prior destroy had nullified the last signature caused swap-to-null → NPE; fixed with unified `entitySignatureChanged` explicit call chain
- **Missing backface culling** — `GLRenderFrame.init()` never enabled `GL_CULL_FACE`, doubling fragment workload; added `glEnable(GL_CULL_FACE)` + `glCullFace(GL_BACK)`

#### Test

- **TesECSRenderFlow** — adapted to new API (`getPosition()`/`getRotation()`, `SceneCollector`, `additionalRenderTask`)

### 0.0.9 — 2026-07-21

---

#### Bug Fixes

- **`InternalFactoryStub.VulkanINSTANCE()` returned wrong instance** — copy-paste bug caused OpenGL instance to be returned for Vulkan path, affecting 10 dispatch points in `ServiceFactory`
- **Shader PCF shadow divisor was wrong** — `main_fsh.glsl` divided 5×5=25 sample sum by `9.0` instead of `25.0`, making shadows too bright
- **`GLTexture.upload()` memory leak** — `MemoryUtil.memAlloc()` intermediate ByteBuffer was never freed, leaking off-heap memory on every upload
- **`GLArrayTexture.getImage()` compile error** — `return manager.get` referenced non-existent field; previously hidden by Gradle build cache
- **`Camera.vp()` cache never hit** — JOML `Matrix4f.equals()` uses reference equality; switched to `equals(Matrix4fc, float)` with epsilon

#### ShadowManager Directional Shadow Algorithm Rewrite

- **Multi-layer frustum depth sampling** — `FRUSTUM_DEPTH_SAMPLES=4`, sampling 4 depth layers × 4 corners = 16 points (was 8: near+far only). Fixes shadow cutoff artifacts on interior frustum geometry (e.g. high-altitude cameras looking down)
- **Adaptive padding** — XY and Z padding now scale with actual frustum extent (10%/20% base), replacing hardcoded ±15/±30 constants
- **Eliminated intermediate world-space AABB** — NDC→world→light in a single pass; `worldCorners[8]` storage removed

#### JOML Zero-Allocation Hot Paths

- **`ShadowManager`** — 15+ reusable buffers (ndcCorners, frustumCenter, invVP, lightView, tmpVec4, etc.) promoted to instance fields, `.set()` replaces per-frame `new`
- **`Camera.vp()`** — 3 `Matrix4f` fields pre-allocated; `set()` replaces `new Matrix4f(...)`
- **`CameraCollector`** — `conjugate(new Quaternionf())` replaced with reusable field

#### TextureManager Redesign

- **Free-list pool** — all available layers pre-populated at construction (1..maxLayers-1); `nextLayer` watermark removed
- **GPU layer zeroing** — `freeLayer()` calls `glTexSubImage3D` with zero-buffer before returning the slot to the pool
- **Texture tracking** — `HashMap<Integer, GLArrayTexture>` records all active textures; `getTexture(int)` / `getTextures()` actually functional
- **`GLArrayTexture` stores `LoadedImage`** — pixel data preserved at upload time; `getImage()` returns valid data; `destroy()` auto-frees
- **Overwrite-safe** — re-uploading to an occupied layer frees the old `LoadedImage` first
- **`LoadedImage`** — `close()` renamed to `free()`; full Javadoc

#### API Documentation

- **`TextureManager` / `Texture` / `LoadedImage`** — complete Javadoc (class-level descriptions, all @param/@return/@see)

#### Project Maintenance

- **OpenGL context** — `GLFWWindowsManager` requests 4.3 Core Profile; `GLFW_OPENGL_FORWARD_COMPAT` removed (caused `GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT` on Windows)

### 0.0.8 — 2026-06-22

---

#### ImGui Character Input Support

- **`glfwSetCharCallback` installed** — `GLFWKeyBoard.attachEnvironment()` installs a char callback, forwarding Unicode codepoints to ImGui
- **`ImGuiHelper.addInputCharacter(int)`** — new character forwarding method, internally calls `ImGui.getIO().addInputCharacter(codepoint)`
- **Fixed ImGui text fields not accepting typed characters** — previously only modifier keys (Shift/Ctrl/Alt/Super) were forwarded; the missing `glfwSetCharCallback` prevented ImGui text widgets from receiving any character data

### 0.0.7 — 2026-06-22

---

#### Input Multi-Action Per-Key Binding + NULL Sentinel

- **ActionMapping refactor** — supports multiple Actions bound to the same key (inner value changed from `Action` to `List<Action>`); non-conflicting Actions sharing a key no longer overwrite each other
- **Reverse map auto-unbind** — new `Map<Action, FoolsEngineKeyCode>` reverse map; `bind()` automatically removes the Action from its previous key's list when rebinding
- **FoolsEngineKeyCode.NULL** — sentinel key code (id=-1), representing "unbound"; `beginFrame()` auto-skips NULL key
- **InputManager.beginFrame() double-loop** — key → list of Actions, each Action independently processes input state

### 0.0.6 — 2026-06-17

---

#### Screenshot API

- **RenderFrame adds `screenShot` methods** — three overloads:
  - `screenShot(ByteBuffer dstBuf)` — copies default framebuffer raw RGBA pixels to user-supplied ByteBuffer
  - `screenShot(Path path)` — saves default framebuffer content as a PNG file (auto-flipped vertically)
  - `screenShot(RenderTarget target)` — blits the default framebuffer into a TARGET_COLOR RenderTarget via `glBlitFramebuffer`
- **GLRenderFrame implementation** — `screenShot(ByteBuffer)` uses direct `glReadPixels`; `screenShot(Path)` uses `glReadPixels` + CPU vertical flip + STBImageWrite PNG output; `screenShot(RenderTarget)` uses `glBlitFramebuffer(GL_LINEAR)` from default framebuffer to target

### 0.0.5 — 2026-06-04

---

#### Shadow System Refactor

- **Unified shadow camera updates** — `ShadowManager.updateDirShadowCamera()` absorbs the original `Light.buildDirLightShadowCam()` logic; added `updateSpotShadowCamera()`; `prepareShadow()` simplified to unified update + VP sync
- **`Light.buildDirLightShadowCam()` marked @Deprecated** — old method body retained for backward compatibility
- **ShadowManager ownership moved into LightEnvironment** — `enableShadows()` creates ShadowManager; `enableDirLightShadow()`/`enableSpotLightShadow()` delegate; `clear()` auto-`reset()`; `destroy()` releases resources
- **`RenderFrame.setShadowManager()` marked @Deprecated** — reads from `scene.getLighting().getShadowManager()` per frame instead
- **Per-instance layer release** — ShadowManager free-list (`Set<Integer> releasedLayers`) + `releaseLayer(int)`; LightEnvironment.remove() auto-reclaims layers

#### ImGui Lazy-Loading

- **New `util/ImGuiHelper.java`** — Class.forName guard + private ImGuiInternal inner class; all forwarding methods no-op when imgui is absent
- **GLFWKeyBoard / GLFWMouse / GLWindow** now forward through ImGuiHelper, without directly importing `imgui.ImGui`
- **build.gradle.kts** — imgui-java changed from `api` to `compileOnly`; `testImplementation` at test layer

#### Texture Array System

- **Texture.java extension** — `belongsTo()`→null and `getLayer()`→-1 default methods; existing GLTexture requires zero changes
- **New `TextureManager.java` API interface** — upload / getPlaceholder / releaseLayer / flushMipmaps / bind / destroy
- **New `GLTextureManager.java` implementation** — `glTexStorage3D` + `glTexSubImage3D`; free-list layer allocation; mipmap dirty flag; 1×1 white placeholder (Layer 0)
- **New `GLArrayTexture.java`** — lightweight Texture proxy bound to a TextureManager layer
- **GLRenderFrame integration** — `belongsTo()` branch in renderCommands (null → TextureBinder slot 0-7, non-null → fixed slot 9 + textureLayer uniform); flushMipmaps at frame start in render()
- **main_fsh.glsl** — added `sampler2DArray textureArray` + `int textureLayer` branch
- **RenderScene** — added `textureManager` field
- **Factory chain** — APIFactory / InternalFactoryStub / GLInternalFactory / ServiceFactory all register `createTextureManager()`

#### LoadMode / WrapMode — Resilient Texture Upload

- **New `util/LoadMode.java`** — `STRETCH` (nearest-neighbor resize) / `CROP_WRAP` (crop + WrapMode padding) / `STRICT` (throw on size mismatch)
- **New `util/WrapMode.java`** — `CLAMP_TO_BORDER` / `REPEAT` / `MIRRORED_REPEAT` / `CLAMP_TO_EDGE`
- **Full CPU-side size handling in GLTextureManager** — STRETCH proportionally rescales; CROP_WRAP fills undersized images by WrapMode tiling (transparent black/repeat/mirrored/edge-extend); oversized images take top-left crop only
- **API enhancement** — upload overloads: `(Path)`, `(Path, LoadMode)`, `(Path, LoadMode, WrapMode)`, `(Path, int, LoadMode, WrapMode)`; defaults to STRETCH for backward compatibility

#### License

- **Added GPL v3 LICENSE file**
- **Copyright headers added to 65 source files** (api/ 27 + util/ 21 + core/ 17)
- **README.md / README_zh.md** license section updated

#### Misc

- **Logger + LogLevel** — 5-level built-in logging (TRACE/DEBUG/INFO/WARN/ERROR)
- **InputState.clearSignalCache()** — fixed sticky isPressed bug
- **InputManager factory pattern** — `<E> createInputManager(E env)` generic creation; api/test layers reference no concrete backend types
- **build.gradle.kts fatJar task**
- **TestLightBackend** — TextureManager example, alternating materials per column, ImGui debug overlay (C key toggle)

### 0.0.2 — 2026-06-03

---

- Added JavaDoc comments to source files
- Moved cursor mode responsibility from InputDevice to Window

### 0.0.1 — 2026-06-03

---

- Initial preview release
