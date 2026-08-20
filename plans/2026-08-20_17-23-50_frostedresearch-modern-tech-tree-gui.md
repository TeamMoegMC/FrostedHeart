# Frosted Research 现代研究工作区与完整科技树方案

- Time: `2026-08-20 17:23:50 +08:00`
- Authors: `Codex (OpenAI, product/interaction and technical planning)`
- Status: `ready`
- Scope: `com.teammoeg.frostedresearch.gui.tech`、`DrawDeskScreen`、研究 GUI 刷新协议、研究操作网络校验、可选研究展示元数据
- Related: `design/creative-principles.md`、`design/mod-and-assets.md`、`ResearchLayer`、`ResearchHierarchyPanel`、`Research.CODEC`

## Goal

把当前“绘图桌中的固定尺寸局部关系面板”升级为一个现代、可理解、可拖动缩放、能展示完整依赖关系的研究工作区，同时保留《冬季救援》的复古工业/绘图册美术，而不是改成与世界观无关的通用扁平界面。

完成后，玩家应当能够：

1. 随时打开只读科技树，查看方向、前置、进度与奖励；
2. 在绘图桌内进入同一套科技树，并执行提交材料、开始、暂停和领取奖励；
3. 用拖拽、滚轮缩放、搜索、分类、状态筛选、定位当前研究等方式浏览完整图；
4. 始终知道 `Esc`、返回按钮、关闭按钮分别会做什么；
5. 在研究数据同步时保持当前选择、画布位置和缩放，不被整页重建打断。

## Verified Current State

### 入口与退出

- `DrawingDeskBlock#use` 是现有玩家入口，服务端通过 `NetworkHooks.openScreen` 打开 `DrawDeskContainer`。
- `DrawDeskScreen` 同时持有 `DrawDeskLayer p` 和延迟创建的 `ResearchLayer r`；`showTechTree()`/`hideTechTree()` 直接切换两层可见性并开关容器槽位。
- `ResearchLayer#onKeyPressed` 把 `Esc` 分成两种隐式行为：有详情模态时关闭详情，否则调用 `onDisabled()` 返回绘图桌。
- CUI wrapper 随后还会把未消费的 `Esc` 解释为关闭整个容器。结果虽然近似一套返回栈，但栈并不存在，行为分散在子层、主层和 wrapper 中，也没有稳定的视觉提示。
- `PrimaryLayer#back()` 当前总是关闭 GUI；鼠标侧键和 Backspace 因此不能在“详情 -> 科技树 -> 绘图桌”之间逐级返回。

### 科技树展示

- `ResearchHierarchyPanel` 只渲染“选中研究 + 直接父节点 + 直接子节点”，并不是完整科技树。
- 节点位置由 `ButtonPos` 和固定坐标硬编码；父/子超过约 6 个时只尝试显示横向滚动条。
- `ResearchLayer` 固定为 `387 x 203`，主列表只有 `103 x 118`，树区域只有 `210 x 160`，无法利用现代宽屏与不同 GUI scale。
- 当前只有滚动条，没有画布拖拽、鼠标锚点缩放、适应全部、定位节点、搜索或概览。
- 详情是 `302 x 170` 的模态层。第一次点击列表节点只选择，第二次点击同一节点才打开详情，发现成本高且难以预期。

### 数据与同步

- `Research` 只有分类和父依赖，没有任何展示坐标或排序提示。
- `FHResearch#reindex` 会从父依赖反向建立 children，现有数据足以构建依赖图。
- `FHResearchSyncPacket` 使用 `Research.CODEC` 同步完整定义，因此新增带默认值的可选展示字段可以向后兼容旧 JSON，并自动进入现有同步链路。
- `ResearchGui` 只是空 marker interface。多个 S2C packet 调用 `ResearchUtils#refreshResearchGui()`，最终整棵 CUI 元素树重建，画布状态也会随实现不慎丢失。
- `FHResearchControlPacket` 与 `FHEffectTriggerPacket` 在服务端没有验证玩家是否仍在有效绘图桌容器中。当前“只能在绘图桌操作”只是客户端入口约束，不是服务端规则。

