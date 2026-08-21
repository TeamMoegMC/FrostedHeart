# Frosted Research 科技树渲染性能优化方案

- Time: `2026-08-21 16:50:42 +08:00`
- Authors: `Codex (OpenAI; performance investigation and implementation planning)`
- Status: `implemented-awaiting-runtime-profile`
- Scope: `com.teammoeg.chorda.client.icon`、`com.teammoeg.chorda.client.ui`、`com.teammoeg.chorda.client.cui.base`、`com.teammoeg.frostedresearch.gui.archive` 的客户端渲染热路径
- Related: `plans/2026-08-20_17-23-50_frostedresearch-modern-tech-tree-gui.md`、`docs/research/research-ui.md`、`diary/2026-08-21_16-32-10_cui-viewport-research-render-fixes.md`、`PanZoomViewport`、`ResearchGraphViewport`、`CIcons.ItemIcon`、`CGuiHelper#drawItem`

## Goal

在不隐藏研究物品、不隐藏研究名称、不提高 `15%` 最小缩放、不改变研究业务逻辑的前提下，降低科技树相对绘图台增加的帧时间，并让同一套基础能力可被未来地图视口复用。

当前测试人员报告的参考值为：

- 绘图台约 `250 FPS`，约 `4.0 ms/frame`；
- 科技树约 `90 FPS`，约 `11.1 ms/frame`；
- 科技树增量约 `7.1 ms/frame`。

首轮目标按同一世界、同一视角、同一窗口和 GUI scale 的帧时间差验收，不用绝对 FPS 掩盖环境差异：

| 场景 | 目标 |
|---|---|
| 当前 `81` 节点、`106` 边，`15%` 静止全览 | 科技树相对绘图台的 median 增量不超过 `2.5 ms/frame` |
| 同上，持续拖动画布 | 总帧时间 P95 不超过 `12 ms/frame` |
| 当前图静止 `30s` | 热路径不再每帧解析名称、拆分描述或重建可见几何 |
| 当前图物品绘制 | 图标批次的强制提交次数由约 `94` 次降至兼容性允许的最少分组，目标不超过 `4` 次 |
| `250` 节点、`500` 边压力数据 | 不崩溃、不出现空白节点，静止总帧时间不超过 `16.7 ms/frame` |

若 Phase 1 和 Phase 2 已达到帧时间目标，Phase 4 的高级缓存不实施。

## Verified Current State

### 数据规模

`run/config/fhresearches` 当前有 `81` 个研究定义和约 `106` 条依赖边。全览时研究图标合计约 `94` 次物品模型绘制：`68` 个研究绘制一个物品，`13` 个组合图标绘制两个物品。

### 已有优化

- `PanZoomViewport` 已提供 camera、拖动、缩放、坐标转换、矩形裁剪和线段裁剪；
- `ResearchGraphViewport` 已把背景、网格、边和节点底色合并到一个 `ShapeTesslator` 提交；
- 屏幕外节点已从内容绘制中剔除，边线段也会裁剪；
- 节点渲染记录按高水位复用，鼠标命中结果会缓存；
- 搜索匹配、项目列表、详情 clue、描述换行和内容高度已有局部缓存；
- 定义、进度、当前研究和 clue 进度已有窄化通知，不需要轮询式重建布局；
- 项目弹窗打开时会暂停图渲染，关闭时不重建 camera 或 layout。

这些基础继续复用，不重新实现 camera、裁剪、布局算法、控件系统或研究状态模型。

### 仍存在的确定热点

