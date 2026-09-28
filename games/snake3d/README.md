# Snake3D

**[English](#english)** | **[中文](#chinese)**

一个基于 foolsEngine 的迷你 3D 贪吃蛇 demo，用于验证引擎闭环玩法。地图是一个 3D 立方体（12³），全部画面由**程序生成的单位立方体**（纯色、无纹理、无外部模型）拼成。

## 运行

```bash
# 在 foolsEngine 项目根目录下
./gradlew.bat runSnakeGame
```

## 操作

| 按键 | 功能 |
|---|---|
| W / S / A / D | 水平转向（前/后/左/右） |
| Q / E | 上升 / 下降 |
| 鼠标移动 | 旋转视角（环绕中心跟随蛇头，带平滑） |
| 鼠标滚轮 | 拉近 / 拉远摄像机 |
| 鼠标右键 | 切换光标模式（锁定 / 释放） |
| R | 重新开始 |
| F / G | 增加 / 减少果子数量（最少 1 个） |
| P / O | 加速 / 减速 |
| ESC | 退出 |

> 注意：引擎的输入系统一个按键只绑定一个 action。多个按键实现同一功能需要注册多个 action 再取或，不能把同一个 action 绑到多个按键上（后绑定会顶掉前一个）。

## 玩法闭环

吃红色方块 → 变长 + 加分 + 加速 → 撞自己 → 游戏结束 → 按 R 重来。

蛇身在两格之间平滑插值滑动；穿越立方体某一面时通过发光传送门绕到对侧，并在穿越点触发着色器生成的漩涡粒子特效（漩涡面与行进方向垂直，蛇身离开后自动消失）。

轨道摄像机的环绕中心平滑跟随蛇头（穿越边界的跳变会被平滑掉），滚轮控制远近。整块空间铺满一层半透明网格框架，默认很暗；只有蛇头周围约 1/5 地图边长的球体范围内，网格边框才会变亮（着色器按到蛇头的距离计算）。

## 素材需求

**当前（占位符）无需任何外部素材** —— 所有几何体由 `MeshFactory` 生成，颜色由 `flat` shader 的 `uColor` uniform 提供。

若想升级成正式画面，替换/补充以下素材即可（纯占位 → 成品）：

| 用途 | 现状占位 | 需要的正式素材 |
|---|---|---|
| 蛇身/蛇头 | 绿色立方体 | 鳞片贴图立方体、蛇头模型（.obj/.gltf） |
| 食物 | 红色立方体 | 苹果/金币模型或带贴图立方体 |
| 立方体边界 | 发光线框 | 可保留，或换成星空/六面传送门贴图 |
| 音效/音乐 | 无 | 吃食物、死亡音效（引擎目前无音频后端，需自行接入） |

---

# Snake3D <a id="english"></a>

A tiny 3D Snake demo on foolsEngine, proving a closed gameplay loop. The map is a 3D cube (12³); all visuals are **procedurally generated unit cubes** (solid colors, no textures, no external models).

## Run

```bash
# from the foolsEngine project root
./gradlew.bat runSnakeGame
```

## Controls

| Key | Action |
|---|---|
| W / S / A / D | Move horizontally (forward/back/left/right) |
| Q / E | Move up / down |
| Mouse | Orbit the camera (center smoothly follows the head) |
| Scroll wheel | Zoom in / out |
| Right mouse | Toggle cursor mode (locked / free) |
| R | Restart |
| F / G | Increase / decrease food count (min 1) |
| P / O | Speed up / slow down |
| ESC | Quit |

> Note: the engine input system binds exactly one action per key. To make several keys do the same thing, register separate actions and OR them — binding the same action to two keys replaces the first.

## Game loop

Eat red cube → grow + score + speed up → hit yourself → game over → press R to restart.

The snake glides smoothly between grid cells. Crossing a face of the cube wraps to the opposite face through a glowing portal, which erupts with a shader-driven swirl oriented perpendicular to travel; the swirl disappears once the snake leaves.

The orbit camera's center smoothly follows the head (softening the wrap jump) and the scroll wheel zooms. A translucent grid fills the whole cube but stays dark except within ~1/5 of the map size around the head, where the grid edges light up (distance computed in the shader).

## Asset requirements

**None required right now** — geometry comes from `MeshFactory`, colors from the `flat` shader's `uColor` uniform.

To upgrade from placeholders to real art, provide (placeholder → final):

| Use | Placeholder | Real asset needed |
|---|---|---|
| Snake body/head | green cubes | scaled textured cube, snake-head model (.obj/.gltf) |
| Food | red cube | apple/coin model or textured cube |
| Cube boundary | glowing wireframe | keep, or replace with a starfield / six portal-face texture |
| Sound/music | none | eat/death SFX (engine has no audio backend yet) |
