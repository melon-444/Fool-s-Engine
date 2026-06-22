## foolsEngine 更新日志

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
