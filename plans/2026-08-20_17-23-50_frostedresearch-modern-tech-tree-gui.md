# Frosted Research 现代研究 GUI 与完整科技树方案

- Time: `2026-08-20 17:23:50 +08:00`
- Last updated: `2026-08-21 02:46:39 +08:00`
- Authors: `Codex (OpenAI, product/interaction and technical planning)`
- Status: `in-progress`
- Scope: `com.teammoeg.frostedresearch.gui`、`DrawDeskScreen` 的客户端导航、完整科技树画布、研究详情与 clue 待做清单表现
- Related: `design/creative-principles.md`、`design/mod-and-assets.md`、`ResearchLayer`、`ResearchHierarchyPanel`、`ResearchDetailPanel`、`ResearchInfoPanel`、`CluePanel`、`DrawDeskLayer`、`DrawDeskScreen`

## Revision Decisions

本版保留已经确定的完整 GUI 设计，只把业务边界收窄到当前研究逻辑：

1. 可拖动、缩放的玩家当前可知科技树是 Frosted Research 主界面，不再是绘图台里的固定局部父子关系面板；普通模式不得接收或展示尚不可知定义；
2. 研究领域图标页签与档案标题、绘图台按钮共用最顶部同一栏；搜索框位于左侧项目列表正上方，左栏不再混排领域分类；
3. 右侧常驻区域只显示所选项目的简洁摘要；点击科技节点后，在同一主界面中央打开小型项目档案弹窗，不创建第二个 Minecraft `Screen`；
4. 居中项目档案保留 `研究详情 / 理论研究 / 实验研究` 三个页签，关闭弹窗后完整树和 camera 不重建；
5. 弹窗使用固定上限和屏幕安全边距响应式缩小，右侧摘要始终保持简洁，不承担完整详情；
6. 绘图台 screen 保留 `[研究档案] [绘图台]` 两个显式顶层模式；切页签不会自动切换模式，只有玩家点击中转按钮才返回绘图台对应区域；
7. 节点布局继续采用自动排布与作者手动锚点混合模式，过滤和搜索不得重新计算坐标；
8. 详情页显示全部真实 `Clue`，包括 `MinigameClue`；理论页保留 `MinigameClue` 的专门筛选视图，并用简短动作说明玩家要做什么；
9. clue 的类型、顺序、`required`、`value/contribution`、完成状态和触发方式全部沿用当前 `Clue`/`ClueData`；
10. 一切需要玩家完成的条件在详情 GUI 中都必须可见；现有实验点数目标由详情页状态和进度区显示，不在实验页重复创建可点击任务；
11. 现有 `Research.points / ResearchData.committed` 是实验点数，不是理论 IOPS；当前 `EXPERIMENT` 页保持空接口，留给未来城镇系统接入，不承载材料、普通线索或实验点数；
12. clue 行上的按钮只是中转：切换到绘图台对应区域、定位当前研究操作，或关闭 GUI 返回世界；按钮本身不提交物品、不增加点数、不完成 clue；
13. 提交物品、开始/暂停研究、绘图小游戏结算、实验点数、击杀/成就/世界事件触发和领奖继续发送当前 packet、调用当前 hook，并由当前 `TeamResearchData`/`ResearchData` 完成判定；
14. 不引入 provider、task、work order、城镇研究建筑、实验台、活动会话或新研究存档模型；未来设备只保留 UI 扩展位置，不在本计划实现玩法。

## Goal

把当前固定尺寸、只展示直接父子节点的研究界面升级为统一研究主界面：完整科技树始终挂载，支持拖动、缩放、搜索、定位和类型过滤；右侧只显示简洁项目摘要，点击节点在树上方打开居中小型项目档案。研究详情用现有 `Clue` 告诉玩家“还要做什么”，中转按钮把玩家带回现有玩法入口，但所有完成与结算仍走当前逻辑。

完成后玩家可以：

1. 随时打开研究档案并直接看到完整科技树，通过左上类型过滤器快速收窄范围；
2. 从左下紧凑列表扫描当前类型全部可知科技，并访问跨类型未领取奖励；
3. 选择列表项目时在右侧查看简洁摘要，单击树节点后打开居中项目档案，在详情、理论、实验三个页签间切换；
4. 关闭项目档案后继续使用原 camera、zoom、过滤和选择状态；
5. 在项目标题下看到现有 `Clue` 待做清单、完成状态、必要标记和简短说明；
6. 点击 clue 中转按钮前往现有物品研究区、理论小游戏或返回世界完成对应事项；
7. 在科技树与绘图台之间往返时保留项目、页签、类型、列表、camera、zoom 和滚动位置；
8. 自动布局保证新增研究可用，作者手动锚点可以修正关键位置、视觉节奏和连线交叉；
9. 继续以现有方式提交、研究、触发 clue、完成项目和领取奖励，旧定义与旧存档行为不变。

## Scope Boundary

### In scope

- 完整科技树画布、确定性布局、拖拽、缩放、搜索、分类和状态筛选；
- 左上研究类型过滤器、左下类型列表、跨类型未领取奖励区；
- 右侧项目简报、居中项目档案及 `DETAIL/THEORY/EXPERIMENT` 三页签；
- `[研究档案] [绘图台]` 顶层模式、来源感知返回和双侧 UI 状态保留；
- 自动布局与作者手动锚点混排所需的纯展示元数据和编辑模式；
- 把现有 `CluePanel` 重做为清晰的待做清单行；
- clue 到现有绘图台/世界入口的纯客户端中转；
- 明确的返回路径和 UI 状态保留；
- 将当前全量 GUI 重建收窄为定义更新与进度更新两类刷新。

### NOT in scope

- 不修改 `Clue.CODEC`、`ClueData.CODEC` 或 `ResearchData.CODEC`；`Research.CODEC` 只允许增加带默认值的 `display.layout` 展示字段；
- 不新增 `ResearchTask`、`ResearchProjectService`、provider capability/session 或新的 contribution 协议；
- 不改变 `ResearchData#getTotalCommitted`、`ResearchData#canComplete`、`TeamResearchData` 的完成判定；
- 不改变 `ResearchHooks#submitItem`、`fetchGameLevel`、`commitGameLevel`、`kill`、`tick` 的目标选择和触发规则；
- 不改变 `FHResearchControlPacket`、`FHDrawingDeskOperationPacket`、`FHEffectTriggerPacket` 的业务语义；
- 不新增城镇研究方块、实验台、多人并行研究、材料 ledger 或新的奖励债权模型；
- 不批量迁移整合包 research JSON；允许新增带默认值的可选 `display.layout` 展示字段，但它不得参与解锁、进度或完成判定。