1. `CIcons.ItemIcon#draw` 经 `CGuiHelper#drawItem` 绘制，每个物品模型都会查询 `BakedModel`、切换 Lighting、创建矩阵并调用 `GuiGraphics#flush()`；
2. `ResearchGraphViewport#drawNodeContent` 每个可见节点每帧调用 `Research#getName()`，触发 `FRTextUtil` 和 `StringTextComponentParser`；
3. 同一方法每帧重新执行名称裁剪，并为每个名称单独 `pushPose/scale/popPose`；
4. `ResearchProjectSummaryPanel` 每帧重复查询状态、解析描述并执行 `Font#split`；
5. camera 静止时仍遍历全部投影节点和边，重复执行哈希查询、世界坐标转换和线段裁剪；
6. 节点状态每帧通过 `Research#isCompleted`、`Research#isInProgress` 等对象 API 查询；`TeamResearchData#getCurrentResearch` 还返回临时 `Supplier`；
7. `Research#getParents` 通过 Stream 重建集合，列表中的 `isUnlocked` 可能逐帧触发；
8. 隐藏的绘图台 layer 不绘制也不 tick，但 CUI 仍会递归更新其 render info 和 mouse-over；
9. `ResearchWorkspaceState#bookmarkedResearchIds` 重复创建不可修改包装，`ResearchFieldTabBar` 重复调用会复制数组的 `ResearchCategory.values()`；
10. 项目列表只绘制可见行，但仍从列表首行循环到末行。

静态阅读可以确认这些工作存在；实际毫秒占比必须由 Phase 0 的运行时采样确认。

## Constraints

- `15%-175%` 全缩放范围始终绘制节点物品和名称；保留当前物品最小 `4px`、文字最小 `0.25` scale；
- 不用“低缩放隐藏内容”、降低研究数量、减少玩家可见定义或缩小节点世界尺寸换取性能；
- 不修改 `ResearchData`、`TeamResearchData` 的完成语义，不修改 clue、packet、codec 或存档；
- 不让 CUI 持有 Frosted Research 业务状态；共享库只接收通用图标、camera、可见性和缓存失效信号；
- 不把每个研究节点改成 CUI 子控件；节点继续使用虚拟批量渲染；
- 语言和资源重载后不得继续显示旧名称、旧裁剪文本或失效模型；
- 所有 Lighting、pose 和 buffer 状态必须在异常路径通过 `try/finally` 恢复；
- 每个阶段单独测量并可单独回退，结构改造和高级 GPU 缓存不一次合并。

## Target Architecture

```text
definition revision
       |
       v
Research presentation cache
  Component / resolved name / lowercase search text
       |
       +------------------------------+
                                      |
progress / active / clue revision     |
       |                              |
       v                              v
Research UI state snapshot ------> Graph render plan
 completed / active / unlocked      direct Research + NodePosition refs
 progress / visible                 visible node screen bounds
                                    clipped edge rectangles
camera / viewport / projection ---> current-width truncated labels
revision                             style flags
                                      |
                                      v
per frame
  1. submit cached shape geometry
  2. submit compatible item icons by lighting group
  3. submit node labels under one shared text transform
  4. draw dynamic hover, selection, tools and tooltip
```

缓存只保存客户端展示派生值。定义和同步后的 `TeamResearchData` 仍是事实来源。

### Invalidation Contract

| 事件 | Presentation | State snapshot | Render plan | Layout/projection |
|---|---:|---:|---:|---:|
| definition reload | rebuild | rebuild | rebuild | rebuild |
| language/resource reload | rebuild | keep | rebuild labels/icons | keep |
| research visibility集合变化 | update | rebuild | rebuild | rebuild projection |
| progress/clue change且可见集合不变 | keep | update affected research | update styles | keep |
| active research change | keep | update old/new active state | update styles | keep |
| selection/bookmark change | keep | keep | update styles only | keep |
| search change | use cached lowercase text | keep | update match/style flags | keep |
| camera/zoom/viewport change | keep | keep | rebuild geometry and truncation | keep |
| 项目弹窗开关 | keep | keep | suspend/resume only | keep |

任何新增缓存都必须在测试中覆盖上表的失效路径。

## Implementation Steps

### Phase 0: 建立可重复性能基线

1. 为研究图形渲染加入可关闭的 profiler section：`shapes`、`item_icons`、`labels`、`summary_list`；优先使用 Minecraft 已有 `ProfilerFiller`，不在每个节点调用 `System.nanoTime()`；
2. 在开发模式记录每帧可见节点数、可见边数、物品绘制数、强制 flush 数、render-plan rebuild 数；默认关闭日志，避免测量代码本身污染帧时间；
3. 固定五个运行场景：绘图台、`15%` 静止全览、`15%` 持续拖动、`100%` 局部视图、项目弹窗打开；
4. 每个场景预热 `30s`，采样 `30s`，记录 median、P95、P99、render-thread CPU、allocation rate 和 GC pause；
5. 先做一次仅用于诊断的图标/文字/边分段 A/B，不把隐藏内容的诊断开关交付给玩家；
6. 保存原始采样配置和结果摘要到完成 diary，作为各阶段对比基线。

