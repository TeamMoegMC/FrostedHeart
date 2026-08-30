# 暖石任务 AI 上下文与公用交接

- Time: `2026-08-26 23:21:11 +08:00`
- Author: `Codex; OpenAI GPT-5; planning and handoff role`
- Related: [`../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md`](../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md), [`../docs/climate/player-temperature.md`](../docs/climate/player-temperature.md), [`../docs/climate/world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md), [`../docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), `TemperatureUpdate`, `PlayerTemperatureData`, `RadiationService`, `MinecraftThermalInput`

> 本文是非权威的协作与交接板。目标行为、任务依赖和完成标准以关联 plan 为准；当前实现以源码和 living docs 为准。不要在本文重新决定热容比、NBT schema、Curios 槽或 thermal runtime 合同。发生冲突时先修正 plan，再追加新的交接记录。

## 1. 当前任务基线

- Plan 状态：`ready`；`T00-T28` 全部未开始。
- 首版物品：`frostedheart:warm_stone` 与 `frostedheart:hot_water_bag`。
- 相对玩家热容：暖石 `0.10`，热水袋 `0.25`。
- 两者共用容量为 `1` 的 `warm_stone` Curios 槽。
- 掉落实体环境导热率默认是库存状态的 `16` 倍，调校下限为 `10` 倍。
- 通用充热入口是把掉落物放在 Campfire 等已注册物理热源旁；首版没有 charger recipe。
- 创建本文时，Frosted Heart `git status --short` 共 `32` 条，尚未逐项判定归属；plan 本身为 untracked。执行 `T00` 的 owner 必须先重新捕获完整状态并保护无关用户改动。
- 当前扫描未在 `TheWinterRescue` 仓库中找到 `AGENTS.md`；`T20` 开始时必须重新定位，不能把本次扫描当作永久事实。

## 2. 三种上下文的操作定义

| 名称 | 本文含义 | 适用条件 | 不适用条件 |
|---|---|---|---|
| 新主对话 | 从空对话开始，由交接文档、plan、源码和测试结果重建显式上下文 | 前一 Gate 已关闭；下一阶段关注点或仓库明显变化；需要清理过长历史 | 前置合同仍在变化；当前阶段还有未合并子任务 |
| 分支对话 | 从指定 Gate 检查点分出可持续多轮的工作流，继承当时的对话背景 | 任务需要多轮实现/验证，但文件所有权与主线清晰分离 | 与另一分支同时修改共享 runtime 或同一核心类 |
| 子 agent | 由当前 owner 派发的有界任务；完成后把结果交回 parent，由 parent 决定是否接受 | 输入、允许文件、产出和测试都能一次写清；不负责 Gate 决策 | 需要改变合同、跨多个未冻结模块、需要持续与用户讨论 |

对话分支不是 Git 分支。所有 agent 若共享同一个工作目录，仍会立即看见彼此文件修改；并行工作必须按文件所有权隔离。若改用独立 Git worktree，则交接记录还要写明 worktree 路径、base commit 和合并顺序。

本文的“分支对话”是逻辑协作边界，不假设当前客户端一定提供专用 branch 按钮。没有该入口时，就从对应 Gate 检查点开一个新对话，使用第 7 节模板并把 parent 对话/checkpoint 写进首条交接记录，效果等同于本文所称分支。