### 已有基础能力与限制

- CUI 已有裁剪、平滑滚动、鼠标拖拽事件和矩阵变换，但 `Button` 在鼠标按下时立即触发点击，直接拼装节点控件会与拖动画布冲突。
- `UILayer#setTransform` 会变换图层自身裁剪区域，不能直接拿来实现固定视口内的缩放画布。
- 因此科技树应使用一个拥有独立坐标换算、命中测试和指针状态的画布控件，而不是继续堆叠固定坐标的 `Button`。

## Product Decisions

### 1. 查看与操作分离

采用同一套 `ResearchWorkspace`，但明确区分两种打开上下文：

| 模式 | 入口 | 能力 | 退出目标 |
|---|---|---|---|
| `VIEW_ONLY` | 可配置快捷键、未来的任务/提示跳转 | 查看、搜索、筛选、定位，不允许改变服务端研究状态 | 世界 |
| `DRAWING_DESK` | 绘图桌顶部“科技树”页签、当前研究卡片 | 查看 + 提交材料 + 开始/暂停 + 领取奖励 | 先返回绘图桌，再退出到世界 |

推荐提供“随时只读查看”，因为规划研究路线不应要求玩家站在方块前；实际改变研究状态仍绑定绘图桌，保留物理设施与游戏世界的意义。

### 2. 显式导航，不再靠可见性猜状态

`DrawDeskScreen` 只管理一级工作区路由：

```text
DRAWING_DESK <-> TECH_TREE
```

`ResearchWorkspaceState` 管理树内部状态：

```text
overview -> selected node -> inspector open
              |                  |
              +---- focus ------+
```

统一返回规则：

1. 有临时浮层（搜索建议、确认框）时，`Esc/返回/鼠标侧键` 先关浮层；
2. 详情检查器在窄屏以覆盖层显示时，下一次返回关闭检查器；
3. `DRAWING_DESK` 模式下，从科技树返回绘图桌；
4. `VIEW_ONLY` 模式下，从科技树关闭到世界；
5. 顶部关闭图标始终代表“关闭整个当前 GUI”，不等同于返回；
6. 背包键仍遵循 Minecraft 习惯，直接关闭到世界。

`PrimaryLayer#back()` 应允许具体 screen/controller 覆写或委托 `NavigationController#back()`，鼠标侧键、Backspace、`Esc` 和可见返回图标调用同一条路径。

### 3. 单击即查看，操作始终可解释

- 单击节点：选中并更新右侧检查器，不再要求再次点击同一节点；
- 双击节点或点击检查器标题：平滑居中该节点；
- 点击前置/后继研究链接：选择并居中目标；
- 所有不可用操作都显示具体原因，例如缺少前置、洞见等级、材料、必要线索或不在绘图桌；
- 服务端操作进入 pending 状态，结果返回前禁用重复提交；失败必须显示服务端 reason，不依赖“点了但没反应”。

## Interaction Design

### 宽屏布局

```text
+--------------------------------------------------------------------------------+
| <- | 研究档案 | 全览 救援 生存 生产 奥术 探索 | 搜索 | 筛选 | 当前研究 | X |
+----------------------+---------------------------------------+-----------------+
| 可折叠结果/图例       |                                       | 研究检查器       |
| - 搜索结果            |          可拖动缩放的依赖图            | 名称 / 状态      |
| - 状态图例            |                                       | 前置 / 进度      |
| - 未领取奖励          |          [节点]---[节点]               | 材料 / 线索      |
|                      |              \---[节点]                | 效果 / 操作      |
+----------------------+---------------------------------------+-----------------+
| 缩放 - 100% + | 适应全部 | 定位选中 | 图模式 / 列表模式 | 状态提示           |
+--------------------------------------------------------------------------------+
```