Phase 0 的进入 Phase 1 条件：确认 `item_icons` 或 `flush` 是前两大热点。若结果不符，暂停实现并按采样结果修订本计划。

### Phase 1: 物品图标兼容批次

目标是消除逐物品强制提交和重复 Lighting 状态切换，同时保持所有物品、组合图标和动画图标可见。

1. 在 Chorda 图标层增加通用的批次渲染上下文；`CIcon` 默认保留立即绘制 fallback，不要求一次迁移全部调用方；
2. `ItemIcon` 向批次提交 `ItemStack + rect + z + decoration policy`；`CombinedIcon` 和 `AnimatedIcon` 解析当前子图标后继续委托批次；
3. 按提交顺序构造连续兼容段，只在 flat/block Lighting、立即绘制 fallback、装饰层或其他 RenderType 屏障处提交；
4. 每个 Lighting 段开始前设置一次光照，段结束时 flush 一次，并在 `finally` 恢复 GUI 的 3D Lighting；
5. 科技树节点图标使用 `drawDecorations=false`，避免在 `4px` 图标上执行无意义的数量、耐久文字流程；详情物品槽和其他现有 GUI 保持原装饰行为；
6. 第一版不缓存 `BakedModel`，避免 compass、clock、CustomModelData、动态 property override 和资源重载错误；先测量模型查询是否仍是热点；
7. 不支持批次的自定义 `CIcon` 必须立即绘制并形成明确 barrier，不能丢失或静默改变层级；
8. `ResearchGraphViewport` 的图标 pass 只负责提交，统一在 pass 结束时关闭批次。

Phase 1 验收：

- 当前全览的图标强制提交目标不超过 `4` 次；
- flat item、3D block item、glint、组合图标、动画 ingredient 图标视觉正确；
- 单个不支持的自定义图标只退化该屏障附近的批次，不让整棵树回到逐物品 flush；
- 异常后下一帧的 GUI 光照、字体和普通物品槽仍正常。

### Phase 2: 展示文本和研究状态快照

1. 在 archive 层建立按 definition revision 管理的只读 presentation cache，保存解析后的名称 `Component`、resolved String 和 lowercase search text；
2. 语言或资源重载时清空 presentation cache；不把语言相关字符串永久写进 `Research` 定义；
3. Render plan 重建时根据当前节点像素宽度生成裁剪标题，静止 camera 不再调用 `plainSubstrByWidth`；
4. 所有节点名称在一个共享文字 scale 下批量绘制，移除逐节点 pose push/pop；
5. 根据现有 definition/progress/active/clue 通知维护只读状态快照，统一计算 `completed/active/unlocked/progress/visible`；
6. archive 更新时只获取一次 `TeamResearchData` 和 active research ID；增加直接只读 getter 或 UI adapter，不再在热路径创建 `Supplier`；
7. 项目列表、图节点和右侧摘要读取同一状态快照，避免对同一研究重复调用 `isCompleted/isInProgress/isUnlocked`；
8. 右侧摘要缓存标题、分类、首段描述的换行结果和按钮文字，按选中研究、宽度、语言和进度 revision 失效；
9. 缓存书签只读 view 和 `ResearchCategory` 数组；项目列表直接计算首个/最后一个可见 row index；
10. 固定翻译 `Component` 提升为实例或静态常量，zoom 百分比直接绘制 String。

Phase 2 验收：静止全览预热后，每帧名称解析次数、描述 split 次数和列表排序次数均为 `0`；状态变化后一帧内显示新进度，不重建 layout 或重置 camera。

### Phase 3: Render plan 和隐藏子树短路