[OpenAI 官方模型指南](https://developers.openai.com/api/docs/guides/latest-model)只建议对“可清晰分成独立 workstream”的复杂任务使用 multi-agent，并要求定义清楚 handoff、输出证据与停止条件。因此本任务只对下列有界节点使用子 agent，不按 `T00-T28` 一项一个 agent 拆分。

## 3. 精确的对话、分支与子 agent 路线

### 3.1 实现启动：新主对话 `C0-contract-player`

**开启时间：** 用户正式要求开始实现时，在 `T00` 前开启新主对话。

**Owner：** `gpt-5.6-sol high`，负责 Phase 0、Gate A、Phase 1 集成和 Gate B。即使部分任务由 Terra/Luna 执行，合同接受权仍在 `C0`。

执行顺序：

1. `C0` 完成 `T00-T01`。
2. `T01` 冻结后，同时开两个子 agent：
   - `A02-two-reservoir`：只做 `T02` 及对应纯 Java 测试，推荐 `gpt-5.6-terra high`。
   - `A03-environment-exchange`：只做 `T03` 及对应纯 Java 测试，推荐 `gpt-5.6-terra high`。
3. 两个子 agent 返回后，`C0` 审查 diff、运行两组测试并完成 `T04`。
4. `C0` 关闭 Gate A，更新 plan checkbox 和本文 Gate A 记录。

子 agent 文件所有权：`A02` 与 `A03` 必须分别创建独立模型类和测试类；不得修改 plan、discussion、`TemperatureUpdate`、`PlayerTemperatureData` 或 thermal runtime。

### 3.2 Gate A 后：分支对话 `B1-item-curios`

**开启时间：** Gate A 关闭并形成可复现参数曲线后，从 `C0` 的 Gate A 检查点开启分支对话。

**Owner：** `gpt-5.6-terra high`，负责 `T05-T09` 的物品状态、profile、注册、Curios 和资源闭环。

执行顺序：

1. `B1` 串行完成 `T05-T08`；`T05-T06` 不能拆开，因为 NBT 状态与 item profile 共同定义 ItemStack 合同。
2. `T07-T08` 验收后，可开子 agent `A09-resources` 完成 `T09`，推荐 `gpt-5.6-terra medium`。
3. `A09` 只修改 tag/datagen、lang、model、texture、slot icon 和 tooltip 直接资源；若 tooltip 需要生产 Java 改动，先报告 `B1`，由 `B1` 修改。
4. `B1` 完成定向资源检查后，将交接结果返回 `C0`。

与 `B1` 并行，`C0` 可在 Gate A 后开启子 agent：

- `A10-player-api`：完成 `T10`，只修改 `PlayerTemperatureData` 及其定向测试，推荐 `gpt-5.6-terra high`。

`B1` 与 `A10` 都被 `C0` 接受后：

1. `C0` 自己完成 `T11`，因为它负责把物品状态、Curios helper 和玩家核心 API 接入同一更新顺序。
2. `T11` 开始后可开子 agent `A12-inventory` 完成 `T12`，推荐 `gpt-5.6-terra high`；它不得修改 `TemperatureUpdate`、Curios 或 world thermal runtime。
3. `C0` 接受 `A12`，运行 Phase 1 定向测试并关闭 Gate B。

**不要在 Phase 0 与 Phase 1 之间开完全空白的新主对话。** 这一边界仍共享 profile、NBT 初始化和玩家核心更新语义；用 Gate A 分支比清空上下文更合适。

### 3.3 Gate B 后：新主对话 `C2-world-runtime`

**开启时间：** Gate B 已关闭，`T05-T12` 已被接受且空槽玩家基线通过后。

**Owner：** `gpt-5.6-sol xhigh`，负责 `T13-T19` 和 Gate C 的 Frosted Heart 侧 runtime 证据。

启动时必须重新读取：AGENTS、两份 memory、climate docs、plan、本文最新 Gate A/B 记录，以及 `T02/T03/T11/T12` 的最终 diff。先运行 Phase 1 定向测试，确认接手基线。

执行顺序：

1. `C2` 串行完成 `T13 -> T14 -> T15 -> T16`。
2. `T13-T16` 不开分支对话、不交给互相隔离的 agent；四项共享 `RadiationService`、`MinecraftThermalInput`、receiver cache/budget 和 `ItemEntity` 时间语义。
3. `T16` 编译且定向测试稳定后，最多并行开启三个子 agent：
   - `A17-runtime-unit-tests`：`T17`，只拥有 `RadiationServiceTest`、`MinecraftThermalInputTest` 和新建的 item-query 单测文件，推荐 `gpt-5.6-terra high`。
   - `A18-campfire-gametest`：`T18`，只拥有 Forge GameTest Java、template/catalog 和必要 GameTest 资源，推荐 `gpt-5.6-sol xhigh`。
   - `A19-workload-regression`：`T19`，新建独立 workload/cadence 测试，不修改 `A17` 的测试文件，推荐 `gpt-5.6-sol high`。
4. 子 agent 若发现生产实现缺陷，只提交复现证据和建议；生产修复统一由 `C2` 完成，避免三个测试 agent 同时改 runtime。
5. `C2` 合并测试、运行 JUnit/GameTest、关闭 Gate C 的 Frosted Heart runtime 部分并追加交接记录。

### 3.4 Gate C runtime 完成后：一个新主对话加一个分支对话

#### 新主对话 `C3-pack-content`

**开启时间：** `T18` 已证明两种物品可以通过通用 Campfire 路径加热，registry ID、NBT schema 和 tooltip 文案语义已冻结。

**Owner：** `gpt-5.6-terra high`，只负责 TheWinterRescue 的 `T20-T22`。

`C3` 必须先完成 `T20`，重新定位 companion `AGENTS.md`、扫描两个仓库关联 ID 并记录两边 Git 状态；之后串行完成 `T21-T22`。不要为 recipe、研究和任务分别开 agent，因为它们共享进度顺序、registry ID 与用户文案。`C3` 不修改 Frosted Heart production Java。

#### 分支对话 `B23-test-tools`

**开启时间：** 与 `C3` 同时，从 `C2` 的 Gate C runtime 检查点分出。

**Owner：** `gpt-5.6-terra medium`，只负责 Frosted Heart 的 `T23` 调试 Stack 和手工观测工具。它需要继承最终 item thermal API，所以应从 `C2` 分支，不应从空白对话开始。

`C3` 与 `B23` 不直接同时修改本文或 plan。`B23` 把结果交给 `C3` 的 Phase 3 coordinator，由 coordinator 统一追加一次 Phase 3 完整记录并勾选 `T20-T23`。

### 3.5 `T20-T23` 全部接受后：新主对话 `C4-final-integration`

**开启时间：** Frosted Heart runtime、配套仓库内容和测试工具都已有完整交接记录后。

**Owner：** `gpt-5.6-sol high`，负责 `T24-T28`、Gate D 和最终完成判断。

执行顺序：

1. `C4` 先读取两个仓库 AGENTS/状态、plan、本文全部 Gate 记录和所有相关 diff。
2. `C4` 串行完成 `T24-T25`；自动化失败必须先修复再做参数冻结。
3. `T25` 冻结最终公式参数、默认值和限制后，开启子 agent `A26-living-docs` 完成 `T26`，推荐 `gpt-5.6-terra high`。它只修改 `docs/climate/` 中计划列明的 living docs 和索引。
4. `C4` 审查文档与源码一致性，自己完成 `T27`。两个仓库的构建/脚本验证不要并发争用同一 Gradle/cache；按仓库分别记录。
5. `C4` 完成 `T28`，更新 plan Outcome、追加 diary，并在 Gate D 记录最终 commit/diff、测试和剩余限制。

`T28` 不交给独立新对话：计划完成判断必须由看到 `T24-T27` 全部证据的 `C4` owner 作出。

## 4. 一眼可执行的切换表

| 到达节点 | 操作 | 名称 | Owns |
|---|---|---|---|
| 开始 `T00` | 开新主对话 | `C0-contract-player` | `T00-T04`, Gate A, `T11`, Gate B |
| `T01` 冻结 | 开两个子 agent | `A02`, `A03` | `T02`, `T03` |
| Gate A 关闭 | 开分支对话 | `B1-item-curios` | `T05-T09` |
| Gate A 关闭 | 开子 agent | `A10-player-api` | `T10` |
| `T11` 开始 | 开子 agent | `A12-inventory` | `T12` |
| Gate B 关闭 | 开新主对话 | `C2-world-runtime` | `T13-T16`, runtime integration |
| `T16` 稳定 | 开三个有界子 agent | `A17`, `A18`, `A19` | `T17`, `T18`, `T19` |
| Gate C runtime 完成 | 开新主对话 | `C3-pack-content` | `T20-T22` |
| Gate C runtime 完成 | 从 `C2` 开分支对话 | `B23-test-tools` | `T23` |
| `T20-T23` 全接受 | 开新主对话 | `C4-final-integration` | `T24-T28`, Gate D |
| `T25` 参数冻结 | 开子 agent | `A26-living-docs` | `T26` |

## 5. 文件所有权和并发规则

1. 同一时刻只有当前 Gate coordinator 可以修改 plan、本文和 diary；子 agent 不修改这些文件，只向 parent 回报结构化结果。
2. 同一生产 Java 文件不能由两个并行工作流修改。确需共同修改时，后一任务等待前一任务被 parent 接受。
3. `T13-T16` 的 production runtime 只归 `C2`；`A17-A19` 默认只修改各自测试文件。
4. `design/` 永远只读。
5. Companion 脚本、recipes、quests、pack config 只归 `C3`；`B23` 不进入 companion 仓库。
6. 发现无关工作树变化时保留并避开；无法绕开才暂停交接并向用户说明具体冲突文件。
7. 新对话不能仅凭“前一阶段完成”开始工作，必须能指出对应 Gate 记录、相关 diff/commit 和已通过测试。

## 6. 标准交接流程

### 6.1 接手前

1. 阅读当前仓库 `AGENTS.md`、`.Codex/memory/project-structure.md`、`.Codex/memory/architecture.md`。
2. 从 `docs/README.md` 进入相关 living docs；不要把 `docs/deprecated/`、discussion 或 plan 当成当前实现事实。
3. 阅读完整暖石 plan 和本文最新追加记录。
4. 运行 `git status --short`；记录 base commit、已有相关修改、无关用户修改和 untracked 文件。
5. 核对所接任务的全部 Depends 已由 Gate coordinator 标记完成。
6. 声明任务 ID、允许文件、禁止文件、推荐模型、预期测试和停止条件后再编辑。

### 6.2 执行中

1. 只实现已声明任务；发现合同问题时向 parent 返回，不自行扩大任务或重写已冻结参数。
2. 每个行为改动同步增加对应层级测试；生产 bug 由拥有该生产文件的 owner 修复。
3. 记录实际改动路径和新增 searchable anchors，不依赖行号交接。
4. 不覆盖其他 agent 或用户产生的修改；共享目录下开始编辑前重新读取目标文件。
5. 到达阻塞条件时保存复现命令、输出摘要和已排除原因，不只写“测试失败”。

### 6.3 完成后

执行者向 parent 提交以下结构化内容；parent 复核后统一追加到本文：

```text
Time / executor / model:
Task IDs and status: completed | partial | blocked
Base commit or checkpoint:
Files changed:
Behavior and contracts implemented:
Decisions made within existing authority:
Commands/tests and exact result summary:
Git status and relevant diff summary:
Known limitations or failing tests:
Documentation impact:
Tasks now unblocked:
Recommended next owner/action:
```

Parent 必须检查 diff、重跑至少一条关键测试，并确认没有越过文件所有权，才能在 plan 中勾选任务。子 agent 自报完成不等于任务完成。

### 6.4 Gate 关闭

Gate coordinator 追加一条 Gate 记录，至少包含：

- Gate 名称与关闭时间；
- 已接受任务 ID；
- 最终合同/参数及其源码锚点；
- 相关文件清单；
- 测试命令与结果；
- 当前 Git commit，或未提交时的相关 diff 清单；
- 被保留的无关工作树变化；
- 下一对话允许开始的任务和禁止重做的任务。

若没有 commit，必须列出全部相关 modified/untracked 文件。仅写“工作树有改动”不足以支持新对话接手。

## 7. 新主对话启动模板

```text
继续实现暖石类热库计划，负责 <conversation name> / <task IDs>。

开始前完整阅读：
1. 当前仓库 AGENTS.md；
2. .Codex/memory/project-structure.md 与 architecture.md；
3. plans/2026-08-24_19-34-16_player-heat-capacity-curios.md；
4. discussion/2026-08-26_23-21-11_warm-stone-ai-context-handoff.md 的最新 Gate 记录；
5. <relevant living docs>。

先核对 git status、base commit/diff 和依赖 Gate，不重做已完成任务。
允许修改：<paths>。
禁止修改：design/、<other owner paths>。
完成标准：<tests/evidence>。
完成后按交接模板报告，由 Gate coordinator 决定是否勾选任务。
```

## 8. 子 agent 任务模板

```text
只执行暖石计划 <task ID/name>。
前置合同：<frozen inputs>。
允许修改：<exact files or directories>。
禁止修改：plan、discussion、diary、design/、<production files owned by parent>。
必须运行：<focused tests>。
停止条件：发现需要改变冻结合同、修改 parent-owned production 文件或依赖未满足时，返回证据，不自行扩展。
返回：文件清单、行为摘要、测试结果、相关 git diff、限制与下一步。
```

## 9. 追加交接记录

后续 owner 在本节末尾按时间追加，不重写前人的记录。首条待 `T00` 启动时创建。

### 9.1 `2026-08-27 21:42:50 +08:00` 三节点模型与玩家传热率决策

- Executor: `Codex; OpenAI GPT-5; planning and handoff role`
- Task IDs and status: `计划细化完成；T00-T28 仍全部未开始`
- Updated plan: [`../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md`](../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md)

用户已确认以下合同，后续 Gate 和执行者不得把它们当作待调参数：

1. 物品热状态从单一温度改为内部温度与表面温度；佩戴时形成“物品内部 -> 物品表面 -> 玩家核心”三个有限热容节点。
2. 每条相邻边的瞬时传热仍与温差线性相关。表面先冷却、内部随后补热带来的时间曲线自然非线性，不引入任意的温差平方公式。
3. 暖石总热容固定为玩家的 `0.10`，表面到玩家传热率固定为 `1.2e-4 degC_player / (s * degC_difference)`。
4. 热水袋总热容固定为玩家的 `0.25`，表面到玩家传热率固定为 `8e-5 degC_player / (s * degC_difference)`。
5. 上述速率直接表示最终玩家核心温度变化，不乘躯干 `0.5` 权重。表面与玩家相差 `20 degC` 时，两件物品的瞬时玩家侧变化分别为 `0.0024 degC/s` 和 `0.0016 degC/s`。
6. 原计划中共同的 `4 game seconds` 玩家接触换热时间常数已取消。Phase 0 只校准表面热容占比、内部到表面导热率、环境导热率和辐射换算参数，不得改写热容比或两项玩家传热率。
7. 环境与玩家都只直接接触物品表面；内部温度只能通过内部到表面传导变化。NBT、测试曲线和调试工具必须能分别观察和保存两个物品温度。

对既有协作路线的修正：

- 第 3.1 节的子 agent `A02-two-reservoir` 自本记录起改名为 `A02-three-node-wearable`，任务仍为 `T02`，推荐模型提高为 `gpt-5.6-sol high`。
- `A02` 的允许产物改为独立的 `ThreeNodeWearableHeatExchange` 纯模型类及测试；不得实现旧的 `TwoReservoirHeatExchange`。
- `A03-environment-exchange` 必须推进内部/表面两个物品节点，环境只作用表面；它不能继续实现单一 `temperature_c` 的指数松弛模型。
- `T05` 使用 version 1 双温度 schema：`core_temperature_c` 与 `surface_temperature_c`。由于功能尚未实施，不需要为计划阶段的旧单字段草案编写迁移。
- Gate A 的曲线证据必须同时输出玩家、内部、表面温度，并包含“内部热、表面接近玩家温度”的延迟释热场景。

新对话或子 agent 接手时，应把本记录作为第 3 节旧任务名称和旧参数的覆盖说明；其余对话边界、文件所有权和交接流程保持不变。

### 9.2 `2026-08-27 22:34:38 +08:00` 表面热容与内外传热率冻结

- Executor: `Codex; OpenAI GPT-5; planning and handoff role`
- Task IDs and status: `计划参数继续细化；T00-T28 仍全部未开始`
- Updated plan: [`../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md`](../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md)

本记录覆盖 9.1 中“由 Phase 0 校准表面热容占比和内部导热率”的安排。用户已进一步确认：

1. 两件物品的表面热容占比统一固定为 `a=0.20`，不再是自由参数。
2. 暖石总热容 `0.10` 拆为内部 `0.08`、表面 `0.02`；热水袋总热容 `0.25` 拆为内部 `0.20`、表面 `0.05`。
3. profile 的权威内部参数使用单位温差内外传热率 `coreSurfaceTransferRatePerSecond`，单位为 `1/s`。暖石固定 `6.1613e-5 /s`，热水袋固定 `9.2420e-4 /s`。
4. 上述速率分别对应隔绝玩家和环境时内外温差 `180 s`、`30 s` 的半衰期。半衰期是派生说明与构造辅助输入，不在 profile 或 NBT 中重复存储。
5. `T02` 必须实现并注释半衰期到权威速率的换算辅助：`k_cs = r*a*(1-a)*ln(2)/t_half`；同时以反向换算和 round trip 测试固定公式。
6. `T04/T25` 只能验证 `r`、`a`、`k_cs`、`g_sp`，不得重新调节它们；尚待曲线选择的只剩环境 `k_inventory` 和辐射换算参数，`k_dropped/k_inventory` 的默认 `16` 与最低 `10` 合同不变。

后续 `A02-three-node-wearable` 的输入必须包含本记录中的四组固定物品参数。实现若需要改变这些值，应停止任务并返回曲线证据，由用户重新决策；不得在代码审查或实机调参阶段静默覆盖。

### 9.3 `2026-08-27 23:02:32 +08:00` 环境倍率与玩家辐射桥接冻结

- Executor: `Codex; OpenAI GPT-5; planning and handoff role`
- Task IDs and status: `计划参数继续细化；T00-T28 仍全部未开始`
- Updated plan: [`../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md`](../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md)

本记录覆盖 9.2 中“环境 `k_inventory` 和辐射换算参数仍待曲线选择”的安排。用户已确认：

1. 环境导热率绑定到同一物品已冻结的表面到玩家传热率 `g_sp`，不再作为两个独立 profile 参数。
2. 库存倍率固定为 `0.5`：`k_inventory=0.5*g_sp`。掉落倍率固定为库存的 `16` 倍：`k_dropped=16*k_inventory=8*g_sp`。
3. 暖石使用 `k_inventory=6.0e-5 /s`、`k_dropped=9.6e-4 /s`；热水袋使用 `k_inventory=4.0e-5 /s`、`k_dropped=6.4e-4 /s`。
4. 上述倍率作用于相对于归一化玩家热容的单位温差导热率，不能直接乘 `dT_surface/dt`。环境仍只接触表面，实际表面温度变化必须除以 `r_surface`。
5. 隔绝内部传导时，暖石库存/掉落表面温差半衰期为 `231.0 s/14.4 s`，热水袋为 `866.4 s/54.2 s`。这些数值只描述表面到环境单边，不是完整双节点物品的总热量半衰期。
6. 掉落物辐射复用玩家现有的等效辐射温度桥接：`deltaT_radiant=radiantFluxWPerM2*0.8/6.0`，`T_effective_env=T_air+deltaT_radiant`。库存状态令辐射为零。
7. 实现应将玩家当前等效辐射温度公式提取为 climate/radiation 共享纯函数；物品不得依赖 HUD 或玩家事件处理器。玩家身体能量增温公式继续独立，不用于物品环境交换。
8. 不再引入 `itemRadiantAbsorptivity`、`itemRadiantTransferWPerM2K` 或其他物品专属辐射平衡参数。Campfire 的辐射份额、距离衰减与遮挡仍由现有 physical source 和 `RadiationService` 决定，receiver 查询不改变 source ledger。

自本记录起，`T01/T03/T04/T25` 只能验证上述环境倍率与共享辐射桥接，不得再把它们列为待调参数。若真实曲线要求改变倍率或共享换算，执行者必须返回完整玩家/表面/内部、空气与辐射时间序列，由用户重新决策。

### 9.4 `2026-08-27 23:23:54 +08:00` 最终正确性、性能与维护性复核

- Executor: `Codex; OpenAI GPT-5; planning and handoff role`
- Task IDs and status: `计划最终复核完成；T00-T28 仍全部未开始`
- Updated plan: [`../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md`](../plans/2026-08-24_19-34-16_player-heat-capacity-curios.md)

本记录是在用户要求最终 review 后对实施合同的工程性收紧，目标是避免严重正确性/性能问题，并保持首版结构简单。它不改变 9.1-9.3 已冻结的玩法参数；若与本文更早的任务描述冲突，以本记录和更新后的 plan 为准：

1. `T02/T03` 必须复用现有 `ThermalExchangeKernel.exchangePairInto`/`exchangeFixedBoundaryInto`，只增加固定三节点和固定环境边界的薄编排、最大子步与 caller-owned scratch；禁止复制指数核、实现第二套通用 solver 或在稳定 tick 路径持续分配临时集合。
2. 热库状态使用 item 实现的 `WearableThermalReservoir` 窄接口、不可变 profile 和 ItemStack NBT；首版不注册 Forge capability/provider，不制造第二份状态权威。
3. version 1 双温度状态只接受 `[-1000,1000] degC` 内有限值。非法、越界或未知 schema 仅在服务端环境有限时以夹到该范围的环境温度同时重建内部和表面；否则保持未初始化并跳过本轮。警告只按固定原因限频，不按实体/Stack 建无界 key。
4. `T04` 只用合成 `q=0/100 W/m2` 验证纯模型，不要求尚未由 `T13-T16` 实现的真实 Campfire item receiver；真实 Campfire 曲线仍由 `T18/T25` 验收。这消除了 Gate A 对 Gate C 运行时的循环依赖。
5. `T03` 创建共享纯函数 `RadiantEquivalentTemperature`，但不修改玩家事件/HUD；`T04` 负责让 `TemperatureComputation` 委托该 helper，并保持现有 `TemperatureComputationRadiationTest` 的 `100 W/m2 -> 13.333333 degC` 精确回归。
6. `warm_stone` 查询只读取准确 handler 的 slot `0`。handler visibility 决定玩法是否启用；Curios `getRenders()` 是外观开关，关闭渲染不得关闭热交换。
7. 保持当前管理分支语义：`creative`、`spectator` 或其他 `invulnerable` 玩家整轮跳过佩戴热库交换，玩家与物品都不推进；`INSULATION` 仍允许玩家与已佩戴物品交换。
8. 库存与掉落状态使用 source-default `20 game ticks` cadence。库存路径只由实际热库 Stack 的服务端 `inventoryTick` 触发；掉落路径只由热库 Item 自身的精确实体 tick hook 触发。禁止 level tick 枚举全部实体、全局掉落物扫描或逐实体邻域方块扫描。
9. 掉落物局部 sample cache 归 per-level `MinecraftThermalInput` runtime 所有，固定容量、按 tick generation 回收并随 level 关闭清理；不用静态 map/`WeakHashMap`。item receiver 拥有独立 hard cap/cache 预算，现有玩家三点 API 数值与 `128` receiver 容量不得下调。
10. cadence、last-update 和 receiver key 是 transient 运行时状态，不写进热状态 NBT。拾取、重新掉落、跨维度或卸载/重载只重置模式计时，不重置温度，也不做容器/离线墙钟追赶；实体删除后的 runtime 状态必须在有界时限内回收。
11. Gate B 必须用计数证据确认每个相关 Stack 每 cadence 至多一次 NBT 写回，没有每 tick slot、full-inventory 或全玩家广播。先测量现有 Curios/容器同步；只有 tooltip 确实陈旧时才增加按显示精度量化、面向相关 Stack 的限频同步。
12. 9.2 记载的“掉落倍率最低 `10`、默认 `16`”已被 9.3 的最终冻结覆盖：首版严格使用 `16`，不再保留 `10` 的调校范围。

对协作路线和文件所有权的最终修正：

- `A02-three-node-wearable` 只能实现复用现有 kernel 的固定拓扑编排及纯 Java 测试，不得新建通用 solver。
- `A03-environment-exchange` 可创建共享辐射 helper、环境固定拓扑编排及其测试，但不得修改 `TemperatureComputation`；玩家公式委托和既有精确回归归 `C0` 的 `T04`。
- `B1-item-curios` 的 `T05-T08` 必须遵守无 Forge capability、固定状态范围以及 render bit 不参与玩法判断的合同。
- `C0` 的 `T11/Gate B` 负责无敌/`INSULATION` 分支、一次写回与同步 packet 计数的最终接受。
- `C2-world-runtime` 串行拥有 `T13-T16` 的共同 receiver 循环、per-level cache、精确实体 hook、预算分区和 transient 生命周期；不得把 production runtime 拆给相互隔离的 agent。
- `T17/T19` 的测试须覆盖玩家容量不回归、无全实体枚举、缓存/状态有界回收及稳定路径分配上限；`T18/T25` 才验证真实 Campfire 链路。

上述修正不新增任务 ID，也不改变计划 `ready` 状态。所有需要用户决定的玩法参数现已冻结；item receiver hard cap、同步量化精度等数值属于必须由 `T00/T13/Gate B` 以现有实现与计数证据确定的工程阈值，不作为新的平衡决策。

### 9.5 `2026-08-28 20:20:29 +08:00` T00-T01 基线与 profile 合同完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task IDs and status: `T00 completed; T01 completed; Gate A open`
- Base checkpoint: Frosted Heart `master@8b8ee276178ac0d96c7b1a72a5ad656931af6d71`; TheWinterRescue `1.20@2a7cbd2a1412434e84ede3859d952fc273e60df9`
- Files changed: `WearableThermalProfile.java`, `WearableThermalProfileTest.java`, plan, this handoff, and `diary/2026-08-28_20-20-29_warm-stone-t00-t01.md`

Behavior and contracts implemented:

1. `WearableThermalProfile` is an immutable record with exactly four authoritative components: `capacityRatio`, `surfaceCapacityFraction`, `coreSurfaceTransferRatePerSecond`, and `playerTransferRatePerSecond`.
2. `WARM_STONE_DEFAULT` freezes `0.10/0.20/6.1613e-5/1.2e-4`; `HOT_WATER_BAG_DEFAULT` freezes `0.25/0.20/9.2420e-4/8e-5`.
3. Inventory and dropped conductance are derived only as `0.5*g_sp` and `16*k_inventory`; no extra profile field, capability, ItemStack state, exchange solver, or gameplay hook was added.

T00 evidence and decisions:

1. `TemperatureUpdate` performs the normal body pipeline before `PlayerTemperatureData.update`, while `INSULATION` and invulnerable currently share `updateWhenInsulated`; synchronization remains outside the update interval and sends `FHBodyDataSyncPacket` every player START tick.
2. `ThermalExchangeKernel.exchangePairInto` and `exchangeFixedBoundaryInto` are the reusable bounded analytic primitives for T02/T03.
3. Forge `1.20.1-47.3.0` exposes `Item#onEntityItemUpdate(ItemStack, ItemEntity)`; a warm-stone override must return `false` after its bounded work so vanilla entity ticking continues.
4. Curios `5.9.1` documents `ICurioStacksHandler.isVisible()` as UI-only and non-locking, while `getRenders()` is appearance-only. This overrides 9.4 item 6: T08 must ignore both and use exact handler/slot existence. `SlotTypeMessage` remains functional but is deprecated in favor of datapack slot registration; the repository's current IMC path is still the T08 compatibility baseline.
5. Curios detects NBT changes with `ItemStack.matches` against a previous Stack copy and sends `SPacketSyncStack`; exact production packet counts remain a Gate B measurement after writeback exists.
6. Frosted Heart preserved all pre-existing untracked user files. TheWinterRescue had a tracked modification in `kubejs/server_scripts/src/recipes/shaped/new.js`, untracked `.workbuddy/`, no located `AGENTS.md`, and no existing warm-stone IDs; it was not modified.

Validation:

- `gradlew.bat --no-daemon test --tests WearableThermalProfileTest`: `4/4` passed.
- Profile + `ThermalExchangeKernelTest` + `TemperatureComputationRadiationTest`: `10/10` passed, including the existing `100 W/m2 -> 13.333333 degC` radiation regression.
- Existing `com.teammoeg.frostedheart.content.climate.thermal.*`: `33` suites, `187/187` tests passed.

Known limitations and next action:

- No ItemStack NBT, item implementation, Curios slot, player exchange, inventory tick, item receiver, or world runtime behavior exists yet.
- `T02` and `T03` are now unblocked and must consume the frozen record rather than redefine its parameters. Gate A remains open until `T02-T04` pass and curves are recorded.
- Living climate docs were not changed because T01 adds no player-visible implemented behavior.

### 9.6 `2026-08-28 21:01:03 +08:00` T02-T04 完成，Gate A 关闭

- Executor: `Codex; OpenAI GPT-5; primary engineering agent, with A02/A03 sub-agents`
- Task IDs and status: `T02 completed; T03 completed; T04 completed; Gate A closed`
- Base checkpoint: Frosted Heart `master@8b8ee276178ac0d96c7b1a72a5ad656931af6d71`
- Files changed: `ThreeNodeWearableHeatExchange.java` + test, `ReservoirEnvironmentExchange.java` + test, `RadiantEquivalentTemperature.java` + test, `TemperatureComputation.java`, `WearableThermalCurveFixtureTest.java`, plan, this handoff, and `diary/2026-08-28_21-01-03_warm-stone-gate-a.md`

Implemented contracts:

1. `ThreeNodeWearableHeatExchange` owns only the fixed core/surface/player topology. Every `<=1 s` substep is `core-surface half -> surface-player full -> core-surface half`; pair work delegates to `ThermalExchangeKernel.exchangePairInto`, using caller-owned `MutableResult/Scratch`.
2. The exact initial player derivative remains `g_sp*(T_surface-T_player)`. Half-life helpers implement the frozen forward/reverse formula and account for the source profile rates being rounded decimal constants.
3. `ReservoirEnvironmentExchange` owns only the core/surface/fixed-environment topology. Inventory consumes `0.5*g_sp` with zero direct radiation; dropped consumes `8*g_sp` and the shared radiant-equivalent boundary. Both use caller-owned state and bounded symmetric substeps.
4. `RadiantEquivalentTemperature.deltaC` is the single `q*0.8/6` implementation for HUD-equivalent and item-boundary temperature. `TemperatureComputation.radiantFeelingTemperatureDelta` delegates to it; `radiantBodyTemperatureDelta` remains independent.
5. `WearableThermalCurveFixtureTest` freezes both default profiles at `0/60/300/900/1800 s` for delayed worn release (`60/37/37 degC`), inventory cooling (`60/60 degC`, `air=0`, `q=0`), and dropped radiant heating (`0/0 degC`, `air=0`, `q=100 W/m2`). The complete six-decimal CSV is the reproducible curve authority.

Curve checkpoints at `1800 s`:

| Profile | Worn core/surface/player | Inventory core/surface | Dropped core/surface, `q=100` |
|---|---|---|---|
| `warm_stone` | `46.491909 / 41.020417 / 38.000239` | `33.460260 / 18.005584` | `9.652867 / 13.108203` |
| `hot_water_bag` | `48.368763 / 47.738715 / 38.789312` | `45.637727 / 44.100345` | `12.710289 / 12.943663` |

Validation and next boundary:

- T00-T04 direct selection: `7` suites, `27/27` tests passed on JDK 17.
- Expanded climate thermal selection: `39` suites, `209/209` tests passed.
- No ItemStack NBT, item registration, Curios slot, player exchange handler, inventory tick, item receiver, or world runtime was added. Gate A is closed; future Phase 1 work must begin from this checkpoint and reread the final T02/T03 APIs and curve snapshot.
- Living climate docs remain unchanged because no player-visible implemented behavior exists yet.

### 9.7 `2026-08-28 21:53:29 +08:00` T05-T08 状态、物品和 Curios 基础完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task IDs and status: `T05 completed; T06 completed; T07 completed; T08 completed; Gate B remains open`
- Base checkpoint: Frosted Heart `master@8b8ee276178ac0d96c7b1a72a5ad656931af6d71`
- Files changed: `WearableThermalState.java`, `WearableThermalReservoir.java`, `WarmStoneItem.java`, `FHItems.java`, `CuriosCompat.java`, the corresponding three JUnit classes, `build.gradle`, climate living docs, plan, this handoff, and `diary/2026-08-28_21-53-29_warm-stone-t05-t08.md`

Implemented contracts:

1. The only persisted state is `WearableThermalState` under root key `frostedheart:thermal_reservoir`, schema `version=1`, with `initialized`, `core_temperature_c`, and `surface_temperature_c`. It accepts finite absolute temperatures in `[-1000,1000] degC`; `read` is side-effect free. Missing, unknown-schema, malformed, nonfinite, or out-of-range state is rewritten only from a finite server environment sample, after clamping, and otherwise stays unavailable this tick. Failure logs are bounded once per fixed reason, not per Stack.
2. `WearableThermalReservoir` is deliberately a narrow profile/ItemStack-state interface with no Forge capability. The generic `WarmStoneItem` holds its immutable profile, sets `stacksTo(1)`, and permits only `warm_stone` slot `0` through `ICurioItem#canEquip`.
3. `FHItems` registers `warm_stone` and `hot_water_bag` with the frozen default profiles through the repository's Registrate style. A normal registered Stack has no thermal NBT until a later server-side initialization path supplies an environment sample.
4. `CuriosCompat` registers `warm_stone` with priority `190`, `size(1)`, and `frostedheart:slot/empty_warm_stone_slot`. `getWearableThermalReservoirInWarmStoneSlot` reads only the exact handler/slot and ignores `isVisible()`/`getRenders()` because they are presentation state. The test uses handler proxies that fail if either presentation method is touched.
5. `build.gradle` now adds the already-declared Curios API to `testCompileOnly`, which is required because the test source set does not inherit production `compileOnly` dependencies.

Validation:

- JDK 17 targeted T05-T08 test classes: `11/11` passed (`WearableThermalStateTest` `6`, `WarmStoneItemTest` `2`, `CuriosCompatWarmStoneTest` `3`).
- JDK 17 direct T00-T08 selection: `10` suites, `38/38` passed. This includes profile, fixed topologies, curve fixture, exchange kernel, shared radiation conversion, player radiation regression, state, item contract, and Curios query tests.
- The first cold `test` attempt exposed stale/incomplete Gradle class output and then a locked generated test-result binary. A complete `compileJava`, daemon stop, and `--no-daemon` test run produced the passing results above; no source workaround was added for those generated-artifact issues.

Current boundary and next work:

- T09 exclusively owns tags, lang entries, item models/textures, the actual empty-slot icon asset, and tooltip. The registered icon resource reference deliberately remains without an asset until then.
- T10-T12 own player adjustment and all runtime exchange/writeback. No player, inventory, entity, or client tick has been added here; no NBT cadence or packet measurement has begun, so Gate B remains open.
- The companion repository was not inspected or modified because T05-T08 do not change KubeJS, recipes, datapacks, quests, or pack configuration.

### 9.8 `2026-08-28 21:56:43 +08:00` T10 玩家核心温度原子调整完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent, with A10-player-api sub-agent`
- Task IDs and status: `T10 completed; Gate B remains open`
- Files changed: `PlayerTemperatureData.java`, `PlayerTemperatureDataCoreTemperatureAdjustmentTest.java`, plan, this handoff, and `diary/2026-08-28_21-56-43_warm-stone-t10-player-api.md`

Implemented contract:

1. `PlayerTemperatureData.applyCoreBodyTemperatureDelta(float)` applies the same finite delta to `HEAD`, `TORSO`, and `LEGS`, then immediately recomputes `coreBodyTemp`. It never writes `HANDS`, `FEET`, or `prevCoreBodyTemp`.
2. The method precomputes all three candidates before mutation. Nonfinite deltas and finite deltas that overflow any core part return `false` as an atomic no-op; zero returns `true` and refreshes a stale core aggregate.
3. Existing `PlayerTemperatureData.update()` now calls the same private `recalculateCoreBodyTemp()` after copying all five context temperatures. The aggregate remains defined only by `BodyPart.CoreParts` and each part's existing `affectsCore`; update retains its single existing `prevCoreBodyTemp` advance.

Validation and boundary:

- A10's targeted JDK 17 run passed the new test `5/5` and player radiation regression `1/1`.
- Primary-agent JDK 17 review run passed `PlayerTemperatureDataCoreTemperatureAdjustmentTest`, `TemperatureComputationRadiationTest`, and `ThreeNodeWearableHeatExchangeTest`: `3` suites, `15/15` tests.
- No player tick, Curios exchange, ItemStack writeback, inventory exchange, sync, or packet counting was added. T11 consumes this API; Gate B remains open.
- Living climate docs were not changed for T10 because the API is not yet connected to implemented player-visible behavior.

### 9.9 `2026-08-28 22:17:46 +08:00` T09 资源与只读 tooltip 完成

- Executor: `Codex; OpenAI GPT-5; T09 resources sub-agent`
- Task IDs and status: `T09 completed; Gate B remains open`
- Files changed: `FHTags.java`, `FHItems.java`, `WarmStoneItem.java`, `WarmStoneItemTest.java`, Curios tag/model/texture/lang resources, climate living docs, plan, this handoff, and `diary/2026-08-28_22-17-46_warm-stone-t09-assets-tooltip.md`

Implemented boundary:

1. `CURIOS_WARM_STONE` and `data/curios/tags/items/warm_stone.json` admit only `frostedheart:warm_stone` and `frostedheart:hot_water_bag`. Both have generated-item models, `16x16` transparent item textures, locale entries, and the registered Curios empty-slot icon resource exists at `frostedheart:slot/empty_warm_stone_slot`.
2. `WarmStoneItem.appendHoverText` only calls `WearableThermalState.read`. The normal tooltip reports the surface node plus the frozen `10%` or `25%` capacity ratio; advanced tooltip adds the core node. A missing state is labelled uninitialized rather than being initialized by the client.
3. The tooltip helper neither calls `getOrCreateTag` nor writes NBT. It adds no player, inventory, entity, exchange, cadence, sync, or packet behavior; those remain T11/T12 and Gate B work.

Validation:

- JDK 17 `WarmStoneItemTest` and `WearableThermalStateTest`: `10/10` passed. The tooltip tests assert no NBT mutation plus normal/advanced component keys and values.
- The new PNGs were inspected as `16x16 Format32bppArgb`; their models and Curios icon resource path match their consumers.

Asset note:

- The built-in image generation tool was unavailable in this environment. The three small transparent pixel sprites were created deterministically in the repository's existing Minecraft 16x16 raster convention; no external asset or fallback API was used.

### 9.10 `2026-08-28 23:04:03 +08:00` T11 佩戴三节点换热完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task IDs and status: `T11 completed; T12 pending; Gate B remains open`
- Files changed: `TemperatureUpdate.java`, `WearableThermalExchangeHandler.java`, `WearableThermalExchangeHandlerTest.java`, climate living docs, plan, this handoff, and `diary/2026-08-28_23-04-03_warm-stone-t11-worn-exchange.md`

Implemented contract:

1. `WearableThermalExchangeHandler` reads only `WearableThermalReservoir` stacks, converts the player's core offset to absolute Celsius, delegates the internal/surface/player step to `ThreeNodeWearableHeatExchange`, applies the player delta through `PlayerTemperatureData.applyCoreBodyTemperatureDelta`, and writes both item nodes together once.
2. Missing or invalid version-1 state is initialized from the finite server environment and ends that cadence without an exchange. Equal-temperature, invalid elapsed/environment, numeric-degraded, non-reservoir, and rejected-player paths are no-write. `Status.stackWriteCount()` exposes the zero-or-one write result for focused verification.
3. `TemperatureUpdate` invokes the handler after both the normal and `INSULATION` body branches and before the existing `FHBodyDataSyncPacket`. It performs at most one exact `warm_stone` slot lookup per cadence. Creative, spectator, entity-invulnerable, or ability-invulnerable players skip before lookup and initialization, leaving both sides unchanged.

Validation and boundary:

- JDK 17 handler tests passed `7/7`; they cover hot/cold direction, one-write initialization and exchange, equal state, invalid environment/elapsed atomic no-op, management-mode policy, and non-reservoir rejection.
- The T00-T11 direct set passed `12` suites and `52/52` tests. The broader player/thermal/Curios regression passed `47` suites and `241/241` tests. Only existing Mixin/JEI compiler warnings were emitted.
- T11 adds no inventory or `ItemEntity` cadence and no new packet. T12 must implement ordinary-inventory environment exchange, then Gate B must measure actual Curios/container packet counts and tooltip freshness before any dedicated sync is considered.

### 9.11 `2026-08-28 23:49:29 +08:00` T12 普通库存环境交换完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent, with t12-inventory-exchange sub-agent`
- Task IDs and status: `T12 completed; Gate B remains open`
- Files changed: `InventoryThermalExchangeHandler.java`, `WarmStoneItem.java`, `InventoryThermalExchangeHandlerTest.java`, climate living docs, plan, this handoff, and `diary/2026-08-28_23-49-29_warm-stone-t12-inventory-exchange.md`

Implemented contract:

1. `WarmStoneItem.inventoryTick` is the exact entry point. It accepts only a server-side `ServerPlayer` and delegates only when the supplied slot contains the identical Stack object in `PlayerInventory.items`; armor, offhand, external containers, clients, non-player entities, and Curios inventory ticks are excluded.
2. `InventoryThermalExchangeHandler` uses a source-default `20 game tick` cadence with fixed bounded `1.0 s` elapsed. The Curios `warm_stone` slot is queried and an identical equipped Stack is excluded. A handler-owned identity set clears when the server tick changes and prevents duplicate same-tick work without adding cadence state to NBT.
3. The inventory target is stable passive air: `WorldTemperature.naturalAir` supplies a noise-free base and `MinecraftThermalInput.gameplayPassiveEnvironment` passively reads an existing publication or composes analytic fields. The primary review replaced the sub-agent's initial `WorldTemperature.air` call because its Gaussian sampling noise is unsuitable for persistent-state integration. No radiation query or thermal-runtime mutation was added.
4. Missing/invalid state initializes once and ends that cadence. Valid state delegates only to `ReservoirEnvironmentExchange.advanceInventoryInto`; environment connects only to the surface and uses the frozen `k_inventory=0.5*g_sp`. A write occurs only when the persisted float representation changes, so every relevant Stack has zero or one write per cadence.

Validation and boundary:

- JDK 17 T03/T05/T06/T11/T12 focused selection passed `5` suites and `33/33` tests; the T12 handler contributes `11/11`.
- The broader player/thermal/Curios regression passed `48` suites and `252/252` tests. A clean rebuild was required to remove an orphaned generated `ObservableTownMapTest$Recorder.class`; the first offline clean then exposed ForgeGradle's missing `downloadMcpConfig/output.zip`, so one non-offline run rebuilt that generated input before the successful offline regression.
- No synchronization packet was added. Gate B remains open until a real multiplayer/server profile records Curios/container packet counts and tooltip freshness. Boxes and other unticked containers remain frozen; `ItemEntity` behavior remains T13-T19 work.

### 9.12 `2026-08-29 16:23:04 +08:00` Gate B 客户端包观测工具完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `Gate B observability completed; Gate B remains open`
- Files changed: `WarmStoneGateBPacketCounter.java`, `WarmStoneGateBClientCommand.java`, `ClientPacketListenerMixin.java`, optional `SPacketSyncStackMixin.java`, Mixin plugin/config, focused test, climate living docs, plan, and `diary/2026-08-29_16-23-04_warm-stone-gate-b-packet-observability.md`

Implemented boundary:

1. `/fh_gate_b start|status|reset|stop` controls a default-off client observer. It counts all and thermal-reservoir-related Curios Stack, Vanilla container slot, and Vanilla full-content packets separately; full-content summaries also retain the number of matching Stack entries.
2. Every related event logs the received Stack's item ID, owner/container, slot, initialized state, core temperature, and surface temperature. `stop` writes one searchable `FH_GATE_B_SUMMARY`; `probe_errors` must be zero for Curios evidence to be accepted.
3. The observer reads only client-received Stack state. It adds no packet, NBT write, initialization, server scan, cadence, or sync behavior. The Curios mixin is skipped when its internal packet class is absent.

Validation and next action:

- JDK 17 focused counter test passed `3/3`; expanded player/thermal/Curios regression passed `49` suites and `255/255` tests.
- Development `runClient` completed loading with the command registered and no new injection error. The Curios packet target is transformed on first actual use, so the real-world run must also show `probe_errors=0`.
- Gate B remains open. In a real connected world, run the observer across static baseline, worn cadence, ordinary-inventory cadence, and slot movement, then compare logged surface temperatures with tooltip freshness. Do not add dedicated sync unless those measurements demonstrate stale display state.

### 9.13 `2026-08-29 16:49:48 +08:00` Gate B 实测通过并关闭

- Executor: `Codex; OpenAI GPT-5; primary engineering agent, with user manual gameplay verification`
- Task status: `T05-T12 accepted; Gate B closed; C2-world-runtime unblocked`
- Evidence source: development client connected to the integrated server, three `/fh_gate_b start ... stop` runs in `run/logs/latest.log`, plus direct tooltip observation

Measured result:

1. Run durations were `162.126 s`, `135.113 s`, and `28.762 s`. Every summary reported `probe_errors=0`; no Curios `SPacketSyncStack` was received. The actual local-player path for worn, inventory, and slot-movement updates was Vanilla container slot/content synchronization.
2. Vanilla slot totals were `147` (`146` thermal), `84` (`84` thermal), and `20` (`20` thermal), or `0.907/s`, `0.622/s`, and `0.695/s`. These counts are far below a `20/s` every-tick stream. Full-content totals were `3`, `0`, and `1`; they occurred with container UI/lifecycle changes rather than as a continuous inventory broadcast.
3. Existing T11/T12 status assertions and focused tests prove zero-or-one Stack NBT write per relevant cadence. Source review confirms the feature added no full-player, full-inventory, or dedicated reservoir packet path.
4. The user observed the tooltip during these scenarios and reported no stale display or other anomaly. Existing diff synchronization is sufficient, so no quantified targeted-sync design is authorized or needed.

Acceptance boundary:

- Gate B is closed. Preserve the default-off observer for future diagnosis, but do not treat its Curios count of zero as a failed probe: no matching dedicated packet was emitted, and all summaries remained error-free.
- `C2-world-runtime` may proceed from T13 using the accepted T05-T12 contracts and this measurement record.

### 9.14 `2026-08-29 17:31:38 +08:00` T13-T16 world runtime 完成，停在 T17 前

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T13 completed; T14 completed; T15 completed; T16 completed; Gate C remains open`
- Files changed: `RadiationService.java`, `MinecraftThermalInput.java`, `DroppedReservoirExchangeHandler.java`, `WarmStoneItem.java`, focused tests, four climate living docs, plan, this handoff, and `diary/2026-08-29_17-31-38_warm-stone-t13-t16-world-runtime.md`

Implemented contracts:

1. `RadiationService.samplePlayer` and `sampleItem` now share one bounded receiver loop for source buckets, top-K ordering, DDA, revision witnesses, flux, flags, and caller-owned scratch. Player production limits remain exactly `128 receivers / 64 visits / top 8 / 24 rays`; item limits are independently reserved and frozen at `64 / 32 / 4 / 4`, so item churn cannot evict player witnesses.
2. `MinecraftThermalInput.sampleItemEnvironment` reads an existing publication, composes analytic fields, uses stable `WorldTemperature.naturalAir` on miss, and then adds one-point radiation. Its per-level cache holds `64` quarter-block locations for one game-tick generation and clears on generation change or level close. Overflow returns current composed air with zero radiation. The wrapper does not start a runtime, admit a Page, load a chunk, scan blocks, or enumerate entities.
3. `WarmStoneItem.onEntityItemUpdate` is the only dropped entry. It returns `false` after passing only the current exact server `ItemEntity` to `DroppedReservoirExchangeHandler`. A stable UUID bucket delays first work until `20+bucket` loaded ticks and then runs every `20` loaded ticks with fixed `1.0 s` elapsed; no level tick or entity scan was added.
4. Dropped exchange uses `ReservoirEnvironmentExchange.advanceDroppedInto`, the frozen `k_dropped=8*g_sp`, and the shared `q*0.8/6` radiant equivalent environment. Same-tick observations may use radiation; stale observations still advance with their air value and zero radiation rather than freezing and replaying against a future sample.
5. Cadence is derived only from transient `ItemEntity.tickCount`; the handler owns no entity map. Pickup, redrop, dimension change, unload/reload, and deletion therefore reset or discard mode timing while the version-1 Stack temperatures persist. No wall-clock catch-up or cadence/cache NBT was introduced.

Validation and next boundary:

- JDK 17 Phase 1 takeover baseline reproduced `49` suites and `255/255` tests.
- T13 targeted tests passed `13/13`; T14 `14/14`; T15 `28/28`; T16 `30/30`. The first T15 run exposed insufficient bucket distribution in structured UUID fixtures (`27/28`); the deterministic bucket mixer was corrected and the whole target set then passed.
- Final expanded player/thermal/Curios regression passed `50` suites and `266/266` tests with zero failures, errors, or skips. `git diff --check` passed for the implementation slices.
- Stop here. T17 still owns comprehensive one-point/publication/cache regression, T18 owns Campfire/occlusion Forge GameTests, and T19 owns bulk dropped-item workload and cleanup counters. Gate C remains open and T20 is not authorized yet.

### 9.15 `2026-08-29 18:48:53 +08:00` T17-T19 验收完成，Gate C 关闭

- Executor: `Codex; OpenAI GPT-5; primary engineering agent with T17/T18/T19 sub-agents`
- Task status: `T17 completed; T18 completed; T19 completed; Gate C closed; stop before T20`
- Files changed: `RadiationService.java`, `MinecraftThermalInput.java`, their focused JUnit tests, three dropped workload JUnit files, `FrostedHeartMinecraftThermalInputGameTests.java`, lifecycle living-doc test coverage, plan, this handoff, and `diary/2026-08-29_18-48-53_warm-stone-gate-c-closed.md`

Acceptance evidence:

1. T17 freezes one-point distance, occlusion revision hit/retrace, item work/cache caps, actual `128 player / 64 item` receiver capacity and close cleanup. Publication-hit/natural-fallback selection and per-level sample cache capacity/generation/close are directly covered. Test-only observability remains package-private, read-only, and allocation-free.
2. T18 adds one real Forge GameTest batch with warm stone, hot-water bag, and a stone-walled warm-stone control. Both exposed items heat beside a lit Campfire; the wall returns zero direct flux. After extinguishing and moving the exposed items `32` blocks outside source range, both cool. Page/admission/cell/loaded-chunk counters do not grow across item queries.
3. T18's first run passed `12/13`; the only failure was an assumption that extinguishing immediately removes already-admitted mesh residual heat. The accepted test preserves that valid residual behavior and uses the already-planned extinguish-or-move condition instead of changing production physics.
4. T19 verifies `400` identities across all `20` cadence buckets, fixed same-tick sample reuse/overflow, generation recovery, item/player receiver isolation, no entity map, and executable allocation ceilings. The measured steady paths allocate `0 B`, `0 B`, and `304 B` for cadence, sample-cache, and radiation hit workloads respectively; bounded churn remains below its declared ceiling.

Validation and next boundary:

- T17: `2` suites, `18/18`; T18: `compileGameTestJava` plus `13/13 required` GameTests; T19: `3` suites, `7/7`.
- Final JDK 17 player/thermal/Curios regression: `53` suites, `277/277`, zero failures, errors, or skips. `git diff --check` and forbidden-path scans pass.
- Gate C is closed. T20 is now unblocked, but this checkpoint does not inspect or modify the companion repository and does not start Phase 3.

### 9.16 `2026-08-29 19:02:05 +08:00` T20 companion 基线与入口复核完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T20 completed; T21/T22 unblocked; stop before T23`

Verified companion boundary:

1. `D:\games\minecraft\Project TWR\TheWinterRescue` 根目录和递归工作树均未找到 `AGENTS.md`。其仓库内可用的局部贡献约束是 `kubejs/CONTRIBUTING.md`：配方沿用 `server_scripts/src/`，先核对 `functions.js` 与 `recipes/remove.js`，翻译资源按对应 modid 放入 `kubejs/assets/`。
2. FrostedHeart 的注册、tag、Curios 和 ItemStack 合同仍完整：`frostedheart:warm_stone`、`frostedheart:hot_water_bag`、`frostedheart:thermal_reservoir`，以及 identifier 为 `warm_stone`、`size(1)` 的专用槽。两件物品的掉落加热继续只走已验收的通用 `ItemEntity` environment receiver；不改 receiver 的 budget、cadence、cache、同步或生命周期。
3. TheWinterRescue 没有上述旧 ID、thermal-reservoir 数据、同名 Curios 槽或暖石/热水袋配方。`caupona:nail_soup` 是现有的 Hot Water 流体，篝火和烟熏现有配方可把水容器加热为它，Create mixing 也可生产它。可选热水灌装只能消耗这个已存在的热输入并显式写入两个节点的相同初温；无热输入的制作结果保持未初始化。
4. `frostedheart:charger` schema、机器和 activated-carbon 配方均已存在，因此本阶段明确不新增暖石/热水袋 charger recipe 或 cost，也不对玩家宣称它可用。

Repository state at verification:

- FrostedHeart: branch `master`; T00-T19 的未提交实现仍存在，`git diff --check` passed.
- TheWinterRescue: branch `1.20`; an existing staged change remains in `kubejs/server_scripts/src/recipes/shaped/new.js` and `.workbuddy/` remains untracked. Neither was modified. `git diff --check` and `git diff --cached --check` passed.

### 9.17 `2026-08-29 19:07:05 +08:00` T21 制作与真实热水灌装入口完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T21 completed; T22 unblocked; stop before T23`

Implemented companion recipes:

1. `the_winter_rescue:minecraft/crafting_shaped/warm_stone` creates `frostedheart:warm_stone` from `minecraft:smooth_stone` and `frostedheart:straw_lining`.
2. `the_winter_rescue:minecraft/crafting_shaped/hot_water_bag` creates `frostedheart:hot_water_bag` from `frostedheart:leather_water_bag` and `frostedheart:straw_lining`.
3. Both ordinary outputs omit `frostedheart:thermal_reservoir`; their first temperature remains server-environment initialization, never a recipe-created high temperature.
4. `the_winter_rescue:minecraft/crafting_shapeless/fill_hot_water_bag` accepts only an existing `frostedheart:wooden_cup_drink` containing `250 mB caupona:nail_soup` alongside any hot-water bag. It returns an empty wooden cup and writes version `1`, `initialized=true`, `core_temperature_c=60`, and `surface_temperature_c=60` to the resulting bag. The existing cup is a verified heat input, not a new thermal source.

Validation and boundary:

- `node --check kubejs/server_scripts/src/recipes/warm_stone.js` and static required/forbidden-ID assertions passed.
- `git diff --check` passed in FrostedHeart and both index/worktree checks passed in TheWinterRescue.
- No special Campfire recipe, no charger recipe or cost, no new temperature synchronization, and no T13-T19 runtime edit were introduced. The existing staged `new.js` change remains untouched.

### 9.18 `2026-08-29 19:20:14 +08:00` T22 研究、任务与 Create tooltip 完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T22 completed; T20-T22 accepted; stop before T23`

Implemented companion progress content:

1. `config/fhresearches/warm_stone.json` is a `frostedresearch:living` child of `hand_warmer`. Its recipe effects unlock `frostedheart:warm_stone` and `frostedheart:hot_water_bag`; its bilingual text accurately documents the one-slot Curios boundary, bidirectional heat exchange, registered physical-source charging, and unticked-container pause.
2. Optional quest `6A97729E570341EF` (`Carry The Warmth`) is placed in `t0.snbt` after the existing Campfire quest `20E34B234337C28A`. It has separate item tasks for both reservoir items and uses matching English/Chinese explanation keys.
3. The new `twr_tooltips` entries provide Create-style summary/condition/behaviour text for both items. They state the dedicated `warm_stone` slot, cold and hot bidirectional behavior, dropped charging beside a burning Campfire or other registered physical source, and no temperature evolution in an unticked container. The hot-water bag additionally states its established Hot Water initialization without mentioning a charger.

Validation and remaining boundary:

- JSON parse, KubeJS syntax, research parent/effect, new language keys, quest SNBT balance/references, and no-new-charger-text assertions passed. Both repository `git diff --check` checks passed.
- Forced JDK 17 player/thermal/Curios regression: `53` suites, `277/277` tests, zero failures/errors/skips.
- `validateResearchCatalog` over the complete companion catalogue reports four pre-existing missing `workbench` parents (`coke_oven`, `mechanical_bellows`, `storage_drawers`, `tetra`); it reports no `warm_stone` error. An isolated two-file preflight was not run because the environment rejected creation of its temporary test directory.
- `config/.gitignore` now whitelists `!/fhresearches/*.json`; without it, the root `*` rule hid the new research file despite the directory whitelist. `warm_stone.json` is therefore visible in repository status.
- Stop here. T23 exclusively owns development test Stack and manual-observation tooling. The presentation-follow-up default aggregate-tooltip/config work is still deferred pending explicit user direction.

### 9.19 `2026-08-29 19:40:03 +08:00` T23 开发测试 Stack 与温度序列工具完成

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T23 completed; T24 unblocked`

Implemented tool boundary:

1. OP-only `/fh_warm_stone_test give <warm_stone|hot_water_bag> <cold|environment|hot|core_hot_surface_cold>` creates one independent version-one reservoir Stack. Presets are fixed at `-20/-20 degC`, current `WorldTemperature.naturalAir` on both nodes, `60/60 degC`, and `60/0 degC` respectively. No debug item, tag, recipe, creative-tab entry, runtime mutation, or dedicated synchronization was added.
2. `/fh_warm_stone_test observe start [interval_ticks]|status|stop` samples only the equipped dedicated Curios slot. It is default-off, emits immediately then every default `20` ticks (or `1..1200` caller-selected ticks), and records `FH_WARM_STONE_OBSERVE` with game tick, player core, reservoir item, core, and surface temperatures. Empty and uninitialized states remain visible rather than being changed.
3. Observation records are transient command state, are removed when the player logs out or the server stops, and do not enter ItemStack/player NBT or T13-T19 receiver lifecycle.

Validation and next boundary:

- `WarmStoneTestCommandTest` plus `WearableThermalStateTest` passed `2` suites and `10/10` tests under JDK 17. The command test covers all presets, state NBT, command shape, and formatted three-temperature line.
- The first command-tree test correctly exposed eager Registrate access from a method reference. Replacing it with a deferred Supplier made command registration testable before registries are live; the targeted rerun passed.
- `git diff --check` passed. No TheWinterRescue file changed in T23; its existing T20-T22 state remains untouched.
- T24 now owns the next required broad automated validation; T25 retains the real in-game measurement matrix. The deferred aggregate-tooltip/config work remains out of scope.

### 9.20 `2026-08-29 20:48:22 +08:00` T24 自动化完成，停在 T25 前

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T24 completed; T25 unblocked; stop before T25`

Validation result:

1. JDK `17.0.2` forced targeted JUnit covered the kernel, profiles, three-node/environment models, curve fixtures, NBT, item/Curios, worn/inventory/dropped paths, Gate B observer contract, player core adjustment, T23 command, radiation, and Minecraft item environment/workload: `21` suites and `101/101` tests, with zero failures, errors, or skips.
2. Forge GameTest recompiled and passed all `13/13 required` tests: `12` thermal plus `1` Frosted Research. The dropped-reservoir batch again covered both items heating beside a lit Campfire, cooling after moving away, and stone-wall occlusion.
3. The first full `test` run found one pre-existing failure among `868` tests: `TeamTownActualSaveCodecProbeTest` depended on a hardcoded private macOS save path. The test now constructs a repository-owned `NbtOps` persistence payload before exercising the same full-sync packet path; its focused `1/1` test passed, followed by `201` suites and `868/868` full tests with zero failures, errors, or skips. Full `build` passed.
4. TheWinterRescue passed KubeJS syntax, seven JSON parses, research parent/effect, quest, bilingual language, hot-water NBT, forbidden charger-path, and both worktree/index diff checks. Full `validateResearchCatalog` still reports only the known missing `workbench` parents in `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra`; it reports no `warm_stone` problem.

Boundary:

- No warm-stone runtime behavior, T13-T19 receiver budget/cadence/cache/sync/lifecycle contract, campfire recipe, charger recipe/cost, dedicated synchronization, or living documentation changed in T24.
- T24 is complete. Stop before T25; the real gameplay matrix and balance measurements remain T25 work. The aggregate-temperature Tooltip/config follow-up remains deferred until the user explicitly restores it.

### 9.21 `2026-08-30 15:37:40 +08:00` T25 实机矩阵完成，停在 T26 前

- Executor: `Codex; OpenAI GPT-5; primary engineering agent, with user manual gameplay verification`
- Task status: `T25 completed and accepted; T26 unblocked; stop before T26`

Acceptance evidence:

1. Isolated hot/cold worn sequences recovered the frozen surface-player rates within `0.1%` over each full run: warm stone approximately `1.2e-4 /s`, hot-water bag approximately `8e-5 /s`. Logged normalized three-node energy residual stayed below `0.001`. Warm stone led initially; the larger bag overtook cumulative hot/cold player movement at about `136 s/204 s`.
2. A `60/0 degC` hot-water bag cooled the player first, reached its player-temperature minimum at about `55 s`, and reversed after its surface crossed the player at about `57 s`. In the `-20 degC` control field, 180-second player changes were `-0.178/-0.091/-0.073 degC` for empty/warm-stone/hot-water-bag cases.
3. Inventory versus dropped state, lit Campfire, moving away, wall occlusion, unticked chest pause, Curios capacity/icon/render toggle, invulnerable modes, dimension/relogin/restart persistence, tooltip freshness, and threshold effects were covered. The user reported no visible anomaly. The heating-pad run remained net-cooling in the cold ambient but warmed the reservoir surface; it is retained as qualitative ordering evidence only and did not authorize tuning.
4. Full client restart preserved the non-equilibrium item: pre-save player/core/surface was `37.183/48.185/46.922 degC`, first post-restart observation was `37.165/48.130/46.988 degC`, then all nodes continued in the expected directions. No uninitialized or reset state appeared.
5. The final multi-entity smoke run covered `31` distinct thermal slot IDs for about `345.936 s`. Nine `/forge tps` samples all held `20.000 TPS`; total mean tick time ranged `11.935..19.080 ms` and averaged `16.404 ms`. Gate B over `364.032 s` reported `1817` thermal slot packets, zero content/Curios packets, and `probe_errors=0`; no catch-up warning or warm-stone error occurred.

Boundary:

- No production behavior, frozen profile, T13-T19 receiver contract, recipe, synchronization path, or living document changed in T25.
- T25 is complete. T26 owns living-document consolidation; T27 owns final separate repository validation; T28 owns final plan/diary closure. The aggregate-temperature Tooltip/config follow-up remains deferred until explicitly restored.

### 9.22 `2026-08-30 16:00:42 +08:00` T26-T28 完成，计划关闭

- Executor: `Codex; OpenAI GPT-5; primary engineering agent`
- Task status: `T26-T28 completed; Gate D closed; plan completed`

Final result:

1. T26 consolidated the final wearable-reservoir formulas, half-life conversion, frozen defaults, NBT/Curios/synchronization ownership, inventory/dropped/container lifecycle, item-environment receiver boundary, and T25 evidence into the three owning climate documents and their index. All three documents are current as of `2026-08-30`.
2. T27 used JDK `17.0.2` for one forced FrostedHeart `test build runGameTestServer` execution: `201` JUnit suites and `868/868` tests passed with zero failures, errors, or skips; full build passed; all `13/13 required` Forge GameTests passed. Static ID/tag/model/version-one NBT, no-dedicated-sync, no-charger, and diff checks also passed.
3. TheWinterRescue independently passed one KubeJS syntax check, seven JSON parses, research/quest/language/Hot Water NBT assertions, unique recipe-path and forbidden-charger scans, and worktree/index diff checks. Full catalogue validation still reports only the known missing `workbench` parents in `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra`; `warm_stone` is not an error.
4. The Forge GameTest server emitted one startup-load `280 ticks behind` warning while all required tests passed. This does not replace the T25 steady gameplay evidence: nine `/forge tps` samples remained `20.000 TPS` with no catch-up warning in the normal-world multi-entity smoke.

Closure boundary:

- Frozen profiles and the T13-T19 receiver budgets, cadence, caches, synchronization and lifecycle contracts did not change. No campfire recipe, charger recipe/cost, dedicated reservoir synchronization, or aggregate-temperature Tooltip/config was added.
- T00-T28 and Gates A-D are complete. Unticked containers still pause, offline wall time is not replayed, and only dropped reservoirs charge through the generic registered physical-source environment receiver. The aggregate-temperature Tooltip/client-config follow-up remains deferred until explicitly restored.