- 逻辑宽度充足时，检查器固定在右侧，画布仍然可操作；
- 小于约 `520` 逻辑像素时，左栏收为按钮，检查器改为覆盖抽屉；
- 小于约 `380` 逻辑像素时，顶部控件分两行，操作按钮固定在检查器底部；
- 不按屏幕宽度缩放 UI 字号。只让画布内容缩放，工具栏与检查器保持可读尺寸；
- `ResearchWorkspaceLayout` 每次 screen resize 计算稳定区域，替代 `ResearchLayer` 的固定坐标。

### 画布操作

| 输入 | 行为 |
|---|---|
| 左键点节点 | 选择节点并更新检查器 |
| 左键拖空白 | 平移画布 |
| 中键拖任意位置 | 平移画布 |
| 滚轮 | 以鼠标位置为锚点缩放 |
| `-` / `+` | 分级缩放 |
| 双击空白 / “适应全部” | 显示当前投影的全部节点 |
| “定位选中” | 将选中节点平滑移到画布中心 |
| 方向键 | 在几何上最近的节点之间移动选择 |
| `Enter` | 打开/聚焦检查器 |
| `Esc` / 鼠标侧键 | 执行统一 back 规则 |

缩放范围建议 `0.55x - 1.75x`。低于 `0.75x` 时隐藏节点副标题，只保留图标、状态环和短名称，避免文字重叠。缩放和平移使用约 `120-180ms` 的功能性缓动，不做持续装饰动画。

画布必须区分点击和拖动：鼠标按下后移动小于 `3` 逻辑像素才在释放时触发节点点击；超过阈值进入 `PANNING` 并取消候选点击。这一逻辑不能复用当前按下即触发的 `Button#onMousePressed`。

### 信息层级

顶部类别增加虚拟的“全览”，不修改 `ResearchCategory` 枚举。分类视图不重新排版，只高亮本分类并保留必要的跨分类前置节点，避免切分类后图形跳动或出现断边。

搜索只索引当前允许玩家知道的文本，至少支持：翻译后名称、research ID、类别、已公开效果。搜索和状态筛选默认“高亮命中、淡化其他”，不删除节点和重排图；另提供列表模式用于精确扫描和键盘操作。

检查器固定呈现：

1. 名称、类别、当前状态、无限研究等级；
2. 前置条件与缺失原因，条目可点击跳转；
3. IOPS 进度、洞见需求、必要材料；
4. 线索及其完成状态；
5. 已公开效果、未领取效果；
6. 当前上下文允许的主操作。

### 节点状态

不要继续把完成、锁定和错误都主要依赖文字颜色。由单一 `ResearchNodeStateResolver` 计算互斥主状态与附加徽标：

| 主状态 | 含义 | 表达 |
|---|---|---|
| `UNKNOWN` | 只允许知道存在，不允许知道内容 | 问号图标、无名称 |
| `LOCKED` | 已公开但前置未完成 | 低对比节点、锁标、缺失前置边 |
| `AVAILABLE` | 前置满足，可提交材料 | 正常节点、细亮边 |
| `PREPARED` | 材料已提交但未作为当前研究 | 实线边、待开始徽标 |
| `IN_PROGRESS` | 当前研究 | 明确脉冲一次的状态环 + 进度弧 |
| `BLOCKED` | 正在进行但受必要线索阻挡 | 警示徽标，检查器说明原因 |
| `COMPLETED` | 已完成 | 完成印章，不降低名称可读性 |

`UNCLAIMED_REWARD`、`INFINITE_LEVEL` 等作为附加徽标，不再通过每秒闪烁文字表达。颜色仍沿用现有羊皮纸、木、铁和暗红/绿色语义，保证现代的是交互逻辑，而不是丢掉复古未来美术。

## Graph Projection And Layout

### 图模型

新增不可变客户端模型，禁止渲染层直接遍历和修改 `FHResearch.researches`：

```java
ResearchGraphSnapshot
  Map<String, ResearchGraphNode> nodes
  List<ResearchGraphEdge> edges
  long definitionRevision

ResearchGraphNode
  id, category, parentIds, childIds, displayHints
```

`ResearchGraphProjection` 根据玩家研究数据生成可见投影：