1. projection 构建后生成带直接引用的 graph render source，避免每帧通过 String ID 查询 `Research`、`NodePosition` 和 context Set；
2. camera、zoom、viewport 或 projection revision 变化时才重建 `GraphRenderPlan`；计划保存可见节点屏幕 bounds、裁剪后的三段边矩形、节点内容位置和当前裁剪标题；
3. selection、search 和 progress 只更新 style/state 字段；如果定义可见集合变化，再走完整 projection rebuild；
4. 静止帧复用 render plan 并重新提交必要顶点，不重复世界坐标转换和线段裁剪；
5. 为 `UIElement/UILayer` 增加不可见子树 render-info 和 mouse-over 短路；重新显示时必须在同一帧恢复坐标和 hover；
6. 保留 tick 的既有语义，避免一次通用优化意外冻结依赖“隐藏但启用”的 CUI 动画；绘图台在 archive 模式下已经 disabled，因此不会 tick；
7. FTB 外部 widget 扫描仍按 tick 执行以捕捉延迟注入，但只在集合身份变化时写状态；它不是 render-frame 主路径。

Phase 3 验收：静止 camera 的 render-plan rebuild count 为 `0/frame`；拖动、缩放、resize、类型切换、语言重载和 definition reload 后画面与命中区域同步，没有一帧错位。

### Phase 4: 仅在指标未达标时扩展

按以下顺序逐项验证，不打包实施：

1. 节点和边按 layout rank 或固定世界网格建立空间索引，查询当前 world bounds 覆盖的桶；
2. 将静态 shape geometry 缓存为 GPU `VertexBuffer`，只在 render-plan revision 变化时上传；
3. 低缩放把物品模型预渲染为资源重载可失效的缩略图图集，高缩放继续使用真实模型；
4. 将静止 graph surface 缓存到离屏 `RenderTarget`，动态 hover、selection 和 tooltip 保持独立绘制。

Phase 4 每一项都需要新的前后采样证明它解决仍存在的热点。禁止仅因为“地图以后可能很大”提前实现。

## Test Plan

所有 Gradle 测试和测试结果验证必须交给 `gpt-5.6-terra`、`medium` 模型执行；主实现代理不自行运行测试。

### Coverage Flow

```text
CIcon submission
  +-- ItemIcon --------------------> compatible lighting segment -> one flush
  +-- Combined/AnimatedIcon -------> child submissions preserve order
  +-- unsupported CIcon -----------> barrier -> immediate fallback -> resume batch
  +-- renderer throws -------------> finally restores Lighting and closes buffer

archive revision
  +-- definition ------------------> presentation + state + render plan rebuild
  +-- language/resource -----------> presentation + labels rebuild
  +-- progress/active/clue --------> affected state/style update
  +-- camera/resize/type ----------> render plan rebuild
  +-- selection/search ------------> style update, no layout rebuild
  +-- no revision -----------------> no cache rebuild

CUI visibility
  +-- hidden subtree --------------> no descendant render-info/hover traversal
  +-- visible next frame ----------> coordinates and hover refresh immediately
```

### Automated Tests

- 新增纯逻辑 batch planner 测试：flat/block 分组、barrier、顺序、fallback、异常关闭和 flush count；
- 扩展 `ResearchGraphViewportPerformanceTest`：静止 cache reuse、camera/resize/projection invalidation、search/status 不重建 layout；
- 新增 presentation cache 测试：定义、语言、资源和宽度 revision 的命中与失效；
- 扩展 `ResearchTypeListPanelCacheTest`：可见 row 范围、状态快照、书签和 active research 更新；
- 扩展 `ResearchArchiveLayerConstructionTest`：各同步通知只失效约定缓存；
- 扩展 `PanZoomViewportTest` 或新增 CUI visibility 测试：隐藏子树短路与重新显示恢复；
- 保留低缩放回归：`15%` 时图标和名称仍被提交，最小像素策略不变；
- 保留项目弹窗回归：图暂停但 camera/render plan 不被销毁；
- 压力夹具覆盖 `0/1/81/250` 节点、`0/106/500` 边、孤立节点、跨类型边和组合图标。

Terra medium 分阶段运行：

