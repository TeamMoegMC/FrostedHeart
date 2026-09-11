# 白幕 V2 电影级渲染工程计划

- Time: `2026-08-24 05:08:24 +08:00`
- Last revised: `2026-08-24 06:48:26 +08:00`
- Authors: `Codex; OpenAI GPT-5; primary architecture planning`、`Codex subagent; OpenAI gpt-5.6-sol ultra; independent performance review`
- Status: `ready`
- Scope: `content.climate.render`、客户端 shader 与天气配置、天气渲染事件和兼容 Mixin；不改变服务端气候权威、存档或网络协议
- Related: [`V1 服务端低开销空间渲染计划`](2026-08-24_04-25-01_white-curtain-server-efficient-spatial-rendering.md)、[`docs/climate/weather-rendering.md`](../docs/climate/weather-rendering.md)、[`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md)、`ClientWeatherState`、`ClientWeatherFrame`、`WhiteCurtainFieldModel.VisualKernel`、`SpatialWeatherRenderer`、`InfraredViewRenderer`、`IrisRenderingPipelineAccess`、`FHShaders`

## Goal

在 V1 已经建立稀疏 descriptor、客户端逻辑时钟、空间天气场、渲染 ownership 和兼容后端之后，把客户端表现后端升级到有体积、有厚度、有遮蔽关系的电影级白幕。目标画面应让玩家经历“远处地标被风暴吞没、墙体抵近、前锋撞击、核心区低能见度、尾部退去”的完整过程，而不是全屏叠一层白色滤镜。

工程上的最优路径是：

1. V2 只替换客户端渲染后端，复用 V1 的 `ClientWeatherState`、`ClientWeatherFrame` 和 `VisualKernel`。
2. 不增加任何服务端模拟、稳定期 packet、玩法状态或逐玩家视觉计算。
3. 大尺度体积效果在低分辨率目标中计算，以时域重投影恢复稳定细节，再深度感知上采样到主目标。
4. 近景高速雪片、地面 impact、雾、风向和声音继续复用 V1，保持锐利细节并避免体积 pass 承担所有画面工作。
5. V1 空间后端和 Vanilla 兼容后端始终可用。V2 首次发布为 opt-in，只有兼容矩阵和性能门全部通过后才考虑成为默认。

## Version Boundary

| Concern | V1 owns | V2 owns |
|---|---|---|
| 服务端权威 | descriptor、玩法离散采样、持久化、快照触发 | 不新增 |
| 网络 | 低频完整 snapshot，稳定运动 `0 packets/player/second` | 不新增 packet 或字段 |
| 客户端事实 | 单一时钟、snapshot、`VisualKernel[]`、空间网格、可见候选 | 只读消费同一个 `ClientWeatherFrame` |
| 基础表现 | 空间墙、近景雪、地面 impact、连续雾/风/声音 | 有厚度的体积前沿、云底、光照衰减、时域稳定 |
| 默认路径 | `SPATIAL_V1`，失败时 `COMPATIBILITY` | 初始为 `CINEMATIC_V2` opt-in |
| 性能策略 | 固定 Fast/Fancy 工作上限 | 低分辨率、最大 60 Hz 体积更新、异步 GPU 预算控制 |

```text
SERVER, unchanged in V2

WorldClimate + WhiteCurtainDescriptor snapshot
                         |
                         v
CLIENT FACT MODEL, delivered by V1

ClientWeatherState -> ClientWeatherFrame -> bounded visible volume primitives
                                      |
                                      v
                         WeatherRenderCoordinator
                         /          |             \
            COMPATIBILITY    SPATIAL_V1     CINEMATIC_V2
             Vanilla bridge   V1 renderer     low-res volume
                                                + V1 near snow
```

V2 不能引入第二份 descriptor、第二个时钟、第二套近场网格或自己的天气强度状态。若 V2 发现输入不足，应先扩展 `ClientWeatherFrame` 的只读、固定容量 primitive contract，而不是绕过它扫描 snapshot。

## Prerequisites And Handoff Contract

开始 V2 之前，V1 必须满足以下 gate：

1. `WhiteCurtainFieldModel.sampleGameplay` 与现役四方向结果一致，`prepareVisual` 只在 snapshot generation 改变时运行。
2. `ClientWeatherState` 在 client tick 更新，稳定 tick 和 frame 均无 Java allocation。
3. `ClientWeatherFrame` 每帧只冻结一次，并提供 camera sample、ownership、全局天气和有固定上限的可见白幕 primitive view。
4. `SPATIAL_V1` 已经接管近景降雪，且 `COMPATIBILITY` fallback 能在异常、无 level、资源重载和维度切换时恢复。
5. 白幕稳定运动不再依赖逐 tick Vanilla 强度包；服务端和 packet 基线已有可重复 profile。
6. V1 的 Fast/Fancy 视觉语义一致，低档只降低密度，不移除前沿、白化、风向或声音。

V1 应提前固定以下后端边界：

```java
enum WeatherRenderBackendKind {
    COMPATIBILITY,
    SPATIAL_V1,
    CINEMATIC_V2
}

interface WeatherRenderBackend extends AutoCloseable {
    WeatherRenderBackendKind kind();
    void beginFrame(ClientWeatherFrame frame, WeatherRenderContext context);
    void renderAtmosphere(WeatherRenderContext context);
    void renderPrecipitation(WeatherRenderContext context);
    void onResize(int width, int height);
    void onResourceReload();
}
```

方法名可按实现调整，但生命周期不能变成每个 hook 自己探测、自己采样。`beginFrame` 每帧一次；`renderAtmosphere` 用于 opaque world 之后、translucent world 之前的体积合成；`renderPrecipitation` 继续使用 V1 的近景降雪阶段。`close` 必须释放所有 GL 资源。

## Intended Player Experience

### 1. Approach

- 地平线先出现有宽度和行进方向的灰白风暴墙，顶部与压低的云底连接。
- 墙前仍能看见地标；随前沿逼近，远处地标从底部和迎风侧开始被吞没，而不是整个屏幕等比例褪白。
- 天空、日光和环境色逐渐变冷变暗，风声先于密集降雪到达。

### 2. Front Impact

- 玩家进入 V1 已判定的过渡区后，出现一次短促的 squall pulse：横向雪流加速、近景大片雪片增加、能见度迅速收缩、声音冲击增强。
- pulse 只调制已经进入视觉边缘或 gameplay 过渡区的密度，不能让宏观墙体跑赢权威前沿。
- 轮廓仍保留短距离可读性，不能用纯白遮罩完全消除空间感。

### 3. Storm Core

- 云底、白幕体积和近景雪形成前中后三层；玩家转头时体积保持世界空间稳定。
- 建筑、树林和地形按深度被逐层吞没；室内通过天空暴露度降低白化和风声，但窗外仍可见风暴运动。
- 近景雪片保持清晰高速，远景密度由体积 pass 表现，避免用海量透明 quad 伪造厚度。

### 4. Retreat

- 能见度、环境光和风声按相反顺序恢复；尾部仍有间歇雪流，不发生一帧清空历史。
- temporal history 在 snapshot phase 连续变化时保留，在事件结束或前沿拓扑改变时受控衰减。

`WhiteCurtainInfo` 当前为 `300 climate seconds/chunk`，即约 `16 / 300 = 0.053 blocks/second`。V2 的宏观墙体位置必须继续由 `VisualKernel` 和同一气候时钟决定。电影冲击感来自局部密度、风、声音和光照的快速编排，不来自伪造更快的 gameplay 前沿。

## Engineering Budgets

### Server And Network

V2 对服务端和网络的增量预算全部为零：

| Metric | Required target |
|---|---|
| 服务端天气 tick CPU | 相对 V1 `0 ms` 增量 |
| 服务端天气 retained memory | 相对 V1 `0 B` 增量 |
| 稳定天气 packet | 相对 V1 `0 packets/player/second` 增量 |
| snapshot 字段和频率 | 不改变 V1 contract |
| 客户端画质反馈 | 不上传服务端 |

### Client

| Metric | Required target |
|---|---|
| V2 render-thread CPU | P95 目标 `C = min(0.75, 0.08 * 1000 / targetFps) ms`，绝对硬门 `<= 1.0 ms/frame` |
| V2 GPU P95 | 相对同轨迹 `SPATIAL_V1` 的完整增量目标 `G = min(2.0, 0.12 * 1000 / targetFps) ms` |
| V2 GPU P95 hard gate | `H = min(2.5, 0.15 * 1000 / targetFps) ms`；60 Hz 为 `2.5 ms`，144 Hz 约 `1.04 ms` |
| V2 GPU stable-run P99 | `<= min(4.0, 0.25 * 1000 / targetFps) ms`；resize/reload/reset 尖峰单独记录 |
| V1+V2 天气 GPU P95 | 端到端目标 `<= min(2.0, 0.12 * 1000 / targetFps) ms`，硬门 `<= min(2.5, 0.15 * 1000 / targetFps) ms`；V2 可用预算必须扣除同帧已测得的 V1 atmosphere/near-weather 成本 |
| 高刷新率 | raymarch 最多 `60 updates/second`；包含 raymarch 的重帧本身仍须满足对应 FPS 的 P95 预算 |
| 稳定 Java allocation | 预热后 `0 B/frame` 和 `0 B/volume update` |
| V2 增量显存, 1080p | 目标 `<= 20 MiB`，驱动对齐后的硬门 `<= 24 MiB` |
| raymarch 尺寸 | 默认线性 `0.5x`，即约 `1/4` 总像素；禁止全分辨率 raymarch |
| 高分辨率像素上限 | Quality 体积目标 `<= 0.52 Mpixel`；实际尺寸同时受线性比例和像素上限约束 |
| raymarch steps | Quality 最大 `12`，Performance 最大 `8`；允许空区间跳过和透射率 early-out |
| 可见体积 primitive | 默认最多 `4` 个；camera-containing/active/coverage/distance/stable-index 排序；每个使用 screen rect/scissor，禁止每个像素循环全部 primitive |
| 共享 step-sample | 每次 update 的 `sum(scissorPixels_i * steps_i) <= tierPixelCap * tierStepCap`；上限属于全部 primitive，不可乘以 `4` |
| 新增 draws | 常见单前沿 `3` 次、硬上限 `6` 次：`1-4` 个 scissored raymarch + temporal + composite |
| GPU timing | 延迟读取的 query ring；禁止同步 readback、`glFinish` 或等待 query result |

`2.0/2.5 ms` 只适用于 60 Hz：一帧约 `16.67 ms`，对应目标 `12%` 和硬门 `15%`。144 Hz 一帧约 `6.94 ms`，同一公式把目标/硬门收紧到约 `0.83/1.04 ms`；240 Hz 则为约 `0.50/0.625 ms`。V2 增量必须单独记录，但发布门看相同录像、分辨率和 FPS cap 下的完整 V1+V2 天气阶段；控制器的剩余预算为端到端门减去最近完成的 V1 atmosphere/near-weather 成本，不能把 V1 的 `8%` 与 V2 的 `12%` 简单相加成 `20%`。update cap 只降低平均 GPU 工作，不能拿来证明 P95；包含 raymarch 的重帧仍要单独达标。

CPU、GPU、显存、allocation、draw count 和 raymarch update rate 必须分别记录。总 FPS 不能替代归因，平均值不能替代 P95/P99 尖峰。

## What Already Exists

| Existing component | Reuse decision |
|---|---|
| V1 `ClientWeatherState` / `ClientWeatherFrame` / `VisualKernel` | 直接复用，是 V2 唯一天气事实来源 |
| V1 `SpatialWeatherRenderer` | 保留近景雪、ground impact 和兼容渲染；实现 `SPATIAL_V1` 后端 |
| `InfraredViewRenderer` | 复用其 `RenderTarget`、深度纹理、逆矩阵和全屏合成经验；不共享它的静态 target/UBO |
| `IrisRenderingPipelineAccess` | 复用 Oculus `RenderTargets`，按生命周期选择 `noHand` / `noTranslucents` depth provider |
| 根包 `FHShaders` 的 `RegisterShadersEvent` | 注册 V2 core shaders，并对 V2 编译失败做局部降级 |
| `bootstrap.client.FHShaders` reload pattern | 作为资源重载参考；V2 资源由 backend lifecycle 统一失效 |
| `FHClientEvents` 的 `RenderLevelStageEvent` | 优先作为标准入口；只有 stage 顺序不足时才增加窄范围 Mixin anchor |
| `LevelRendererMixin` | 继续作为 precipitation ownership 和 Vanilla fallback 桥 |
| `BlizzardRenderer` | 不复用；其未接入 quad 路径不能提供深度、时域或资源生命周期契约 |

`InfraredViewRenderer` 证明项目可以取得主目标和 Oculus 深度，但 V2 不能直接复制其资源所有权。V2 target 必须在 resize、fullscreen、resource reload、level unload 和 backend downgrade 时显式释放或重建。

## Target Architecture

```text
ClientWeatherFrame, immutable for this display frame
  | camera/projection/history generation
  | whiteout/snow/wind/exposure
  | <= 4 visible GPU volume primitives
  v
WeatherRenderCoordinator
  | select backend only on config/reload/level lifecycle
  | one frame begin, two ordered render stages
  v
CinematicWeatherBackend
  +-- WeatherDepthSource
  |     +-- Vanilla main depth
  |     `-- Oculus RenderTargets provider
  +-- WeatherVolumeTargets
  |     +-- RGBA16F current raymarch
  |     +-- RGBA16F + D32F history ping
  |     +-- RGBA16F + D32F history pong
  |     `-- no full-resolution intermediate
  +-- scissored volume raymarch shader
  |     +-- analytic corridor/front intersection
  |     +-- packed 3D weather noise + blue-noise jitter
  |     +-- cloud ceiling + white-curtain density
  |     `-- approximate single scattering
  +-- temporal resolve shader
  |     +-- previous view-projection
  |     +-- depth/disocclusion rejection
  |     +-- current-neighborhood clamp
  |     `-- writes next history color + gl_FragDepth
  +-- bilateral upscale/composite -> main target
  `-- delegate V1 near precipitation -> SpatialWeatherRenderer
```

### Backend Selection

可配置模式：

```text
COMPATIBILITY       current Vanilla bridge
SPATIAL_V1          V1 wall + spatial precipitation
CINEMATIC_AUTO      V2, asynchronous GPU-budget controller
CINEMATIC_QUALITY   V2 fixed 0.5x / max 12 steps for reproducible QA
CINEMATIC_BALANCED  V2 fixed middle tier
CINEMATIC_PERF      V2 fixed 0.33x / max 8 steps
```

首次发布默认 `SPATIAL_V1`。一个持久化 client config enum 让玩家明确选择 backend 和固定 V2 档位；`CINEMATIC_AUTO` 也是玩家主动选择的模式，只能在 V2 四档内调节，不能自动改成 `SPATIAL_V1`。固定档位不创建 timer query，避免为不会使用的自适应付费。backend 只在客户端配置改变、资源重载、窗口尺寸改变或 level lifecycle 切换时选择。每帧只调用已选 backend，不能每帧扫描 mod、重新编译 shader 或反复探测 Oculus pipeline。

shader 编译失败、FBO 不完整、target 创建失败或未捕获的 render 异常会立即把当前 level session 降级到 `SPATIAL_V1`，停止该 session 的 V2 重试并只记录一次诊断。resize、Oculus target generation 切换、单帧无效矩阵或单帧 depth mismatch 属于可恢复瞬态：只跳过该帧、清 history 并排队重建；同一稳定 generation 连续 `3` 次失败才降级。显式资源重载、配置重新启用或进入新 level 才允许从 session quarantine 重新探测。

`InfraredViewRenderer` 已在 `AFTER_LEVEL` 执行另一个主目标全屏 pass。V2 首版在红外半径大于零时暂停 cinematic atmosphere 并临时使用 `SPATIAL_V1`；红外完全关闭后清空 history，再恢复 V2。首版不尝试叠加两个后处理 owner。

### Depth And Render Ordering

体积大气必须在 opaque world 已产生稳定深度之后、translucent world 和近景天气之前合成。这样玻璃、水、粒子和近景雪可以自然覆盖远景体积，也避免白幕错误覆盖手部。

Phase 0 必须用无 shader、Embeddium 和 Oculus 实测 Forge 各 `RenderLevelStageEvent` 的颜色/深度内容：

1. 优先选择标准 event stage。
2. 若没有同时满足 opaque depth 已完成且 translucent 未开始的 stage，只增加一个窄范围、版本锚点明确的 `LevelRenderer` Mixin。
3. 无 shader 使用当前 main depth；Oculus 使用生命周期中选定的 `RenderTargets` depth provider。
4. provider 对象可每帧读取当前 texture ID，但能力类型和 backend 不做每帧重选。
5. depth 尺寸、矩阵或重建结果无效时，本帧不合成 V2，清 history 并进入 pending rebuild；相同稳定 generation 连续 `3` 次失败才触发 session downgrade。禁止 NaN 传播到主目标。

### Render Targets And Memory

默认 1080p 的 `0.5x` 目标约为 `960 x 540`。目标 scale 使用 `min(profileScale, sqrt(maxWeatherPixels / framebufferPixels))`，并向纹理/viewport 友好的固定像素倍数取整；不能只按窗口比例计算。Quality 的 `0.52 Mpixel` 上限对应 1080p 的 `0.5x` 和 4K 的约 `0.25x`。建议最小布局：

| Target | Format | Count | Approx. 1080p memory |
|---|---|---:|---:|
| current scattering RGB + transmittance | `RGBA16F` | `1` | `~4.0 MiB` |
| scattering RGB + transmittance history | `RGBA16F` | `2` ping-pong | `~7.9 MiB` |
| sampled history depth attachment | `DEPTH_COMPONENT32F` | `2` ping-pong | `~4.0 MiB` |
| blue-noise + packed weather noise | immutable sampled textures | shared | `< 1 MiB` target |
| driver alignment/FBO bookkeeping | measured, not guessed | n/a | remaining budget |

raymarch 写 current color，并输出代表性的 current weather depth（第一次达到消光阈值的位置或有界加权深度），不能把 opaque depth 冒充天气深度。独立 temporal pass 读取 current neighborhood、current weather depth、opaque depth 和旧 history，把结果写到下一 history color/weather-depth attachment。这样天空和平移时也能用天气自身深度做重投影、disocclusion rejection 和 neighborhood clamp，不依赖 MRT 之外的 full-resolution copy。最后只在可见 rect union 加固定 guard band 的范围向 main target做 bilateral composite，不创建 full-resolution weather color target。history-only 帧直接从最近 volume history 重投影并合成，不再无条件执行一次低分辨率 temporal 写入。窗口尺寸改变只标记一次 pending rebuild，在 render thread 的安全点关闭旧 target 后创建新 target。连续 resize 事件合并为最后尺寸，不能每个事件创建一组 FBO。

### Volume Density And Lighting

每个上传 primitive 由 V1 `VisualKernel` 派生固定布局的 world-space 数据：走廊边界、移动轴/横轴、权威前沿位置、边缘宽度、snow/whiteout/wind、云底高度参数和确定性 seed。CPU 以 allocation-free fixed top-K 做视锥、screen rect、scissor 和优先级裁剪，顺序键固定为 camera-containing、active/support-domain、screen coverage、distance、snapshot stable index；每个 scissored draw 只采样一个 primitive。多个前沿按 back-to-front 稳定合成；超过共享 `tierPixelCap * tierStepCap` 的远候选保留 V1 wall，不静默丢失前沿语义。GPU 工作模型与硬门都是 `sum(primitiveScreenPixels * steps)`，不是每个 primitive 各拿一次完整 tier 上限。

raymarch 规则：

1. 先与 primitive 的有限 corridor/front slab 求交，空像素直接退出。
2. 用 opaque depth 限制最大射线距离；天空像素使用有限天气距离，不能走无限射线。
3. 起点用 blue-noise 加帧序列抖动，避免固定 banding。
4. 每步最多读取一组预计算 weather noise；禁止在 shader 内做多层昂贵 hash/sin FBM。
5. 低频 base density 表示墙体和云底，高频 erosion 只改变细节，不移动权威零交叉前沿。
6. 透射率低于阈值立即停止；完全位于 slab 外的区间跳过。
7. 使用廉价单次散射近似、环境光和太阳方向，不实现多次散射或体积阴影图。

云底和白幕在同一个 scissored primitive pass 中求值，不增加第二类 raymarch。地形变暗、冷色偏移和远景吞没由 transmittance/scattering 完成，不触发区块重建或 Minecraft 光照更新。

### Temporal Reprojection

V2 体积 raymarch 在 60 FPS 以内最多每显示帧更新一次；高于 60 FPS 时最多 `60 volume updates/second`。用 accumulator 均匀安排 `60/45/30 Hz` 更新，避免连续重帧后连续复用；camera cut 或 history reset 允许强制一次更新并计数。未 raymarch 的显示帧使用当前 camera/depth 对上一份 history 重投影并合成，V1 近景雪仍按显示帧更新，因此高速转头不会让雪片显得降到 60 FPS。这个调度只降低平均工作，所有性能门仍分别统计 raymarch 重帧和 history-only 帧的 P95/P99。

history 接受条件同时包含：

- 当前和上一帧 view-projection 可逆且 finite；
- 重投影 UV 在目标内；
- depth 差在阈值内，未发生 disocclusion；
- 当前低分辨率邻域的亮度/透射率 clamp 没有拒绝旧值；
- backend、level、snapshot generation、target generation 和 profile 未改变。

以下事件强制清空或快速衰减 history：teleport、dimension change、respawn、camera entity 改变、FOV/projection 改变、窗口 resize、资源重载、长时间暂停恢复、snapshot 拓扑改变、时钟大幅校正、V2 降级/重新启用。另维护 visible-volume generation：top-K 成员/顺序、support phase 或代表性 front domain 改变时，即使 snapshotGeneration 不变也必须衰减或清 history；普通移动、旋转和同一 support phase 内的连续推进只做重投影。

### Bilateral Upscale And Composite

使用固定 `4` 或 `5` tap 的 depth-aware upscale：

1. 在可见 primitive 的 full-resolution rect union 加 guard band 范围内工作；空 union 完全跳过 composite。
2. 在低分辨率邻域中优先选择与 full-resolution opaque/weather depth 同一侧的样本。
3. 深度边缘不混合前景和远景 history；拒绝时退回最近有效样本。
4. composite shader 不采样 main color。它输出 premultiplied `vec4(scattering, 1 - transmittance)`，以 `ONE, ONE_MINUS_SRC_ALPHA` 混合到 main target，得到 `scattering + sceneColor * transmittance`，避免读写同一颜色纹理。
5. 天空和 opaque terrain 使用分开的 depth 阈值，避免地平线 halo。
6. 不做 full-resolution blur；细节稳定来自 temporal history，不来自额外模糊 pass。

### Quality Controller

`CINEMATIC_AUTO` 只使用异步 GPU timer query 的已完成旧结果。query ring 不可用或结果尚未完成时继续当前 profile，绝不等待 GPU。

控制器使用少量离散档位，例如：

| Tier | Max linear scale | Max volume pixels | Max steps | Max update rate | 60 Hz heavy-frame GPU target |
|---|---:|---:|---:|---:|---:|
| Quality | `0.50` | `0.52 M` | `12` | `60 Hz` | `<= 2.0 ms` |
| Balanced | `0.40` | `0.42 M` | `10` | `60 Hz` | `<= 1.5 ms` |
| Performance | `0.33` | `0.30 M` | `8` | `45 Hz` | `<= 1.0 ms` |
| Minimum cinematic | `0.25` | `0.18 M` | `8` | `30 Hz` | `<= 0.75 ms` |

Auto 使用上表的 `G = min(2.0, 0.12 * 1000 / targetFps) ms`；`targetFps` 取当前有效 FPS cap，没有 cap 时取显示器刷新率。它让 60 Hz 目标为 `2.0 ms`、144 Hz 约为 `0.83 ms`、240 Hz 约为 `0.50 ms`。降档顺序为 resolution -> update rate -> steps；升级使用更长的稳定窗口和滞回。档位最多每 `2s` 评估一次，升级至少需要 `5s` 低于预算，避免画质泵动。任何档位都保留体积墙、云底、空间吞没、V1 近景雪、风向和声音。Auto 到 Minimum 后即保持该档并暴露本地 over-budget 诊断，不擅自切换 backend；是否改用 `SPATIAL_V1` 由玩家决定。

### Low-End And High-End Resource Policy

“低端机可运行”的可靠保证来自玩家可选、语义完整的 `SPATIAL_V1 Fast`，而不是强迫所有 GPU 执行体积 raymarch。玩家选择 Auto 时，它只在 V2 内从当前档位向下收敛；Minimum 的 V2 render-thread CPU P95 目标为 `<= 0.5 ms/frame`，60 Hz 完整重帧 GPU P95 目标为 `<= 0.75 ms`，高刷新率仍受 `12%` 帧预算约束。仍不能达标时保持玩家选择并显示可检索的本地性能状态，不自动换 backend。

昂贵机器也不开放无上限的 Ultra 档。Quality 永远受 `0.52 Mpixel`、`12 steps`、`60 updates/second`、GPU P95 `12%` 帧预算、`15%` 硬门和 `24 MiB` 显存约束；空闲 GPU 时间作为整合包其他 shader、区块、实体和高刷新率的余量，不继续换成更高 raymarch 密度。Quality 的 V2 render-thread CPU P95 仍受 `8%` 帧预算目标和 `1.0 ms` 绝对硬门约束。

## Implementation Path

### Phase 0: Freeze V1 Contract And Measure Render Stages

1. 完成 V1 handoff gate，记录 `SPATIAL_V1` 的 CPU/GPU/allocation/VRAM 基线。
2. 用颜色探针和 GPU capture 验证无 shader、Embeddium、Oculus 下各候选 stage 的 depth、translucent 和 hand 顺序。
3. 记录 1080p、1440p、4K 以及 `60/120/144+ FPS` 的主 target/depth 格式和尺寸。
4. 制作可重复相机轨迹：远处地标、逼近墙体、进入核心、室内窗边、转头、传送和退出维度。

Exit: 后端 render stage 和 depth source 有实测证据；V1 fallback 已达标。没有这个 gate 不开始 shader 美术。

### Phase 1: Backend Boundary And Resource Lifecycle

1. 让 `COMPATIBILITY` 与 `SPATIAL_V1` 实现同一个 backend lifecycle。
2. 新增 coordinator，在 config/reload/level lifecycle 选择 backend；所有 render hook 只委托 coordinator。
3. 建立 V2 shader 注册、target owner、resize coalescing、close 和一次性 downgrade。
4. 用纯色半分辨率 target 完成深度裁剪和主目标合成，不做体积算法。

Exit: 重复 resize/fullscreen/reload/dimension 不能泄漏 GL 资源；强制 shader/target 失败会稳定回到 V1 且不逐帧报错。

### Phase 2: Bounded Volume MVP

1. 从 `ClientWeatherFrame` allocation-free top-K 打包最多 `4` 个可见 primitive，不直接访问 snapshot；全部 primitive 共享 tier step-sample 预算，溢出者继续使用 V1 wall。
2. 实现 ray/slab intersection、权威 signed distance、base density、云底和 blue-noise jitter。
3. 以 `0.5x`、固定 `8` steps 先锁定形体、前沿位置和地标吞没。
4. 接入 V1 近景雪，验证远景体积与近景锐利层没有双重 ownership。

Exit: 四方向墙体位置与 V1 一致；screen rect 外的 descriptor 不增加 pixel work；完整 V2 GPU P95 先满足目标 FPS 的 `12%` 目标后再增加细节，`15%` 只是发布硬门，不是 shader 美术可主动填满的预算。

### Phase 3: Temporal History And Bilateral Upscale

1. 加入 history ping-pong、previous matrices、current/history weather depth、opaque-depth rejection、visible-volume generation 和 neighborhood clamp。
2. 加入 history reset state machine 和 `30/45/60 Hz` volume scheduler。
3. 加入固定 tap bilateral upscale，专项处理 terrain edge、sky horizon、手部和 translucents。
4. 用旋转、冲刺、骑乘、FOV change、teleport、dimension 和 snapshot correction 压测 ghosting。

Exit: 静止和慢速移动没有 banding/shimmer；快速运动不出现可持续 ghost、深度 halo 或一帧 NaN；144 FPS 不执行 144 次 raymarch/second。

### Phase 4: Performance Controller

1. 建立不阻塞的 timer query ring 和纯逻辑质量控制器。
2. 实现四档 resolution/steps/update rate 与滞回，不改变视觉语义。
3. 加入工作计数器：rect-union pixels、每 primitive pixels/steps、共享 step-samples、early-outs、visible primitives、history rejection、draws、target bytes、volume updates，以及完整 V1+V2 weather GPU time。
4. Auto 在 Minimum 持续超预算时保持玩家选择，记录本地 over-budget 状态并停止继续降档；不自动切换 backend。

Exit: Auto 在预算压力下平稳降级，无同步 readback、无频繁 FBO 重建、无每帧 allocation。

### Phase 5: Cinematic Choreography

1. 在同一 density pass 中加入低频云底、前沿 erosion 和有限单次散射。
2. 用 descriptor/time 派生确定性 squall pulse，并限制在已进入过渡区的视觉支持域。
3. 扩展 V1 音频编排：远处低频风、前锋冲击、核心持续风和室内 exposure；严格限制 loop/one-shot 数量。
4. 调整 composite 的环境冷色和 transmittance，让地标按距离消失而不是全屏纯白。

Exit: Approach/Impact/Core/Retreat 四段录屏都可辨认；电影感来自空间层次和时间编排，不靠提高 steps、切片或粒子数量突破预算。

### Phase 6: Compatibility, Rollout And Documentation

1. 完成无 shader、Embeddium、Oculus 目标 shader pack、Fast/Fancy、所有 particle status 和常见分辨率矩阵。
2. 首次发布保持 `SPATIAL_V1` 默认，V2 opt-in；收集 GPU vendor、fallback reason 和 profile 证据时只使用本地诊断，不增加网络遥测。
3. 达到所有 release gate 后再单独决定是否把 `CINEMATIC_AUTO` 设为默认；该决定不与实现 commit 绑定。
4. 更新 living docs、配置说明、diary 和本计划 Outcome。

Exit: 所有失败路径自动恢复，性能数据可复现，文档只描述实际启用状态。

## Test Coverage Plan

### Code And User-Flow Diagram

```text
backend lifecycle                              player experience
  +-- config/reload/level selects backend        +-- approach -> distant wall/cloud/sound
  |    +-- capable -> CINEMATIC_V2                +-- impact -> bounded squall pulse
  |    `-- unavailable/fault -> SPATIAL_V1        +-- core -> volume + sharp near snow
  +-- beginFrame once                             +-- indoors -> exposure attenuation
  +-- atmosphere stage                            `-- retreat -> stable history decay
  |    +-- valid depth -> volume/history
  |    `-- invalid -> downgrade
  +-- precipitation stage -> V1 near snow
  `-- close -> release targets/query objects

volume update                                  temporal/composite
  +-- visible primitive count 0 -> no raymarch   +-- valid reprojection -> accept/clamp
  +-- ray misses all slabs -> early-out           +-- disocclusion -> reject history
  +-- opaque depth clamps ray                     +-- reset event -> clear history
  +-- transmittance low -> early-out               +-- bilateral depth edge -> nearest valid
  `-- max steps reached -> bounded result          `-- invalid math -> V1 downgrade
```

### Automated Tests

| Test | Required assertions |
|---|---|
| `WeatherBackendSelectionTest` | mode/capability/reload/level matrix；fault quarantine；显式 lifecycle 后允许一次重试 |
| `CinematicVolumePrimitiveTest` | 四方向 basis；固定容量稳定 top-K；共享 step-sample budget；屏外/溢出 primitive 走 V1 wall；前沿不跑赢 `VisualKernel` |
| `CinematicDensityModelTest` | slab bounds、density/transmittance finite 且有界、squall pulse 只在视觉支持域内 |
| `CinematicHistoryResetTest` | teleport、dimension、FOV、resize、reload、snapshot generation、clock correction、pause gap |
| `CinematicVolumeSchedulerTest` | `30/45/60/120/144` FPS 输入下 raymarch rate 上限和强制更新条件 |
| `CinematicQualityControllerTest` | 固定档不创建 query；Auto 的 query unavailable 保持档位；旧结果驱动；降档/升档滞回；minimum 超预算保持 backend 并暴露状态 |
| `CinematicTargetLayoutTest` | resize 合并、格式/尺寸、1080p bytes 预算、close 幂等、generation 变化 |
| `CinematicRenderPlanTest` | no-volume skip、`1-4` 个 scissored raymarch、rect-union+guard composite、history-only 无 temporal rewrite、共享 steps/rect 上限、V1 near-weather 委托一次 |
| `CinematicHistoryMathTest` | reprojection bounds、invalid `w`/NaN 拒绝、current/history weather depth、opaque depth discontinuity、visible-volume generation、sky/terrain 阈值分支 |
| `CinematicFallbackTest` | compile/depth/target/render fault 后一次降级；render state 恢复；无逐帧重试或日志洪泛 |

GLSL 编译、真实 depth 内容、FBO 生命周期、timer query、GPU 时间、透明顺序、声音和画面冲击力必须做集成验证，不能用 mock 代替。自动化测试验证纯模型、状态机、工作上限和恢复决策，不用不稳定的 wall-clock assertion。

### Runtime Matrix

| Axis | Required cases |
|---|---|
| Renderer | no shader、Embeddium、Oculus supported shader pack |
| GPU | 至少一组集显/低端、一组主流独显；记录 vendor/driver，不按名称硬编码策略 |
| Resolution | 1080p、1440p、4K；窗口 resize 与 fullscreen 循环 |
| Refresh | `30/60/120/144+ FPS` caps；站立和高速转头 |
| Weather | global clear/snow/blizzard；local outside/front/core/retreat；`0/1/4/32` descriptors |
| Scene | 开阔地标、树林、山谷、室内窗边、玻璃/水、粒子、手持物 |
| Lifecycle | login、respawn、dimension、teleport、FOV、pause、resource reload、config switch |
| Competing post-process | 红外视图开/关、半径退场、退出后 V2 history reset |
| Fault injection | shader compile、depth mismatch、target allocation、invalid matrix、render exception、query unavailable |

同一 seed、descriptor、相机轨迹、分辨率和 FPS cap 比较 `COMPATIBILITY`、`SPATIAL_V1`、`CINEMATIC_V2`。采集 render-thread CPU、GPU median/P95/P99、allocation、retained GL bytes、draws、volume updates、steps、history rejection 和 fallback reason。

## Failure Modes

| Failure | Recovery | Coverage | Player-visible result if handled |
|---|---|---|---|
| V2 shader compile/reload fails | 留下 V2 unavailable，切 `SPATIAL_V1`，一次诊断 | fault injection | 仍有完整 V1 天气，无崩溃 |
| Oculus pipeline/target changes | resource/level lifecycle 重建 provider 和 targets | Oculus reload QA | 最多一次无体积帧，然后恢复或 V1 |
| depth dimensions/format mismatch | 跳帧、清 history、pending rebuild；同一 generation 连续 3 次才故障恢复到 V1 | fallback test + resize QA | 瞬态最多缺一帧体积；持续故障保留 V1 天气 |
| resize/fullscreen leaks FBO | 合并 resize，关闭旧 generation，记录 target bytes | target test + retained GL capture | 无持续显存增长 |
| stale history ghosts landmarks | depth rejection、neighborhood clamp、reset state machine | math tests + camera recording | 短暂降低 history 权重 |
| depth edge creates white halo | bilateral discontinuity fallback to nearest valid | math test + terrain/glass QA | 边缘保持锐利 |
| hand/translucent ordering wrong | stage gate；不通过的环境降级 V1 | shader matrix | 手部和玻璃不被白幕错误覆盖 |
| invalid inverse/reprojection produces NaN | finite checks，丢弃 history；重复则降级 | history/fallback tests | 单帧退回当前有效结果 |
| render state not restored | 每个 pass `try/finally` 恢复 target、viewport、blend、depth、textures | fallback integration | 后续世界/UI 不受污染 |
| GPU query stalls | 只读取 available 的旧 query；无结果保持档位 | controller test + capture | 画质暂不自动变化 |
| Auto quality pumps | 离散档位、2s 评估、5s 升档滞回 | controller test | 档位变化不频繁闪动 |
| minimum tier still slow | 保持玩家 backend，停止降档并暴露本地 over-budget 状态 | sustained-load test | 效果保持；玩家可主动选择 `SPATIAL_V1` |
| infrared full-screen pass conflicts with V2 | 红外半径大于零时暂停 cinematic atmosphere，退出后 reset history | infrared toggle matrix | 红外期间使用 V1 天气，退出后无旧 history 残影 |
| high FPS repeats full raymarch | 体积 scheduler 上限 60 Hz，其他帧重投影 | scheduler test | 近景仍跟手，GPU 成本受控 |
| snapshot correction moves wall abruptly | V1 bounded clock correction；拓扑变更 reset history | state/history QA | 无旧墙残影 |
| backend retries every frame | quarantine 到下一显式 lifecycle | selection/fallback test | 无持续卡顿和日志洪泛 |

任何失败路径都不能同时“无测试、无恢复、静默破坏画面”。V2 失败的统一恢复目标是 `SPATIAL_V1`，不是黑屏、纯白屏、崩溃或把修复工作推给玩家。

## Acceptance Criteria

1. V1 handoff gate 全部完成；V2 未复制 snapshot、时钟、天气网格或玩法采样。
2. V2 源码改动不增加服务端天气 CPU、内存、packet 类型、payload 或频率。
3. Approach/Impact/Core/Retreat 四阶段可从录屏明确分辨；远处地标按空间深度被风暴吞没。
4. 宏观墙体位置与 `VisualKernel` 一致；任何 squall pulse 都不能推进 gameplay 前沿。
5. 默认 raymarch 使用线性 `0.5x` 或更低；代码和 capture 均证明不存在 full-resolution raymarch。
6. 完整 V1+V2 weather GPU P95 满足目标 FPS 的 `12%` 目标和 `15%` 硬门：60 Hz 为 `2.0/2.5 ms`，144 Hz 约为 `0.83/1.04 ms`，240 Hz 约为 `0.50/0.625 ms`；同时单独记录 V2 增量，Auto 只使用扣除 V1 后的剩余预算。
7. V2 render-thread CPU P95 满足目标 FPS 的 `8%` 目标且绝对不超过 `1.0 ms`；`120/144+ FPS` 时 raymarch 不超过 `60 updates/second`，重帧自身仍单独通过 P95，稳定运行 P99 不超过帧预算 `25%` 或 `4.0 ms` 中较小者。
8. 稳定画面和 volume update 均为 `0 B` Java allocation；无同步 GPU readback 或 wait。
9. 1080p V2 target 实测增量显存目标 `<= 20 MiB`、硬门 `<= 24 MiB`；resize/reload/dimension 循环无增长。
10. pixel work 受 rect union、最多 `4` 个 scissored draws 和共享 `sum(scissorPixels_i*steps_i) <= tierPixelCap*tierStepCap` 限制，不与全 snapshot、白幕覆盖面积或 primitive count 乘法扩张；超预算候选保留 V1 wall。
11. terrain、sky、glass/water、hand 和近景雪没有持续 ghost、halo、错误遮挡或双重 precipitation。
12. teleport、FOV、dimension、pause、resize、reload 和 snapshot topology change 正确重置 history。
13. 性能策略不自动改变玩家 backend；只有 shader 编译失败、FBO 不完整、持续 depth/provider 无效或 render 异常等功能故障才恢复到 V1，且 render state 完整恢复、同一 session 不逐帧重试。
14. Fast/Auto 降级仍保留墙体、云底、空间吞没、近景雪、风和声音语义。
15. 首次发布仍以 `SPATIAL_V1` 为默认；只有完整 runtime matrix 和至少低端/主流两档 GPU 证据通过后，才另行决定默认切换。
16. 所有纯逻辑测试、`gradlew.bat test`、shader reload smoke test 和 `git diff --check` 通过。
17. 实现完成后 living docs、配置说明、diary 和本计划 Outcome 与实际默认 backend 一致。

## NOT In Scope

- 不改变服务端白幕速度、气候事件、温度、伤害、作物、预报或存档语义。
- 不增加逐帧、逐 tick、逐雪花、逐体素或逐玩家视觉网络同步。
- 不做服务端或客户端三维流体/雪粒物理模拟。
- 不使用 compute shader、ray tracing、multi-scattering、体积阴影图或全分辨率 raymarch。
- 不修改 Minecraft light engine、区块 mesh 或方块材质来表现风暴变暗。
- 不要求任意第三方 shader pack 都支持 V2；未通过矩阵的环境自动使用 V1。
- 不重建 `BlizzardRenderer` 的遗留 quad 路径。
- 不让单片近景雪花跨客户端确定性同步；大尺度墙、方向、强度和 pulse 必须确定。
- 不在 V2 首次发布中删除 `SPATIAL_V1` 或 `COMPATIBILITY`。
- 不修改 `design/`、KubeJS、配方、任务、数据包或伴生整合包配置。

## Documentation Impact

V2 实现前，living docs 只能链接本计划并标注未实现。实现完成后再更新：

- `docs/climate/weather-rendering.md`: 当前 backend、stage、target/depth、quality、fallback 和实测预算。
- `docs/climate/data-lifecycle-and-integration.md`: 仅增加客户端资源生命周期和配置；服务端/网络章节应明确 V2 无变化。
- `docs/climate/README.md`: 保持入口为 Current，链接实际实现文档。
- `diary/`: 记录各 phase、GPU/CPU/显存证据、兼容矩阵、默认模式和未支持环境。

## Worktree Execution Strategy

| Workstream | Modules | Depends on |
|---|---|---|
| A. V1 contract and stage/depth probe | climate client state、render events、profiling | V1 complete |
| B. Backend lifecycle and target ownership | render、shader registration、client config | A |
| C. Volume math and quality controller tests | pure render models、tests | A contract |
| D. Volume/history/upscale shaders | shader resources、render backend | B and C |
| E. Near-weather/audio/choreography integration | render、sound、weather Mixin | D |
| F. Compatibility/performance/docs | tests、docs、diary | B through E |

```text
A contract/probe
      |
      +--> B lifecycle/targets --+
      |                          +--> D shaders/history --> E integration --> F validation
      +--> C pure models/tests --+
```

B 和 C 可在独立 worktree 并行；B 冻结 GL lifecycle，C 冻结 primitive/history/quality 纯契约。D 必须在两者合并后进行。E 与 D 都会触碰 `render`，保持顺序以避免两个 backend owner。F 的性能证据和 living docs 必须基于合并后的同一 build，不能由各 lane 单独宣称完成。

Recommended commit boundaries:

1. V1 backend handoff and render-stage evidence;
2. coordinator、fallback、resource lifecycle and solid-color target;
3. pure volume/history/quality models and tests;
4. bounded raymarch MVP;
5. temporal reprojection、bilateral upscale and volume scheduler;
6. Auto GPU controller and counters;
7. cinematic choreography、audio and visual assets;
8. compatibility matrix、performance evidence、living docs and Outcome.

## Inline Diagram Requirements

实现时只在以下非显然契约旁保留短 ASCII 图：

- coordinator: lifecycle selection -> two render stages -> one backend owner；
- cinematic backend: target generation、history generation 和 downgrade；
- history reset model: retain / decay / clear transitions；
- Oculus depth provider: lifecycle probe 与 per-frame texture handle 读取的边界；
- quality controller: async query ring -> hysteresis -> tier/fallback；
- render ownership test: atmosphere、near precipitation、Vanilla fallback 的唯一 owner。

## Outcome

Not implemented. V2 depends on the V1 handoff gates above. Record final backend classes, render stage, depth formats, measured CPU/GPU/VRAM budgets, supported shader matrix, default mode and fallback reasons here after implementation.