- 已完成、进行中、已解锁、`keepShow` 节点显示完整内容；
- 已公开但锁定的节点显示完整或受限内容；
- 只在探索前沿可见的未知节点显示 `UNKNOWN`；
- `hidden` 节点完全排除，不进入搜索、tooltip、minimap 或计数；
- 跨分类依赖保留为淡化桥接节点，边永远不悬空。

隐藏策略必须先于搜索文档构建，避免通过搜索泄漏秘密研究名称或效果。

### 确定性分层布局

采用从左到右的分层 DAG 布局，不引入大型图形依赖：

1. 校验父 ID，缺失引用记诊断并跳过该边；
2. 用 Tarjan SCC 折叠环，保证错误数据不会卡死布局；编辑模式下把环成员标红并列出诊断；
3. 在缩合 DAG 上按最长路径确定 `rank`；
4. 按可选 author hint、类别、稳定 ID 生成初始 lane 顺序；
5. 做固定次数的上下行 barycentric sweep，减少交叉；
6. 按 `rankGap`、`laneGap` 放置节点，并纵向打包不连通分量；
7. 计算 world bounds，供 `fitAll()`、裁剪和 minimap 使用。

布局必须在相同定义输入下确定性输出，不能依赖 `HashSet`/registry 遍历顺序。定义同步或编辑器保存后才重算；研究进度变化只更新节点状态，不重算布局。

### 可选作者提示

自动布局是默认路径。为处理叙事顺序、跨分类依赖和美术构图，在 `Research.CODEC` 增加可选字段：

```json
"display": {
  "rank": 3,
  "order": 20
}
```

- `rank` 是科技阶段列，不是像素 X；
- `order` 是同 rank 内的相对顺序，不是像素 Y；
- 字段缺失时完全自动布局，旧 JSON 无需迁移；
- in-game editor 后续暴露这两个高级字段；
- 现有 `FHResearchSyncPacket` 已通过 `Research.CODEC` 同步，无需另造布局同步包。

避免保存绝对像素坐标，因为它会把数据绑定到分辨率、节点尺寸和当前美术资源。

## Technical Architecture

### 状态与控制器

```text
ResearchOpenContext
  mode: VIEW_ONLY | DRAWING_DESK
  initialResearchId
  returnTarget

ResearchWorkspaceController
  ResearchWorkspaceState
  ResearchGraphSnapshot
  ResearchGraphProjection
  ResearchActionClient
  back(), select(), focus(), filter(), switchView()

ResearchWorkspaceLayer
  Toolbar
  Search/Legend panel
  ResearchGraphViewport | ResearchListView
  ResearchInspectorPanel
```

`ResearchWorkspaceState` 只保存 UI 状态：category filter、search、status filters、selected ID、inspector open、view mode、camera center、zoom。服务端研究数据继续由 `TeamResearchData`/`ResearchData` 持有，不能复制成第二份可写真相。

`ClientResearchUiSessionStore` 在当前客户端会话内按 world/team 保存最后选择、分类、视图模式和相机。重新进服或研究定义 revision 变化时校验 ID 并执行安全回退：当前研究 -> 最近选择 -> 第一个 available -> fit all。

### 专用画布

新增 `ResearchGraphViewport extends UIElement`，由它统一负责：

- `worldToScreen` / `screenToWorld`；
- 浮点 camera center 与 zoom；
- 固定视口裁剪；
- edge-first、node-second 的批量绘制；
- world rect 可见性裁剪；
- 节点空间索引与命中测试；
- `IDLE / PRESSED_NODE / PANNING` 指针状态；
- 鼠标锚点缩放和 fit/focus 动画；
- hovered node tooltip 与键盘邻近导航。

不为每个节点创建普通 CUI `Button`。这样可避免按下即点击、嵌套 transform 裁剪错误和每次 packet 到来重建大量控件。

### 增量刷新

把空 marker `ResearchGui` 改为有语义的刷新契约：

```java
interface ResearchGui {
    void onResearchDefinitionsChanged();
    void onResearchProgressChanged(String researchId);
    void onActiveResearchChanged(@Nullable String researchId);
}
```