```powershell
.\gradlew.bat test --tests "com.teammoeg.chorda.client.cui.base.*" --tests "com.teammoeg.frostedresearch.gui.archive.*"
.\gradlew.bat test
```

### Runtime And Visual QA

- 同一硬件按 Phase 0 五场景重新采样，并保留 before/after JFR；
- 覆盖 `15%/25%/55%/100%/175%`，确认所有节点物品和名称持续存在；
- 覆盖 flat item、3D block item、glint、耐久、数量、组合图标和动画 ingredient；
- 覆盖中英文切换、资源包 reload、窗口 resize 和 GUI scale 切换；
- 静止、拖动、滚轮缩放、快速切类型、搜索和打开/关闭项目弹窗均无闪烁或错位；
- 弹窗、右侧摘要、左侧列表和顶栏层级不被物品批次穿透；
- 关闭 archive 返回绘图台后，绘图台槽位物品 Lighting 和装饰正常。

## Failure Modes

| 失败模式 | 防护 | 自动测试 | 玩家表现 |
|---|---|---|---|
| flat/block item 被错误合并 | Lighting compatibility key + barrier + `finally` 恢复 | batch planner | 图标可能过暗或角度错误，视觉 QA 可见 |
| unsupported icon 被批次吞掉 | `CIcon` 默认 immediate fallback | fallback test | 不允许静默丢图标 |
| 组合图标顺序改变 | 按提交顺序形成连续段，不跨 barrier 重排 | order test | 重叠图标前后层错误，视觉 QA 可见 |
| 语言重载未失效标题 | client reload revision 清空 presentation | reload test | 显示旧语言，明确可见 |
| progress 通知未更新状态快照 | notification-to-cache contract | archive notification tests | 状态短暂陈旧，不允许持续静默 |
| camera 改变但复用旧 bounds | camera/viewport revision 是 render-plan key | invalidation tests | 绘制和点击错位，属于阻断发布的问题 |
| 隐藏子树重新显示后 hover 陈旧 | visible transition 当帧重新 update | CUI visibility test | 首帧按钮状态错误，测试必须覆盖 |
| 图标渲染抛异常污染后续 GUI | batch close 和 Lighting restore 放入 `finally` | exception test | 后续界面整体变暗或 buffer 未关闭，阻断发布 |
| 动态模型被错误缓存 | Phase 1 不缓存 `BakedModel` | 不适用 | 首轮规避该风险 |
| 高级缓存消耗过多显存 | Phase 4 单项实施并记录 texture/buffer size | 条件阶段测试 | 未达到门槛时不实施 |

不存在“无测试、无恢复且玩家不可见”的已知新路径；运行时 GPU 排队时间仍必须通过实际采样验证。

## Acceptance Criteria

1. 当前测试数据达到 Goal 中的 median、P95 和 flush count 目标，或 diary 中记录无法达到的硬件/驱动证据；
2. `15%-175%` 全范围持续显示研究物品和名称，没有按缩放阈值隐藏内容；
3. camera 静止时不解析名称、不拆分摘要描述、不重算坐标或裁剪边；
4. progress、active、clue、search、selection、语言、资源和 resize 都按失效表更新，不出现持续陈旧状态；
5. 物品批次正确处理 flat、3D、glint、组合、动画和 fallback 图标；
6. 绘图台、科技树、项目弹窗之间切换后 Lighting、层级、槽位和 tooltip 正常；
7. 所有新增纯逻辑分支有自动测试，全部测试由 Terra medium 执行并通过；
8. 性能改动不发送新 packet、不修改研究完成逻辑、不改存档；
9. 每个阶段有 before/after 数据，未证明收益的 Phase 4 代码不合入；
10. `docs/research/research-ui.md` 和完成 diary 与最终实现、指标和剩余限制一致。

## NOT in Scope

