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