## Verified Current State

### 当前研究真相

- `Research` 保存 `ingredients`、`points`、`clues`、`effects`、父依赖和分类；
- `ResearchData` 保存 `active/committed/clueData/finished/level/effectData`；
- `Research.points` 是完成研究所需的实验点数上限，`ResearchData.committed` 是当前直接提交的实验点数；
- `ResearchData#getTotalCommitted` 把 `committed` 与已触发 clue 的 `contribution` 合并成有效实验点数，`ResearchData#getProgress` 以它计算总进度；
- `ResearchData#canComplete` 检查所有 `required` clue 是否完成；
- `ClueData.completed` 是 clue 勾选状态，部分 clue 的类型数据仍保存在现有 `data` 字段；
- 当前代码没有为实验点数创建真实 `Clue` 实例，但它仍是必做条件；GUI 需要把它适配成一条只读“系统 clue”，保证所有完成条件都出现在同一待做清单中；
- 系统 clue 只读取 `getTotalCommitted/getRequiredPoints`，不能新增 `ClueData`、nonce、NBT 或另一套完成判断。

### 当前操作路径

```text
提交研究材料
  ResearchInfoPanel
    -> FHResearchControlPacket(COMMIT_ITEM)
    -> 当前服务端逻辑

绘图台研究物品
  EXAMINE_SLOT + DrawDeskLayer.itemSubmit
    -> FHDrawingDeskOperationPacket(blockPos, 3)
    -> ResearchHooks.submitItem(...)

理论研究
  MainGamePanel
    -> ResearchHooks.fetchGameLevel()
    -> 当前未完成 MinigameClue
    -> ResearchHooks.commitGameLevel(...)

世界 clue
  kill / advancement / tick / custom listener
    -> 当前事件监听器
    -> TeamResearchData#setClueCompleted(...)

领取效果
  ResearchInfoPanel
    -> FHEffectTriggerPacket
    -> 当前领奖逻辑
```

新 GUI 的实际操作按钮必须复用上面这些入口。中转按钮只改变客户端页面或关闭 GUI，不出现在这条业务调用链里。

### 当前 GUI 限制

- `ResearchHierarchyPanel` 只显示选中科技的直接父节点和直接子节点；
- `ResearchLayer` 固定为 `387 x 203`，树区域只有约 `210 x 160`；
- 当前没有完整图、空白拖拽、鼠标锚点缩放、适应全部和全局搜索；
- `ResearchDetailPanel` 是固定 `302 x 170` 模态层，第一次点击节点只选择，第二次才打开；
- `CluePanel` 同时显示名称、描述、required 和 `+百分比`，但缺少清晰的“要做什么 / 去哪里做”层级；
- `showTechTree()/hideTechTree()` 只切换可见性与槽位，返回行为分散在 layer 和 screen；
- `ResearchGui` 是空 marker，多个同步事件可能整页 `refreshElements()`，容易丢视图状态。

## Preserved Behavior Contract

GUI 重做前先把以下行为视为回归契约：

1. `Research#getClues()` 的定义顺序仍是待做清单的基础顺序；当前未完成的 required clue 可以视觉置顶，但不得改变底层列表或 nonce；
2. clue 是否勾选只调用现有 `ResearchData#isClueTriggered` / 客户端同步查询；
3. clue 行不提供“手动完成”能力；
4. `Clue#getResearchContribution()` 仍参与当前研究进度计算，但主清单行不再用醒目的 `+百分比` 抢占动作说明；需要时放进 tooltip/展开说明；
5. `ResearchData#canResearch`、`Research#isInProgress`、`ResearchData#canComplete` 和 `Research#hasUnclaimedReward` 继续决定按钮与节点状态；
6. 所有现有 C2S packet 的发送时机只允许由原有实际操作按钮触发；
7. 从新 GUI 中转到绘图台后，玩家仍需放入物品并点击现有提交按钮，或在现有 `MainGamePanel` 中完成小游戏；
8. 切换页面、选中 clue、返回详情、拖动树、搜索和筛选不得发送研究业务 packet。

## Interaction Design

### 科技树主界面

```text
+--------------------------------------------------------------------------------+
| [研究类型: 全部 v] | 研究档案 | 搜索 | 状态筛选 | 本地关注 | X             |
+--------------------------------------------------------------------------------+
|                                                                                |
|                    可拖动、缩放的完整科技树画布                                 |
|                                                                                |
|       [节点]----[节点]----[节点]                                                |
|          \          \----[节点]                                                |
|           \----[节点]                                                          |
|                                                                                |
| [待领取奖励 / 当前类型科技列表]        [-] 100% [+] [适应全部] [定位关注]       |
+--------------------------------------------------------------------------------+
```

- 画布是视觉与操作主角；左上过滤器和左下列表叠放在画布安全区内，不切走主内容；
- 单击节点在原树上打开居中项目档案弹窗，不发生 `Screen` 跳转，也不销毁画布；
- 搜索和状态筛选默认高亮命中、淡化其他节点，不删除节点并重排；
- 字号不随屏幕宽度缩放，只缩放科技树世界坐标；
- 窄屏顶部可以分两行，左下列表折叠成图标/抽屉，但必须给画布保留稳定操作区域；
- `hidden` 研究不进入画布、搜索、列表、tooltip、奖励区或计数。

### 左上研究类型过滤器

`ResearchTypeFilter` 固定在科技树左上角，数据源直接复用现有 `Research.category`。客户端展示适配器 `ResearchTypeIdNormalizer` 负责把现有 namespace alias 归一为同一个过滤项，但不修改研究定义：