- registry/sync end：重建 snapshot、projection、layout，并尽量保留选中与 camera；
- 单研究进度包：只重算该节点及依赖它的直接后继状态；
- active research 包：更新旧/新 active 节点和顶部当前研究卡；
- effect/clue 包：更新对应节点与检查器局部内容；
- 不再把所有更新都降级为 `PrimaryLayer#refreshElements()`。

### 研究操作与服务端权限

新增统一 action request/result，而不是继续让按钮“发包后等待某个同步也许发生”：

```text
C2S ResearchActionRequest(requestId, researchId, action)
S2C ResearchActionResult(requestId, accepted, reasonKey)
```

服务端 `ResearchActionService` 依次校验：

1. sender、research ID、team data 存在；
2. action 是否要求绘图桌；
3. `sender.containerMenu` 是否为有效 `DrawDeskContainer`，方块仍存在且距离有效；
4. 研究前置、材料、洞见、线索、完成/领取状态；
5. 校验成功后才修改 `TeamResearchData`，并沿用现有 S2C 数据同步。

默认策略为所有改变研究状态的动作都要求 `DRAWING_DESK`。若设计以后决定“暂停可随时操作”，只修改 `ResearchAccessPolicy`，不改 screen 和 packet handler。

`FHResearchControlPacket` 与 `FHEffectTriggerPacket` 在迁移期委托同一个 service；新 UI 稳定后再移除旧入口。客户端禁用按钮只是体验，服务端校验才是权限边界。

## Visual Direction

- 继续使用 `escritoire.png`/`DrawDeskTheme` 的羊皮纸、木框、金属结构和红色标签；
- 从固定整张背景改为可伸缩的 nine-slice 框、分隔线、标签与节点皮肤，支持任意 workspace 尺寸；
- 完成态使用“盖章”，进行态使用机械刻度/进度环，未知态使用现有问号语言；
- 连接线按状态改变材质/明度，不使用高饱和霓虹或通用 SaaS 风格；
- 动效只用于说明状态变化：抽屉、节点定位、解锁边、完成盖章，各自一次且可被快速操作打断。

这与 `design/creative-principles.md` 的沉浸、好奇心和解决问题的成就感一致，也遵守 `design/mod-and-assets.md` 的复古未来与像素资产方向。

## Implementation Steps

### Phase 1: 纯模型与导航骨架

1. 新增 `ResearchOpenContext`、`ResearchWorkspaceState`、`ResearchNodeStateResolver`；
2. 新增 `ResearchGraphSnapshot`、projection 与确定性 layout engine；
3. 为图校验、环处理、排序、投影隐私和导航 back 规则写单元测试；
4. 给 `FHResearch` 增加 definition revision；
5. 保留旧 `ResearchLayer`，先不改变玩家界面。

### Phase 2: 新工作区与完整画布

1. 实现响应式 `ResearchWorkspaceLayer`、toolbar、graph viewport、inspector；
2. 在 `DrawDeskScreen` 增加显式 `DRAWING_DESK/TECH_TREE` route；
3. 让绘图桌当前研究卡可直接进入并定位当前节点；
4. 实现拖拽、缩放、fit、focus、搜索、分类/状态高亮和列表模式；
5. 迁移旧详情中的材料、线索、效果和 action 控件；
6. 用新工作区替换默认入口，旧 hierarchy/list/detail 保留一个开发 fallback。

### Phase 3: 只读入口与服务端 action service

1. 新增非容器 `ResearchBrowserScreen`，复用相同 workspace；
2. 注册可配置快捷键，默认键位需要实现时与整合包现有键位表确认；
3. 引入 action request/result、pending 与本地化失败原因；
4. 服务端校验绘图桌上下文，旧 packet 委托 service；
5. 覆盖鼠标侧键、Backspace、`Esc`、关闭图标和背包键的统一返回测试。

### Phase 4: 作者提示、收尾与删除旧 UI

