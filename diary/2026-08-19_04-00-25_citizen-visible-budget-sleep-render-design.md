# 居民可见预算与床上睡眠展示设计

- Time: `2026-08-19 04:00:25 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `town citizen presence, AOI synchronization, sleeping presentation, client LOD, and server configuration`

## Completed

- 完成“睡眠居民在床上可见、超过上限自动隐藏、全天动态平衡可见数量”的实现级设计。
- 复核了现有睡眠定位、床朝向、错峰醒来、AOI、客户端缓存、批量渲染和近距假实体路径。
- 本次只形成设计，不修改 Java 实现，也不新增或运行测试。

## Goals

- 有效床位上的睡眠居民与清醒居民共享同一个显示预算。
- 任一玩家客户端最多持有并显示 `K` 个居民；真实人口、行为和存档不受 `K` 影响。
- 人数或观察位置变化后自动补充最近的可见居民，同时避免边界居民反复闪现。
- 睡眠居民水平躺在对应床上，不参与移动、分离或交互。
- 夜间大量入睡不能造成假实体激增、全量对象分配或高频 spawn/despawn 抖动。

## Non-goals

- 不限制城镇真实人口，不删除或暂停隐藏居民。
- 不改变住宅容量、床位分配和错峰醒来规则。
- 不把可见性写入居民存档，也不增加每玩家、每居民的持久化关系。
- 不在本阶段引入遮挡查询、GPU instancing 或新的服务端实体。

## Architecture

显示预算按观察者独立计算，而不是按维度全局计算：

```text
真实人口 / 行为 / 住宅与床位
              |
              v
       presentationEligible
              |
              v
   玩家 AOI 内稳定、有限的 Top-K
              |
       +------+------+
       |             |
     spawn         despawn
       |             |
       +------> 客户端缓存 <= K
                     |
           +---------+----------+
           |                    |
      清醒近距假实体       批量 LOD 渲染
                                |
                         睡眠水平床上模型
```

选择每玩家预算的原因：网络包、`ClientCitizenCache`、假实体和渲染成本都由观察者分别产生。维度全局预算会让多人争抢同一批名额，既不能准确限制单个客户端成本，也会造成玩家互相影响。

## Budget Semantics

- 新增服务端配置 `maxVisibleCitizensPerPlayer`，建议默认 `128`，范围 `0..4096`。
- `K = 0` 时隐藏全部居民；`K > 0` 时是严格的每玩家上限。
- 预算统计清醒和睡眠居民的合计，不为睡眠居民另设额外名额。
- AOI 继续使用当前 96 格半径；AOI 内符合展示资格的居民少于 `K` 时全部显示，否则只显示稳定 Top-K。
- 多名玩家可对同一居民得到不同结果；这只是网络展示差异，不改变服务端模拟状态。
- 上限永不因补位而突破。候选失效后可立即移除，空出的名额最多在下一次 20 tick AOI 刷新时补齐。
- 配置降低时，下一个服务端 tick 标记预算需要刷新；配置提高后也只补发新增名额，不重建模拟。

这里选择“严格上限 + 最多 1 秒补位”，而不是每个状态变化都重新扫描全部居民。后者在夜间批量入睡时会把一次每秒的有界工作放大为每 tick、多次全量重排。

## Presentation Eligibility

当前 `CitizenPresence.networkVisible` 同时承担同步、渲染和交互语义，需要拆分：

```text
behaviorScheduled(state)       行为状态机是否继续运行
movementIntegrated(state)      是否移动和贴合地形
spatialPresent(state)          是否进入空间网格与分离
presentationEligible(sim, i)   是否可进入显示候选集
interactionAllowed(state)      是否允许客户端操作
```

展示资格规则：

```text
合法的非 SLEEP 状态                         -> eligible
SLEEP + 已定位到当前有效床头                 -> eligible
SLEEP + 无床、床失效、区块不可验证或入口回退 -> hidden
非法状态                                     -> hidden
```

交互规则独立判断：`SLEEP` 永远不可交互。`C2SCitizenActionPacket` 应使用 `interactionAllowed`，不能再用“是否允许展示”代替权限判断。

## Transient Bed Flag

在 `CitizenSim` 增加瞬态 `byte[] presentationFlags`，首个 bit 为 `PRESENT_ON_VALID_BED`：

- 仅在成功验证床头并完成睡眠定位后置位。
- 醒来、换房、床位失效和居民移除时清零。
- 随 SoA 容量增长和尾项交换，不进入 Codec/NBT。
- 不在 `SyncEngine` 的每玩家扫描中重复查询方块状态。

选择 1 字节而不是位图，是为了保持 SoA 索引交换、扩容和热路径读取简单且连续。成本约为每万名居民 10 KB；相比按稳定 id 保存集合或每秒重复世界查询，内存、CPU 和实现风险更低。不要复用 `halt` 或在 `state` 高位塞标志，这会把移动与协议语义耦合到展示逻辑。

## Stable Top-K

在 `SyncEngine.refreshAOI()` 中用无对象的定长最大堆替换“AOI 内全部加入”：

1. 顺序扫描各 `CitizenContainer`，过滤展示资格与 96 格 AOI。
2. 对候选计算稳定排名，最大堆只保留排名最好的 `K` 个 id。
3. 将堆结果写入一个可复用的临时 `IntOpenHashSet selected`。
4. 原地修改现有 `tracked[player]`：旧集合有而 `selected` 没有的发送 despawn；`selected` 有而旧集合没有的发送 spawn。
5. 保留后的居民继续走现有批量增量同步。

排名按以下顺序决定：

1. 当前正在与该玩家交互的居民优先，但仍占用一个名额且不突破 `K`。
2. 已在 `tracked[player]` 中的居民获得 4 格保留优势。
3. 按玩家平面距离排序。
4. 距离相同按稳定 citizen id 排序。

建议使用 1/16 格量化的有效距离：

```text
distanceQ = floor(sqrt(distanceSquared) * 16)
effectiveQ = max(0, distanceQ - (previouslyTracked ? 64 : 0))
rank = (effectiveQ, citizenId)
```

这形成确定性的全序，并要求新居民至少明显更近才替换旧居民。不要使用“距离差小于 4 格时旧居民优先”的成对比较器，因为该比较规则可能不满足传递性，会破坏堆排序。

堆使用可复用的 `int[K] ids` 和 `long[K] ranks`，逐玩家清空并复用；不创建 `Candidate` 对象。`selected` 也可作为 `SyncEngine` 级共享 scratch，在玩家之间顺序复用。每玩家只保留已有的一个 `tracked` 集合。

## State Transitions

### Entering sleep

```text
RETURN_HOME 到达入口
  -> state = SLEEP
  -> 解析 UUID 稳定 homeSlot 对应的床头
  -> 验证 BedBlock + HEAD，读取 FACING
  -> 成功：定位床头中心、写入已有 dir、置 PRESENT_ON_VALID_BED
  -> 失败：清标志、保留入口回退位置，但不允许展示
  -> 清除移动/分离缓存
  -> 通知同步层发生高优先级状态与位置变化