- 第一项是“全部”，其后按稳定顺序显示救援、生活、生产、奥术、探索；
- `frostedheart:*` 与 `frostedresearch:*` 的同义分类不得显示成重复类型或伪跨类型边；
- 选择某类型后，该类型节点完整显示；必要的跨类型前置节点与桥接边保留为低对比上下文，其余节点淡化或隐藏；
- 搜索和状态筛选在当前类型投影内工作，切回“全部”才执行全树搜索；
- 过滤只改变 projection，不重算节点坐标；
- 每个类型分别记忆 camera/zoom，第一次进入执行 `fitCurrentType()`，之后恢复上次视角；
- 项目档案打开时过滤器仍属于底层科技树并保持状态，但由模态遮罩阻止误操作；
- 当前类型在 definition reload 后失效时回退“全部”，并显示明确空态而不是空白画布。

### 左下紧凑列表与奖励区

`ResearchTypeListPanel` 固定在左下角，继续使用当前“图标 + 名称 + 状态”的高密度扫描方式：

```text
+---------------------------+
| 待领取奖励 (2)            |
| [icon] 热能交换器    [领取]|
| [icon] 城镇供暖学    [领取]|
+---------------------------+
| 本类型科技：生产 (14)     |
| [icon] 蒸汽动力       [✓] |
| [icon] 高压锅炉       [▶] |
| [ ? ] 未知研究        [锁] |
| ...                   [↕] |
+---------------------------+
```

- “本类型科技”显示当前类型下玩家按现有可见性规则可知的全部科技，不受当前视口限制；
- 未领取奖励独立置顶并跨类型汇总；点击普通行主体只切换选择和右侧简报，点击奖励操作才继续调用现有领奖 packet；`BROWSE` 只显示待领取标记；
- 已完成但有未领取奖励的研究同时出现在类型列表与奖励区；
- 画布节点与右侧简报的详情动作调用 `openProject(researchId)`；普通列表行只调用 `selectResearch(researchId)`；
- 宽屏常驻，窄屏折叠为左下图标，抽屉展开高度不超过屏幕约 `55%`；
- 列表展开/折叠不改变图世界坐标，也不触发布局重算。

### 右侧项目简报与居中项目档案

```text
+--------------------------------------------------------------------------------+
| [研究类型: 生产 v] | 搜索 | 状态筛选 | 本地关注 | X                          |
+----------------------+---------------------------+-------------------------+
|                      | [居中项目档案小弹窗]       | [icon] 项目简报          |
|  完整科技树画布       | [详情] [理论] [实验]       | 状态 / 进度 / 一句摘要   |
| [节点]--[选中节点]    | clue 与现有研究操作        | [打开项目档案]           |
| [类型 / 项目列表]     | [关闭]                    |                         |
+----------------------+---------------------------+-------------------------+
```

- `ResearchProjectSummaryPanel` 常驻右侧，只显示名称、分类、状态、进度和一句摘要；
- `ResearchProjectWorkspace` 是同一 `ResearchScreen` 内的固定居中小弹窗，不是新的 screen；
- 弹窗宽高使用固定上限，并按屏幕安全边距缩小；点击遮罩或关闭按钮收起弹窗；
- 弹窗打开时底层 camera、zoom、过滤和选择保持不变；关闭后不重新布局；
- 项目标题、总进度、clue 摘要和页签固定；页签正文独立垂直滚动；
- 详情不再使用卡片套卡片；前置、材料、clue、效果用标题、分隔线与可点击行组织，只有物品槽等真实工具区才使用有边界面板。

### 项目三页签

三个页签只重新组织现有 `Research`、`ResearchData` 和 `Clue` 的展示，不定义新的完成轨道：

| 页签 | 展示内容 | 允许的操作 |
|---|---|---|
| `DETAIL` 研究详情 | 名称、描述、分类、前置、洞见、requiredItems、完整 clue 待做清单、效果、等级和总状态 | 复用现有开始/暂停/提交材料/领奖；clue 中转 |
| `THEORY` 理论研究 | 当前 IOPS、理论相关 `MinigameClue`、现有绘图小游戏说明与来源 | 仅“前往绘图台理论区”；不在页签内新增结算 |
| `EXPERIMENT` 实验研究 | 空状态；保留给未来城镇研究系统接入 | 无现有研究物品、clue 或实验点操作 |

- 上表的实际研究按钮只在 `DRAWING_DESK` wrapper 中复用现有控件；`BROWSE` wrapper 只显示状态、阻塞原因和所需入口；
- 页签归类是 `CluePresentationClassifier` 的客户端展示规则，不写回 clue 类型，也不改变 contribution/required；
- 某页没有对应 clue 时显示紧凑空态，不隐藏整个工作层或创造虚构任务；
- 普通节点首次打开默认 `DETAIL`；切换节点时保留当前页签，目标无对应内容时回退 `DETAIL`；
- clue 中转完成后再次打开档案，恢复原项目与原页签；
- 点击 `THEORY/EXPERIMENT` 页签本身不进入绘图台、不关闭 GUI、不发送 packet。

### 研究档案与绘图台顶层模式

```text
[研究档案] [绘图台]

RESEARCH_ARCHIVE
  完整科技树 -> 居中项目档案 -> DETAIL / THEORY / EXPERIMENT

DRAWING_DESK
  当前研究摘要 -> EXAMINE/PAPER/INK -> MainGamePanel -> 玩家背包
```

- 使用绘图台打开时默认进入现有绘图台工作页，顶部始终能切换到“研究档案”；
- 绘图台工作面保持现有 `387 x 203` 逻辑布局、`EXAMINE/PAPER/INK` 槽位、玩家背包、帮助层和 `MainGamePanel`；外层 screen 可以响应式扩展，但不能拉伸槽位坐标或复制一套背包；
- 点击绘图台当前研究名称或“研究档案”时，档案定位当前研究并打开项目；没有当前研究时只显示完整树；
- clue 中转按钮从档案切回绘图台并设置一次性 focus target；仅查看页签不会切换；
- 顶层模式切换不销毁任何一侧：树 camera/filter/list/project scroll 与绘图台 slots/game/help state 分别保留；
- 来源感知返回顺序为“项目档案 -> 科技树 -> 绘图台 -> 关闭 Screen”；
- 本计划只实现绘图台工作区。未来设备可以复用顶层模式位置，但不在这里定义 provider、槽位、完成规则或数据模型。

