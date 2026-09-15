# 恢复解析场红外显示并删除自然背景

- Time: `2026-09-12 19:25:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial`
- Scope: `红外显示修复代码与自动测试完成；用户停止客户端操作后的实机验收待完成`

## Completed

- 删除`InfraredCapture.background`、729值同步、背景key/flags、客户端背景buffer/纹理/上传/reset及仅服务背景的`peekNaturalTemperature`。
- 以surfaceNodeMask材料值为基础，恢复`MinecraftGameplayFields`有序解析场合成。GeneratorData球形保底作用于已有冷材料；无材料时仅在命中场且公式需要时查询原naturalAir。无场缺值保持INVALID，shader蓝色占位。
- 恢复前后field Page并集刷新，复用Page/Brick候选裁剪、Sample、按需邻区及精确同biome Y层复用；场路径double合成后量化，无场保留量化节点快路径。不创建runtime、不加载chunk、不改求解器或写回H/C/T。
- 协议统一为最终display事务，保留物理readable作为请求状态而非渲染开关。局部失败重建该Page的场/INVALID，全局物理失效提交field-only full；同一不可读状态可继续delta。delta显式INVALID取代客户端按材料presence擦Page，LAST才提交两个mask、origin与epoch。
- 保留原-20..20°C色标、0.43混合、64-block扫描和材料内侧深度取样；没有Embeddium顶点改动、新世界draw或第二温度纹理。
- Documentation impact: 更新世界温度、runtime、网络生命周期及两个plan入口，保留先前实施历史。其他开始前工作区改动、design与伴生整合包未改动。

## Decisions

- 解析场修正后的displayTemperature不是材料实测；明确恢复用户要求的玩法热区显示，不把它伪称材料真实升温。
- 删除区域自然估计不等于删除能量塔相对自然温度保底的必要输入。无场窗口不做IR自然查询；有场成本随刷新范围增长，暂不引入观察者缓存、revision/hash或GPU场解释器。
- 测试客户端使用新建`Codex Infrared Repair`世界。用户物理Esc停止控制后未再操作游戏，没有把未见到的最终画面作为验证通过。

## Validation

- Java17 `compileJava compileGameTestJava --offline --no-daemon --console=plain`通过（43s）。
- 完整`runGameTestServer --offline --no-daemon --console=plain`：58项全部通过（2m21s），未排除测试类、未写/跑JUnit。命令沿用`-Dnet.minecraftforge.gradle.check.certs=false`跳过已知卡住的离线联网预检，项目配置未改。
- 新/修订夹具覆盖无runtime蓝底、真实GeneratorData塔底场、调档/缩小/删除、材料与Air分离、冷材料场保底、局部/全局不可读与场保留、field-only delta、0°C、合成后量化、PILLAR边界、包field mask/full中心与完整RAW分包；保留144组深度数值检查。
- 日志：`run-gametest/infrared-field-repair.log`、`build-infrared-field-repair-compile.log`、`run/infrared-field-repair-client.log`。
- 100次GeneratorData RLevel=2 delta：capture p50 3.5303 ms/p95 8.3229 ms、wire 3,971,100 B；100次full：p50 3.4527 ms/p95 3.812 ms、wire 3,754,500 B。营火+半径8场delta：p50 0.2582 ms/p95 0.4022 ms、wire 288,300 B/100次。串行固定窗口样本；不是实际百人服、GPU或retained heap基准。
- `git diff --check`通过；查找确认生产红外路径不再有background/旧materialUpdate字段。客户端已加载本次构建并进入新世界，日志确认红外shader资源加载；实机蓝底与场热色对照未完成。

## Remaining

- 当前客户端继续验证实际蓝底、0°C热色、关塔恢复、不同图形模式/resize/第三人称和实体/粒子遮挡；按用户停止界面控制的要求，本轮不再自动操作。
- 目标场景的受控GPU/堆分配和真实多玩家性能验收。代码实现与自动测试已完成，不声称已证明全场景最佳性能。