- 不修改研究内容、解锁关系、节点布局算法或当前玩家可见性规则；
- 不通过隐藏名称、隐藏物品、提高最小 zoom 或删除边来提升 FPS；
- 首轮不实现离屏 framebuffer、缩略图图集、tile renderer 或通用 scene graph；这些需要 Phase 4 数据触发；
- 不为尚未开始的 FTB 地图实现 marker、tile、dimension 或 waypoint 业务；只保持 CUI 批次和 viewport API 可复用；
- 不重写 Minecraft `GuiGraphics` 或全局替换所有 Chorda item GUI；先让科技树成为受控调用方，再评估推广；
- 不修改服务器、网络同步、clue、codec、KubeJS、配方、任务或整合包配置。

## Documentation Impact

实现后更新 `docs/research/research-ui.md` 的 Rendering And Derived-State Performance 和 Validation，记录实际缓存层、失效事件、批次边界和运行时性能结果。若 Chorda 新增公共图标批次 API，更新对应 package Javadoc。

每个完成阶段在同一篇最终 diary 中记录：改动、采样环境、before/after、Terra medium 测试命令与结果、未实施的条件阶段。若实现中止或方案变化，更新本计划状态和 Outcome，不把未实现内容写入 living docs。

## Execution Strategy

顺序实施，不并行拆分代码工作流。Phase 1、Phase 2、Phase 3 都会修改 `ResearchGraphViewport` 或其直接数据源，并且后一阶段的性能判断依赖前一阶段的新基线；并行开发会制造冲突并让性能归因失真。

可以并行的只有验证角色：每个实现阶段完成后，由 Terra medium 在共享改动稳定时运行专注测试和完整测试。运行时视觉/FPS 采样必须在对应测试通过后执行。

推荐提交边界：

1. profiler 和 baseline；
2. Chorda icon batch + graph integration；
3. presentation/state snapshot；
4. render plan + hidden subtree short-circuit；
5. docs、diary 和最终性能证据；
6. Phase 4 每个被数据触发的项目单独提交。

## Outcome

Phase 1-3 已于 `2026-08-21` 实施，且保留了本计划开始前已有的相机、命中缓存、弹窗暂停和 `15%` 低缩放内容修复。

- 新增 Chorda `CIconBatch`。默认严格保持提交顺序；科技树显式使用 `LAYER_THEN_LIGHTING`，按组合图标 z 层和 flat/block Lighting 分组。没有立即绘制屏障时，普通节点图标最多两次物品提交，带物品叠加层时最多四次。模型仍逐 pass 解析，没有缓存 `BakedModel`。
- `ResearchArchiveViewCache` 按定义/语言 revision 缓存名称、分类和搜索文本，并按研究/active 通知刷新完成、进行中、解锁和进度快照。`TeamResearchData` 增加无临时 `Supplier` 的直接只读 getter，同时保留旧 getter 兼容性。
- `ResearchGraphViewport` 投影后保存直接引用，并按 camera/viewport/projection/language 构造屏幕空间 render plan。静止帧复用节点几何、边裁剪、网格和裁剪标题；search/selection/progress 只更新样式。名称在一个共享缩放 pose 中绘制，物品 `4px` 和文字 `0.25` 下限未改变。
- 项目列表按首尾可见行绘制；右侧摘要缓存标题、状态、描述换行和按钮文字；书签只读 view 与研究领域数组不再逐帧包装/复制。
- 隐藏 CUI 元素/层跳过 descendant render-info 和 mouse-over，`setVisible(false)` 清理自身 hover；重新显示后的同一 frame update 恢复坐标与 hover，tick 语义未改变。
- 新增 profiler sections：`frostedresearch_graph_shapes`、`frostedresearch_graph_item_icons`、`frostedresearch_graph_labels`。

Terra medium 使用 JDK 17 完成验证：定向缓存/批次/可见性测试 `BUILD SUCCESSFUL in 22s`，研究 GUI 定向测试 `BUILD SUCCESSFUL in 17s`，全量 `.\gradlew.bat test --no-daemon --console=plain` 为 `BUILD SUCCESSFUL in 18s`；`compileJava`、`compileTestJava` 和 `git diff --check` 均通过。

当前没有游戏内采样环境，因此本计划的 median/P95、实际提交次数和 allocation rate 尚未得到运行时证据。Phase 4 未实施；是否进入 Phase 4 必须由相同场景下的新 JFR/Spark/FPS 对比决定。