```

必须移除 `BehaviorSystem` 与 `TownSimData.onSleepEntered()` 当前对有效睡眠居民的无条件 `notifyHidden()`。只有无有效床位的睡眠居民才进入 `pendingHidden`。

### Sleeping

- 继续参与行为调度，以便早晨自行醒来。
- 不参与移动、空间网格和分离。
- 有效床位居民可以参与 Top-K，但静止后不发送移动心跳。
- 房屋布局刷新时重新验证正在睡眠的居民；床位失效则清标志并立即 despawn。

### Waking

```text
SLEEP
  -> 清 PRESENT_ON_VALID_BED
  -> 按现有确定性出口算法放到住宅出口
  -> 进入 WORK 或 WANDER
  -> 恢复移动与空间系统
  -> 通知同步层高优先级发送状态与位置
```

清醒后是否显示仍由同一个 Top-K 决定。预算不能改变醒来时间、出口位置或行为状态。

## Transition Synchronization

新增瞬态 `pendingImmediate` id 集合，用于睡眠和醒来的离散位置变化：

- 下一次现有 4 tick flush 绕过距离档位限频，立即发送最终状态、位置和方向。
- 发送后沿用当前 canonical model 回写并清除 id。
- 无有效床位使用 `pendingHidden`，不发送睡眠位置。
- 状态变化同时只标记 AOI 预算为 dirty，仍由合并后的刷新节奏补位。

客户端 `ClientCitizen.update()` 检测进入或离开 `SLEEP` 时应直接 snap 到新快照，并重置插值窗口。否则现有“从当前渲染位置开始 lerp”会让居民从门口穿墙滑到床上，或早晨从床滑到出口。

现有 state+dir 打包字节已经能传递床朝向，不需要扩展 spawn/batch 协议。

## Sleeping Rendering

睡眠居民统一由 `ClientCitizenRenderer` 的批量路径渲染，不创建 `FakeCitizenEntity`：

- `FakeCitizenManager` 驱动已有实体时遇到 `SLEEP` 立即移除。
- 第二遍创建近距实体时跳过 `SLEEP`。
- 批量渲染器对 `SLEEP` 不受 `FakeCitizenManager.has(id)` 的普通站立路径影响，始终选择睡眠模型。

睡眠模型以同步的床头中心和 `dir` 为锚点：

- 模型沿床朝向水平旋转，身体从床头向床尾延伸。
- Y 取床面高度加一个很小的视觉偏移，避免 z-fighting。
- 身体和头部使用两个扁平盒体；关闭行走起伏和视觉软转向。
- 0..64 格绘制水平低模人形；64..96 格绘制贴近床面的水平轮廓 quad。
- 睡眠状态使用独立低矮 AABB 做视锥剔除，避免继续使用 1.9 格高的站立包围盒。

这样近距睡眠居民也不会支付实体注册、原版实体 tick、动画和生命周期维护成本；一个批次即可覆盖全部可见睡眠居民。

## Configuration

配置放在 `FHConfig.SERVER.TOWN` 下，而不是客户端配置：

```text
maxVisibleCitizensPerPlayer = 128
```

服务端是 tracked 集合和网络流量的权威，客户端无需同步一份上限。Forge 配置热更新后，`SyncEngine` 在下一 tick 读取新值并请求一次合并 AOI 刷新。

## Cost Model

| 项目 | 成本 |
|---|---:|
| 居民存档增量 | `0 B / resident` |
| 服务端床上展示标志 | `1 B / resident capacity` |
| 每玩家长期追踪 | `O(K)`，且由配置硬限制 |
| 同步选择 scratch | 一个共享 `O(K)` 堆和集合 |
| AOI 刷新 CPU | `O(P * C * log K)`，每 20 tick 一次 |
| 客户端居民缓存 | `<= K` |
| 睡眠假实体 | `0` |
| 稳态睡眠网络 | 状态切换一次，之后接近 `0` |

其中 `P` 为当前维度玩家数，`C` 为扫描到的居民数。`K=128` 时堆深度最多 7；原始堆数组约 1.5 KB，并由所有玩家刷新复用。相比当前无限增长的 AOI tracked/cache，上限会直接限制网络、客户端对象和渲染开销。

若以后实测超大维度人口使全容器扫描成为瓶颈，再单独引入展示空间索引；本方案不提前复制一套睡眠空间网格，避免为尚未出现的瓶颈增加长期存储与一致性维护。

## Failure Handling

- 床被拆除或不再是床头：清床标志并隐藏；绝不在住宅入口绘制重叠睡眠人形。
- 床所在区块无法验证：当夜先隐藏，待住宅布局/区块有效事件触发重新验证后才可显示。
- 玩家高速移动：AOI 外居民在刷新时无条件淘汰，4 格优势不能跨越 96 格边界。
- 临界距离多人密集：保留优势和稳定 id 消除每秒随机换人。
- 多玩家观察同一住宅：各自独立 Top-K，一名玩家隐藏不影响另一名玩家。
- `K` 小于近距人数：严格选择最近的 `K` 个，不因近距或睡眠状态突破上限。
- `K = 0`：清空 tracked/cache；不能发起新的居民交互。
- 交互对象发生重排：交互对象占预算内固定名额，替换最远的非交互居民；睡眠居民不可固定。

## Implementation Order

1. 在 `CitizenPresence` 拆开展示资格与交互资格，并增加瞬态床展示标志。
2. 调整 `TownSimData` 入睡、床位刷新和醒来回调，取消有效睡眠的无条件隐藏。
3. 在 `SyncEngine` 实现共享原始类型最大堆、原地 tracked diff、迟滞排名和配置热刷新。
4. 增加离散睡眠/醒来位置的 `pendingImmediate` 同步。
5. 在客户端对睡眠切换 snap，并让假实体完全跳过 `SLEEP`。
6. 在批量渲染器增加水平睡眠低模与远距轮廓。
7. 最后做多人独立预算、配置升降、床拆除、夜间集中入睡和早晨错峰醒来的游戏内验证。

## Acceptance Criteria

- AOI 内符合展示资格的人数不超过 `K` 时，清醒与有效床上睡眠居民全部可见。
- 人数超过 `K` 时，每个玩家的 tracked 集合和客户端缓存始终不超过 `K`。
- 睡眠居民与所分配床一一对应、方向一致、水平显示，且没有近距假实体。
- 无效床位居民不会在入口或 `housePos` 处堆叠显示。
- 入睡和醒来不会出现穿墙滑动；状态变化最多 4 tick 到达已追踪客户端。
- 候选变化后最多 20 tick 补齐空缺，边界居民不会持续 spawn/despawn 抖动。
- 两名玩家的位置和结果互不干扰，配置变化不修改人口、床位、行为或存档。

## Decisions

- 采用每玩家统一预算，而不是维度全局预算或清醒/睡眠双预算。
- 采用有界最大堆和 4 格保留优势，在内存上保持 `O(K)`，在视觉上保持稳定。
- 采用 1 字节瞬态床标志，避免 AOI 热路径世界查询，也不污染存档。
- 睡眠统一走批量水平模型，不创建假实体。
- 允许最多 20 tick 的补位延迟，以合并夜间大量状态变化；上限本身始终优先。

## Validation

- 静态复核了 `CitizenPresence`、`BehaviorSystem`、`TownSimData`、`CitizenSim`、`SyncEngine`、`ClientCitizen`、`ClientCitizenRenderer`、`FakeCitizenManager`、`HouseBuilding` 与 `FHConfig` 的当前实现边界。
- 按用户要求未运行测试、构建或客户端。

## Remaining

- 按上述顺序实现；本次仅交付设计。
- 实现后进行游戏内视觉和多人预算验证，不需要新增测试类。