### 普通只读入口

为了满足“随时查看科技树”，使用相同 `ResearchArchiveLayer` 增加无容器 `ResearchBrowserScreen`：

- 普通入口只挂载研究档案，不显示 `[绘图台]` 模式和 START/PAUSE/COMMIT_ITEM/CLAIM 等现有实际操作按钮；
- clue 中转行显示“需要使用绘图台”或“返回世界”，不能伪造绘图台槽位或发送原本只能从绘图台发起的 packet；
- 深链可以指定 `initialResearchId + projectTab + clueNonce`，只负责初始定位；
- `Esc/返回` 顺序为“项目档案 -> 科技树 -> 关闭 Screen”；
- 普通入口与绘图台入口共享布局、过滤、列表、项目简报/档案和客户端状态，不维护第二套科技树实现。

### Clue 待做清单

`ResearchClueListPanel` 直接遍历当前 `Research#getClues()`，每条使用重做后的 `ResearchClueRow`：

```text
研究待做
  ☑ 研究阀门样品                         [已完成]
  ◐ 完成理论研究                         [前往绘图台]
  ☐ 完成低温燃烧实验                     [返回世界]
  ☐ 找到旧时代实验记录          [必需]   [查看说明]
```

每行固定包含：

1. 当前完成标记，来源只能是现有 `ClueData`；
2. `Clue#getName(research)` 返回的简短动作说明；
3. 未完成 required clue 的“必需”标记；
4. 可选的中转按钮；
5. `getDescription`、`getHint` 与 contribution 放入展开区或 tooltip，不挤占主行。

排序只属于 UI：未完成 required -> 其他未完成 -> 已完成，同组内保持原定义顺序。不得写回定义、nonce 或 `ClueData`。

### Clue 中转规则

新增纯客户端 `ClueDestinationResolver`，只决定“去哪里”，不决定“怎么完成”：

| 当前 clue | 中转目标 | 按钮行为 | 业务行为 |
|---|---|---|---|
| `ItemClue` | 绘图台物品研究区 | 切回 `DrawDeskLayer`，聚焦/高亮 `EXAMINE_SLOT` 与现有提交按钮 | 无；玩家随后使用现有 item submit |
| `MinigameClue` | 绘图台理论研究区 | 切回 `DrawDeskLayer`，聚焦 `MainGamePanel` | 无；小游戏仍读当前研究和现有 clue |
| `KillClue` / `AdvancementClue` / tick/world clue | 世界 | 显示简短确认后关闭 Screen，或仅显示“返回世界” | 无；仍由世界事件触发 |
| `CustomClue` | 可配置展示目标；未知时仅查看说明 | 打开说明或无按钮 | 无；不猜测自定义完成方式 |
| 已完成 clue | 无 | 隐藏中转按钮，保留完成状态与说明 | 无 |

首版 resolver 可以基于现有 clue 子类做 `instanceof` 映射。这是表现层适配，不修改 `Clue` 类层级或 codec。后续若确有多个自定义 GUI 目标，再单独增加可选展示元数据；不能为了本次 GUI 先造通用 provider 系统。

### 中转前置条件

- 只有从 `DrawDeskScreen` 打开的 `DRAWING_DESK` 上下文能中转到绘图台区域；
- 只读科技树若保留普通入口，物品/理论 clue 显示“需要使用绘图台”，不伪造可点击入口；
- `MinigameClue` 只有在其研究是当前研究，且仍是当前逻辑会选中的未完成小游戏 clue 时才显示“前往理论研究”；否则显示“先开始此研究”或“先完成前一项理论研究”；
- `ItemClue` 中转不会自动选择研究、搬动物品、点击提交或发送 packet；
- 中转目标不存在、绘图台被关闭或 screen resize 时安全回到研究详情，不丢树状态。

### 导航状态

```text
DRAWING_DESK
   |
   +--> TECH_TREE(selectedResearch, camera, filters)
            |
            +--> DETAIL(selectedClue)
            |       |
            |       +--[中转]--> DRAWING_DESK(focusTarget)
            |
            +--[back]--> TECH_TREE --> DRAWING_DESK --> close
```

`ResearchWorkspaceState` 仅保存客户端 UI 状态：分类、搜索、筛选、selected research、selected clue nonce、详情开关、camera、zoom、列表滚动。切到绘图台时保留该对象；玩家再次打开科技树就回到原位置。

`Esc`、鼠标侧键、Backspace 和可见返回按钮统一调用 `ResearchNavigationController#back()`。顶部 `X` 仍代表关闭整个 Screen，不等同于返回。

### 画布输入

| 输入 | 行为 |
|---|---|
| 左键点节点 | 选择节点并打开/更新详情 |
| 左键拖空白 | 平移画布 |
| 中键拖任意位置 | 平移画布 |
| 滚轮 | 以鼠标位置为锚点缩放 |
| `-` / `+` | 分级缩放 |
| 双击空白 / “适应全部” | 显示当前投影全部节点 |
| “定位选中” | 平滑居中选中节点 |
| 方向键 | 选择几何上最近节点 |

缩放范围为 `0.15x - 1.75x`，“适应全部”居中当前投影并固定使用 `0.15x`。鼠标按下后移动小于 `3` 逻辑像素才在释放时触发点击；超过阈值进入 `PANNING`，避免从节点开始拖动时误开详情。

### 节点状态与徽标

由单一 `ResearchNodeStateResolver` 读取现有 `ResearchData`，计算互斥主状态与附加徽标；它不创建新的研究状态：

| 主状态 | 现有数据含义 | 节点表达 |
|---|---|---|
| `UNKNOWN` | 只允许知道存在，不允许知道内容 | 问号图标，不显示真实名称 |
| `LOCKED` | 已公开但父研究未完成 | 低对比节点、锁标和缺失前置边 |
| `AVAILABLE` | 前置满足，可按当前逻辑提交材料 | 正常节点、细亮边 |
| `PREPARING` | 已有准备动作但尚未 `canResearch`；若现有数据无法区分则不启用 | 材料徽标与准备提示 |
| `PREPARED` | `canResearch` 但不是当前研究 | 实线边、待开始徽标 |
| `IN_PROGRESS` | `Research#isInProgress()` | 状态环与现有总进度弧 |
| `BLOCKED` | 当前进度满足但 required clue 未完成，或现有 `canComplete` 为 false | 警示徽标，项目档案列出未完成 clue |
| `COMPLETED` | `ResearchData#isCompleted()` | 完成印章，不降低名称可读性 |