1. 给 `Research.CODEC` 增加可选 `display.rank/order` 并补编辑器字段；
2. 用真实整合包研究数据检查交叉数和布局，需要时只加 rank/order 提示；
3. 删除 `ResearchHierarchyPanel`、旧 `ResearchListPanel`、旧模态 `ResearchDetailPanel`；
4. 清理 `showTechTree()/hideTechTree()` 的直接可见性切换和通用全量刷新；
5. 完成 living docs、语言键、资源与 diary。

## Validation

### 自动测试

- 同一图以不同 registry/HashSet 顺序输入，节点坐标完全一致；
- 多父节点、跨分类、孤立分量、缺失父 ID、自环和多节点环都能完成布局；
- `hidden` 研究不会进入 projection、搜索、tooltip、minimap 或统计；
- status 解析覆盖 available/prepared/in-progress/blocked/completed/unclaimed/infinite；
- `back()` 在两种 open mode 与 inspector/overlay 状态下符合状态表；
- viewport 的 screen/world 互转、鼠标锚点缩放和边界 clamp 可逆且无漂移；
- action service 拒绝非绘图桌、失效方块、越界 ID、重复请求与不满足条件的动作；
- 旧 research JSON 缺少 `display` 字段时仍能加载并得到自动布局。

### 手工 QA 矩阵

- 逻辑分辨率至少覆盖 `320x240`、`427x240`、`640x360`、`960x540`；
- GUI scale、窗口 resize、语言切换和超长中英文名称不重叠；
- 空白拖拽、从节点开始拖拽、中键拖拽、滚轮锚点缩放、快速反向缩放均不误点；
- 打开详情、切分类、搜索、收到进度包后，camera 和 selected node 不跳回默认；
- drawing desk -> tree -> inspector -> back -> desk -> world 顺序稳定；
- hotkey -> tree -> inspector -> back/close -> world 顺序稳定；
- 断线、研究定义 reload、当前研究被删除时安全回退；
- 对真实科技树执行全览，确认连接线不穿过节点、跨分类边可辨认、未知研究不泄漏。

### 性能门槛

- 目标数据量 `250` 节点、`500` 边时，定义更新后的布局在普通客户端单次低于约 `25 ms`；
- 静止画布不产生每帧集合分配；
- 只渲染 viewport 扩展边界内的节点和相关边；
- 平移缩放在目标数据量下保持 `60 FPS`，研究进度包不能触发布局重算。

## Acceptance Criteria

1. 玩家第一次打开即可通过可见控件理解返回、关闭、搜索、分类、缩放和定位；
2. 任意可见研究最多一次点击即可看到详情和不可用原因；
3. 完整依赖图可平移缩放，不再受固定父/子数量和 `210 x 160` 面板限制；
4. 视图更新不丢失选择、分类、camera 或 zoom；
5. 小屏没有文字/按钮/检查器互相遮挡；
6. 只读模式无法通过手工发包绕过绘图桌操作规则；
7. 旧研究定义与存档无需迁移即可使用；
8. 自动布局对异常图给出诊断而不是崩溃或无限循环；
9. 美术仍明显属于《冬季救援》的研究档案/复古工业系统。

## Documentation Impact

实现时新增 `docs/research/README.md`，记录入口、模式、状态、操作规则与代码锚点；新增 `docs/research/research-ui.md`，记录导航状态机、画布输入、布局算法、`display.rank/order`、刷新协议与服务端权限。并在 `docs/README.md` 增加 Frosted Research 条目。

如果实际实现阶段只完成其中一部分，plan 状态不得标为 completed；应在 Outcome 中明确完成的 phase、fallback 是否仍存在及下一份 plan。

## Assumptions To Confirm During Implementation

- 默认采用“随时只读查看，绘图桌内修改”的权限模型；
- 所有改变研究状态的 action 默认要求有效绘图桌容器；
- 图默认从左到右，类别是过滤/强调维度，不是五张完全独立的树；
- `hidden` 表示完全保密，不能通过图结构之外的 UI 泄漏；
- 快捷键默认值必须先检查整合包按键冲突，方案不预先锁死具体按键。

这些假设都被隔离在 `ResearchOpenContext`、`ResearchAccessPolicy`、projection 和 key mapping 中，产品决定变化时不需要重写画布或布局引擎。

