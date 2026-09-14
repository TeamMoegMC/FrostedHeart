# 恢复水的原地表随机抽样

- Time: `2026-09-15 00:24:30 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `水的随机更新调度；保留统一材料能量和相变规则`

## Completed

- 按用户要求恢复`ServerLevelMixin_TemperatureUpdate`的Chunk地表抽样：默认每20tick随机选一个X/Z列，通过MOTION_BLOCKING定位地表，命中水后调用现有统一相变入口。冻结不要求正在降水；randomTickSpeed为0时仍禁用。
- `StateTransitionData.hasRandomTransitions`使水不增加方块随机资格，同时让通用Section相变入口跳过水。其他方块开启Section抽样时也不会额外执行水冻结。资格重载比较复用同一判断。
- 水的材料law、潜热、活动worker相变及地表命中后的休眠能量交接保留。未恢复重复水温算法、旧概率配置或旧格式兼容层，没有新增Mixin、调度器或节点状态。
- 静态辐射按用户的游戏平衡设计保持独立；文档明确它与材料温度脱钩不属于缺陷。
- Documentation impact: 更新热源/相变、runtime、生命周期文档及原plan中的调度决定。未修改design、配方或伴随仓库。

## Decisions

- 修正此前“纯水Section一定跳过”的表述：已核对当前Forge映射的`LevelChunkSection`源码/字节码，`setBlockState`增量统计非空流体，而`recalcBlockCounts`只统计需要随机刻的流体。因此变动过的纯水Section也可能按原生规则进入抽样。保留这个既有行为，本次撤回的是额外的水方块随机资格和水相变检查。
- 地表水冻结抽样次数按参与更新的Chunk计算，不因海洋水深增加。覆盖/地下休眠水不再获得此前新增的Section随机相变机会，活动能量模拟独立执行。

## Validation

- Java17 `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：**90/90通过**，[最终日志](../build/thermal-water-surface-validation.log)。
- 新增纯水/全部液位资格与Section计数回归；新增实际`ServerLevel.tickChunk`回归，在包含其他随机方块的Section中验证未到期不冻结、零随机速度不冻结、单次到期调用最多冻结一个地表水方块、覆盖水不冻结。
- 保留88项已有回归。首轮错误地断言原生纯水增量计数必须不抽样，核实原生源码后修正为“不增加方块随机计数”；另为已有休眠冻结交接测试加入局部冷场，固定环境条件，并增加提交失败诊断，最终完整运行通过。
- `git diff --check`通过。静态辐射、shader及材料能量生产代码未改。

## Remaining

- 真实多人整体MSPT未实测；本次确认调度范围恢复与行为回归，不声明全服负载已达最优。
- 普通JUnit旧类型引用与完整datagen掉落表问题仍属先前记录的独立事项。