`UNCLAIMED_REWARD`、`INFINITE_LEVEL`、`BOOKMARKED` 是附加徽标，不覆盖主状态。颜色仍沿用羊皮纸、木、铁、暗红与绿色语义；不能只靠颜色区分完成、锁定和错误。

## Graph Projection And Layout

### 客户端快照

```text
ResearchGraphSnapshot
  Map<String, ResearchGraphNode> nodes
  List<ResearchGraphEdge> edges
  long localRevision

ResearchGraphNode
  id, category, parentIds, childIds
```

快照只读取 `FHResearch.researches` 与现有 parents/children，不修改研究定义。`ResearchGraphProjection` 结合现有客户端研究数据计算可见性与节点外观；研究进度变化只更新状态，不重算布局。

### 确定性分层布局

1. 校验父 ID，缺失引用只记录诊断并跳过该边；
2. 用 Tarjan SCC 折叠环，异常数据不能让布局无限循环；
3. 在缩合 DAG 上按最长路径确定从左到右 rank；
4. 按分类与稳定 research ID 生成初始 lane 顺序；
5. 做固定次数的 barycentric sweep 降低交叉；
6. 放置节点并打包不连通分量；
7. 计算 world bounds 供 `fitAll()`、裁剪和定位使用。

相同定义必须产生相同坐标，不能依赖 `HashSet` 或 registry 遍历顺序。自动布局是无配置默认值；可选 `display.layout` 只承载作者展示坐标，不改变任何研究字段语义。

### 自动布局与手动锚点混排

保留原方案的节点级混合布局，不要求整棵树只能“全自动”或“全手动”：

```json
"display": {
  "layout": {
    "mode": "manual",
    "x": 420,
    "y": 136
  }
}
```

- 默认 `mode=auto`，旧定义缺少 `display.layout` 时完全走自动布局；
- `manual` 的 `x/y` 是科技树世界坐标，不是屏幕像素，不随分辨率和 GUI scale 改变；
- 手动节点作为固定锚点参与自动布局；相邻自动节点围绕锚点排布，不能覆盖锚点；
- 自动节点与手动节点碰撞时移动自动节点；两个手动锚点冲突时保留作者坐标并产生可见诊断；
- 过滤、搜索、项目档案开关和 camera 变化都不改节点坐标；
- 作者编辑模式允许拖动节点、切换 auto/manual、恢复自动并显示重叠/环/缺失父诊断；普通玩家拖节点仍然只平移画布；
- `display.layout` 只进入定义同步和编辑器，不被 `ResearchData`、clue 或完成判定读取。

定义变更后布局流程：

```text
Research definitions
  -> graph validation / SCC
  -> place manual anchors
  -> rank/lane auto layout
  -> avoid manual bounds
  -> stable world coordinates
  -> projection/filter only changes visibility
```

## Technical Architecture

### 页面结构

```text
ResearchBrowserScreen                    read-only wrapper
  ResearchArchiveLayer                  shared archive UI

DrawDeskScreen
  ResearchRootModeBar                   [研究档案] [绘图台]
  ResearchArchiveLayer                  same shared implementation, mounted while hidden
    ResearchTopBar
      ResearchTypeFilter                fixed top-left
      Search / StatusFilter / Bookmark
    ResearchGraphViewport               always mounted in archive
    ResearchTypeListPanel               fixed bottom-left / responsive drawer
    ResearchCanvasControls              fixed bottom-right
    ResearchProjectSummaryPanel         concise always-visible right summary
    ResearchProjectWorkspace            optional centered modal project file
      ResearchProjectHeader
      ResearchClueSummary               current todo summary
      ResearchProjectTabs
      ResearchDetailPage
        ResearchRequirementSection
        ResearchClueListPanel
        ResearchEffectSection
        ResearchActionSection           existing START/PAUSE/COMMIT_ITEM/CLAIM
      ResearchTheoryPage
        TheoryClueList
        DrawingDeskTheoryTransfer
      ResearchExperimentPage
        ExperimentClueList
        ExistingClueTransferButtons
  DrawDeskLayer                         current slots/game/help; mounted while hidden
    ResearchProgressPanel
    EXAMINE/PAPER/INK slots
    MainGamePanel
    Player inventory
```

不把绘图台槽位、玩家背包或 `MainGamePanel` 复制进研究项目档案。clue 中转切回现有 `DrawDeskLayer`，实际操作仍由原控件和原 packet 负责。两个顶层 layer 始终保留实例，只切换输入与可见性，避免往返时丢失状态。

### 工作区状态

```text
ResearchOpenContext
  mode: BROWSE | DRAWING_DESK
  initialResearchId
  initialProjectTab
  initialClueNonce

ResearchWorkspaceState (client-only)
  surface: RESEARCH_ARCHIVE | DRAWING_DESK
  researchTypeFilter
  search/statusFilters
  selectedResearchId
  selectedClueNonce
  projectWorkspaceOpen
  projectTab: DETAIL | THEORY | EXPERIMENT
  bookmarkedResearchIds
  typeListExpanded
  typeListScrollByType
  cameraByResearchType
  drawDeskFocusTarget
```

`ClientResearchUiSessionStore` 按当前 world/team 保存最后类型、节点、页签、本地关注、列表展开和每类型 camera。研究定义 reload 时逐个校验 ID，按“原节点 -> 当前研究 -> 第一个可见节点 -> fit all”回退。该状态不写入团队存档，也不参与研究完成。

### 纯客户端控制器

```java
enum DrawDeskFocusTarget {
    NONE,
    ITEM_EXAMINE,
    THEORY_GAME
}

interface ResearchNavigationController {
    void openResearch(String researchId);
    void openClue(String researchId, String clueNonce);
    void goToDrawingDesk(DrawDeskFocusTarget target);
    void returnToWorld();
    boolean back();
}
```

`goToDrawingDesk` 只调用 screen/layer 的可见性、焦点和一次性高亮方法。该接口不得依赖 `FRNetwork`，用结构约束保证中转层不能误发业务 packet。

### Clue 展示适配

```java
record ResearchClueView(
    String nonce,
    Component title,
    @Nullable Component description,
    @Nullable Component hint,
    boolean required,
    boolean completed,
    float contribution,
    ClueDestination destination
) {}
```

`ResearchClueViewFactory` 只读取 `Clue` 与 `ResearchData`。它不缓存可写状态，不调用 `start/end`，也不改 `ClueData`。收到 clue 同步后重建当前研究的 view rows 即可。

### 专用画布

`ResearchGraphViewport extends PanZoomViewport` 复用 Chorda CUI 的二维 camera、空白/中键拖动、鼠标锚点缩放、坐标换算、矩形裁剪、可见性测试和 fit/center API。研究侧只保留图投影与布局、虚拟节点批量绘制、节点命中测试、tooltip、状态着色及每研究领域 camera 持久化。fit/focus 使用普通 CUI `Button`，大量研究节点仍是虚拟内容，避免每帧遍历完整子控件树。

### 增量刷新

把空 marker `ResearchGui` 改为仅面向展示的刷新契约：

```java
interface ResearchGui {
    void onResearchDefinitionsChanged();
    void onResearchProgressChanged(String researchId);
    void onActiveResearchChanged(@Nullable String researchId);
    void onClueProgressChanged(String researchId, String clueNonce);
}
```

- definitions changed：重建快照、投影和布局，并恢复可验证的 selection/camera；
- research progress：更新对应节点、详情进度和按钮可见性；
- active research：更新旧/新当前节点与绘图台摘要；
- clue progress：只更新对应 `ResearchClueRow`、项目阻塞状态和总进度；
- 不以同步为由销毁 `ResearchWorkspaceState` 或重建 `DrawDeskLayer`。

### 现有业务按钮复用

研究详情中的实际操作区继续调用当前代码路径：

| 操作 | 保留调用 |
|---|---|
| 提交材料并开始 | `FHResearchControlPacket(Operator.COMMIT_ITEM, research)` |
| 开始研究 | `FHResearchControlPacket(Operator.START, research)` |
| 暂停研究 | `FHResearchControlPacket(Operator.PAUSE, research)` |
| 领取奖励 | `FHEffectTriggerPacket(research)` |
| 绘图台提交物品 | 现有 `FHDrawingDeskOperationPacket(blockPos, 3)` 按钮 |

本方案不新增 action packet，也不把中转按钮接到这些调用上。若现有服务端校验需要加固，应单列安全修复，不与 GUI 重做混合。

## Visual Direction

- 继续使用 `escritoire.png`、`DrawDeskTheme` 的羊皮纸、木框、金属结构和暗红标签；
- 绘图台工作页保留现有美术、`387 x 203` 工具布局和槽位位置；现代化集中在档案主树、右侧简报、居中项目档案和双模式导航；
- 用可伸缩 nine-slice 框、分隔线和索引标签适配不同尺寸；
- clue 清单采用稳定的复选标记、简短动作文字和明确中转图标；
- 完成态使用盖章，进行态使用机械刻度/进度环，未知态使用问号；
- 中转按钮使用方向/进入图标并提供 tooltip，不把它画成“完成”或“提交”按钮；
- 动效仅用于节点定位、抽屉开关、中转目标一次性高亮和完成盖章，约 `120-180ms` 且可打断。

这保持 `design/creative-principles.md` 的沉浸、好奇心和解决问题成就感，也遵守 `design/mod-and-assets.md` 的复古未来与像素资产方向；现代化来自信息层级与交互，而不是替换世界观美术。

## Implementation Steps

### Phase 1: 锁定现有行为与客户端模型

1. 为当前 `ResearchData#getTotalCommitted`、`canComplete` 和 clue 排序/完成展示补回归测试或测试夹具；
2. 新增完整 `ResearchWorkspaceState`、每类型 camera/list scroll 和来源感知导航控制器；
3. 新增 `ResearchClueViewFactory`、`CluePresentationClassifier`、`ClueDestinationResolver`；
4. 新增 graph snapshot、projection、自动/手动混合 layout 与纯函数测试；
5. 保留旧 GUI 作为开发 fallback，首阶段不改 packet 和研究存档。

### Phase 2: 完整科技树主界面

1. 实现始终挂载的 `ResearchArchiveLayer`、完整 `ResearchGraphViewport` 和画布安全区；
2. 实现左上 `ResearchTypeFilter`、搜索、状态筛选、本地关注和每类型 camera；
3. 实现左下 `ResearchTypeListPanel`、跨类型未领取奖励区及窄屏 `55%` 高抽屉；
4. 实现拖拽、缩放、fit、focus、键盘导航和列表模式；
5. 保证过滤/搜索/列表展开只改 projection，不重算节点位置；
6. 增加复用同一 archive layer 的只读 `ResearchBrowserScreen` 与初始深链；
7. 收到同步时保留 selection、camera、zoom、过滤和列表滚动。

### Phase 3: 项目简报、居中档案与三页签

1. 实现右侧简洁 `ResearchProjectSummaryPanel` 与固定上限、带安全边距的居中 `ResearchProjectWorkspace`；
2. 实现固定项目标题、clue 摘要、`DETAIL/THEORY/EXPERIMENT` 页签和独立正文滚动；
3. 迁移现有名称、描述、前置、IOPS、requiredItems、effects 和 START/PAUSE/COMMIT_ITEM/CLAIM 控件；
4. 用 `ResearchClueListPanel/ResearchClueRow` 替代现有 `CluePanel` 布局；
5. 直接使用现有 clue 名称、说明、hint、required、contribution 和完成状态；
6. 实现客户端 clue 展示分类，页签切换不发送 packet；
7. 实现列表选择只更新简报、树节点点击打开档案；关闭档案不调整或重置 camera。

### Phase 4: 绘图台双模式与 clue 中转

1. 在 `DrawDeskScreen` 实现 `[研究档案] [绘图台]` 顶层模式，两个 layer 实例始终保留；
2. 实现 `ItemClue/MinigameClue/world clue/CustomClue` 的纯客户端 destination resolver；
3. 增加 `goToDrawingDesk(focusTarget)`，复用现有 `DrawDeskLayer`；
4. 为 `EXAMINE_SLOT`/提交按钮和 `MainGamePanel` 增加一次性定位高亮，不自动点击；
5. 实现从绘图台返回原研究、原 clue、原页签和原树视角；
6. 统一“项目 -> 树 -> 绘图台 -> 关闭”的 `Esc`、鼠标侧键、Backspace 和返回按钮行为。

### Phase 5: 手动锚点、替换与收尾

1. 给 `Research.CODEC` 增加只读于 GUI 的可选 `display.layout.mode/x/y`，旧 JSON 默认 auto；
2. 实现作者布局编辑模式、拖动固定锚点、“恢复自动”和布局诊断；
3. 用真实研究数据检查自动/手动混排、跨类型边、环、缺失父和手动冲突；
4. 用新工作区替换 `ResearchHierarchyPanel`、旧列表和固定模态详情；
5. 删除旧 GUI 类之前确认没有编辑器或其他 screen 引用；
6. 完成语言键、资源、living docs 和开发 diary；
7. 旧 clue/研究存档、业务 packet 和 hook 保持不变。

## Validation

### 自动测试

- 相同 registry 内容以不同遍历顺序输入，布局坐标完全一致；
- 多父、跨分类、孤立分量、缺失父、自环和多节点环不会卡死；
- 自动节点在相同定义/手动锚点下坐标稳定，手动节点不被自动布局移动；
- 自动节点避让手动锚点；两个手动锚点冲突时产生稳定诊断而不静默改作者坐标；
- `display.layout` codec 和定义同步 round-trip 不丢 mode/x/y，旧 JSON 默认 auto；
- `hidden` 不进入画布、搜索、列表、tooltip 或统计；
- 现有分类 namespace alias 被归一为同一过滤项，不产生重复类型或伪跨类型边；
- 类型过滤保留必要跨类型前置上下文，且不会改变任何节点坐标；
- 每个研究类型独立保存 camera/zoom 和列表滚动；definition reload 后失效类型回退“全部”；
- 左下列表包含当前类型全部可知研究，奖励区跨类型汇总，两个入口复用现有领奖动作；
- 项目档案开关、三页签切换和选择更新不销毁树 viewport；
- 右侧简报保持简洁；居中档案不超过固定上限，并在窄屏按安全边距收缩；
- `BROWSE` wrapper 不创建实际研究按钮，不会因 clue 中转发送绘图台业务 packet；
- clue view 保持原 nonce、定义顺序、required、contribution 和完成状态；
- 待做清单排序仅改变 view 顺序，不修改 `Research#getClues()`；
- `ItemClue`、`MinigameClue`、world clue、未知 `CustomClue` 得到正确 destination；
- 已完成 clue 不显示中转操作；
- 非当前研究的 `MinigameClue` 不会错误跳入并结算当前研究小游戏；
- 中转动作只改变 screen route/focus，不调用 `FRNetwork#sendToServer`；
- 单独切换 `DETAIL/THEORY/EXPERIMENT` 不调用 `FRNetwork#sendToServer`；
- `[研究档案] [绘图台]` 往返保留两侧状态，返回顺序符合“项目 -> 树 -> 绘图台 -> 关闭”；
- clue 同步只刷新对应行，不重置 camera、selected research 或列表滚动；
- 现有实际按钮仍发送与改造前相同的 packet 类型与参数；
- `ResearchData#getTotalCommitted` 与 `canComplete` 在相同输入下结果不变；
- 旧 research JSON 与旧团队存档不经过迁移即可加载。

### 手工 QA

- 覆盖 `320x240`、`427x240`、`640x360`、`960x540` 与多档 GUI scale；
- 左上类型过滤器与左下列表在所有分辨率可操作，展开列表不遮挡顶部导航或缩放控件；
- 宽屏和窄屏右侧简报都不挤掉画布的基本操作区域，且没有文字/按钮重叠；
- 项目档案打开时底层画布被遮罩阻止误操作；关闭后 camera、zoom 和选择不跳变；
- `DETAIL/THEORY/EXPERIMENT` 只改变展示；切换页签不进入绘图台、不开始小游戏；
- `[研究档案] [绘图台]` 顶层模式清楚反映当前位置，两侧往返不重建槽位或树；
- 普通只读入口与绘图台入口显示同一树坐标和项目布局，但普通入口没有研究 mutation 控件；
- 切换研究类型恢复各自 camera/zoom；搜索、过滤和本地关注不引起节点跳位；
- 普通玩家拖动节点只平移画布；作者编辑模式才能修改手动锚点；
- 超长中英文 clue 名称换行但不挤压状态标记和中转按钮；
- 空白拖拽、从节点拖拽、中键拖拽和快速缩放不误开详情；
- 单击节点打开详情，关闭后树不重建、不跳 camera；
- `ItemClue` 中转后只高亮物品槽与现有提交按钮，物品没有自动移动或消耗；
- `MinigameClue` 中转后只定位现有小游戏，未自动开始、恢复或结算；
- world clue 的“返回世界”只关闭 GUI，随后仍由击杀/成就/tick/custom 事件完成；
- 完成 clue 后列表勾选、required 阻塞提示和总进度按现有同步更新；
- 开始、暂停、提交材料、提交研究物品和领奖的实际结果与旧 GUI 一致；
- 详情 -> clue 中转 -> 绘图台 -> 科技树能回到原项目、原 clue 和原树视角；
- 定义 reload、当前研究变化、断线和窗口 resize 均安全回退。

### 性能门槛

- `250` 节点、`500` 边时，定义更新后的单次布局目标低于约 `25 ms`；
- 静止画布不产生每帧集合分配；
- 只渲染 viewport 扩展边界内的节点和相关边；
- 平移缩放目标 `60 FPS`；
- 研究/clue 进度包不触发布局重算。

## Failure Modes

| 场景 | 处理 |
|---|---|
| 中转时绘图台 layer 已失效 | 留在详情并显示无法前往，不发送 packet |
| 非当前研究的理论 clue 被点击 | 提示先使用现有开始研究操作，不进入错误小游戏 |
| `CustomClue` 没有已知目标 | 只显示说明，不猜测或自动完成 |
| clue 同步时所选 nonce 已不存在 | 清除 clue 选择，保留 research 详情 |
| definition reload 删除所选 research | 回退当前研究或首个可见研究并 `fitAll()` |
| 长文本或窄屏挤压按钮 | 主行换行，按钮固定宽度，说明移入展开区 |
| 玩家连续点击中转 | route 操作幂等，只产生一次页面切换和高亮 |

## Acceptance Criteria

1. 玩家能在完整科技树中拖动、缩放、搜索、过滤并定位研究；
2. 任意可见研究一次点击即可打开详情；
3. 详情把现有 `Clue` 明确表现为玩家待做清单，名称简短、完成与必需状态清楚；
4. clue 中转按钮只负责前往现有操作位置或返回世界，本身不提交、不计点、不触发完成；
5. 提交物品、理论小游戏、世界事件、完成判定和领奖与改造前行为一致；
6. 左上类型过滤器和左下当前类型列表/跨类型奖励区始终可访问，且不改变布局坐标；
7. 右侧简报保持简洁，居中项目档案在宽屏/窄屏遵守固定上限与安全边距，三页签只重组现有信息；
8. `[研究档案] [绘图台]` 双模式和来源感知返回顺序稳定，两侧状态都被保留；
9. 自动布局可独立工作，作者也能对单个节点设置手动锚点并与自动节点混排；
10. 新 GUI 不新增 task/provider/project 存档，不修改 clue 或研究进度 codec；`display.layout` 仅影响绘制坐标；
11. 在研究详情与绘图台之间往返不丢项目、页签、选中 clue、camera、zoom、过滤和滚动位置；
12. `hidden` 研究不会通过图、搜索、列表、奖励或 clue 路由泄漏；
13. 小屏、GUI scale 和长文本下没有遮挡或不可点击控件；
14. 旧研究定义与旧存档无需迁移即可使用；
15. 普通只读入口和绘图台入口复用同一档案 UI，普通入口不暴露现有实际研究按钮；
16. 美术仍明显属于《冬季救援》的研究档案和复古工业系统。

## Documentation Impact

实现时新增 `docs/research/README.md`，记录当前研究真相、clue 完成逻辑与 GUI 边界；新增 `docs/research/research-ui.md`，记录完整树、待做清单、中转规则、导航状态和刷新契约；更新 `docs/README.md` 增加 Frosted Research 入口。

这份计划只记录 GUI 意图，不把 provider、城镇研究或新任务模型描述成已实现行为。完成开发后在 `diary/` 记录实际替换的类、验证命令和仍保留的 fallback。

## Confirmed Constraints

- 当前 `Clue` 就是待做清单项；
- clue 的完成与研究完成逻辑不变；
- 物品提交、理论研究和实验完成继续走现有路径；
- clue 按钮只做页面中转，不直接执行研究动作；
- 首版只改 GUI 和客户端导航；
- 不引入 provider/task/城镇研究等扩展架构。

## Outcome

- `2026-08-21`: Phase 1 completed. Added client-only workspace/navigation state, clue presentation and destination adapters, a stable full-graph snapshot/category projection, deterministic SCC-aware layout with manual-anchor support, and focused regression tests.
- `ResearchGui` distinguishes definition, research-progress, active-research, and clue-progress refreshes. `DrawDeskScreen` now forwards those notifications incrementally; the old `ResearchLayer` remains only as an unmounted source fallback.
- `2026-08-21`: The drawing-desk player surface now mounts `ResearchArchiveLayer`, the full pannable/zoomable graph, type/project index, search, bookmarks, fit/focus controls, a concise right summary, and a centered three-tab project dialog. The old `ResearchLayer` remains in source but is no longer mounted by `DrawDeskScreen`.
- Drawing-desk/archive routing now preserves both layer instances, consumes `Esc`/mouse-back in project -> archive -> desk order, and provides one-shot item/theory focus frames. Clue routes remain client-only; actual research actions reuse existing packets.
- `2026-08-21`: Player feedback revision completed. Research fields moved to original-style top icon tabs, search moved above the project index, normal mode now filters undiscovered definitions before projection, FTB-injected widgets are hidden in archive mode, and the dialog returned to the original `302 x 170` texture and render depth. Materials and all real clues are shown in `DETAIL`; `THEORY` retains a dedicated theoretical-clue view, and `EXPERIMENT` is an empty future integration boundary.
- `2026-08-21`: Follow-up feedback revision completed. Research fields now share the exact top header with the archive title and drawing-desk action. Graph zoom reaches `15%`, node geometry scales without the former `54 x 24` floor, and fit-all centers the projected tree at `15%`. Icons and names are never skipped at low zoom; they retain `4px` and `0.25` scale minimums. FTB sidebar groups are suspended at their manager data source because `SidebarGroupGuiButton` bypasses native widget visibility, then restored on desk return or close.
- `2026-08-21`: The project `DETAIL` checklist now includes theoretical `MinigameClue` rows as well as other real clues. `THEORY` remains a dedicated filtered view of those same theoretical clues, while `EXPERIMENT` remains empty for future town integration.
- `2026-08-21`: Archive hot paths now cache project filtering/sorting, graph search matches, clue presentation, wrapped descriptions, and detail content metrics. Graph shape primitives are batched through CUI, orthogonal edges are viewport-clipped, fit/focus use CUI buttons, and unchanged drawing-desk ticks no longer relayout the archive.
- `2026-08-21`: Map-style camera/input/clipping behavior was extracted into Chorda CUI `PanZoomViewport`. `ResearchGraphViewport` now consumes the shared viewport while retaining only research graph layout, projection, virtual nodes, hit testing, and presentation; future FTB-style maps can reuse the same pan/zoom foundation and override camera constraints for bounded worlds.
- Remaining planned work: standalone read-only `BROWSE` entry, explicit status/reward filters, keyboard graph navigation, optional `display.layout` codec/editor integration, in-game JFR/Spark performance measurement, and visual QA across GUI scales.
