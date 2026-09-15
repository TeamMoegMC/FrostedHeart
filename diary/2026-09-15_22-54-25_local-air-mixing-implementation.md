# 实施近源空气混合与1/0.5°C滞回

- Time: `2026-09-15 22:54:25 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `生产空气连接、源生命周期、驻留请求及必要回归；不改材料相变规则、静态辐射或渲染实现`

## Completed

- 按用户最终选择实施1°C新活动/扩张、0.5°C保留/释放滞回；没有实施已撤回的统一0.5单阈值。
- AirPairs用一个方向byte取代两份Y double数组；真实接触轴及排序后的上下端点决定浮力。水平直接用G，纵向沿用原公式，多方向同节点对分别聚合。
- 新增一个生产helper `AirMixingRegion`：四格范围内真实直接Air单位面4倍，重叠范围取并集；整Brick面只放大实际覆盖的单位面。一个编译期复用端口scratch，不增加Air节点或查询插值。
- 源绑定复用原Section索引，通过ledger只读状态更新每源一个emitting标记；注册/移除/开关/零功率/位置变化记录附近已驻留的world-Brick位置。正功率调整不重建范围，源只改变G时不替换H节点。
- 一份pending集合跨WORK_LIMITED保留，成功拓扑提交后清空。TopologyPlan复用fragment-only路径，源关闭后能量由原账本立即停供。
- 修复材料邻居已经驻留时被跳过、导致持续需求消失的问题。保持原材料/面条件，读取已提交邻居签名并继续orDesired，不加保留计时器或新引用计数。
- hot位图在下次publish开始时交换，驻留收集后仍可读取最新完成cut；保留真正需要的两份滞回位图，删除旧finish交换接口。
- 更新三个受影响的旧JUnit调用，删除旧质心方向API，没有兼容重载。没有新增Mixin、网络字段或Embeddium接口。

## Decisions

- 圆形范围作用于混合系数，不截断热量传播。材料导热/潜热、原水地表tick、两层材料限制、间接通风阻力和独立静态辐射继续沿用现有实现。
- source→compiler用一个标准Consumer传递编译期scratch；拓扑编译器不直接依赖Minecraft源绑定类，不建立第二套源空间索引。
- 预算不足时保留待更新系数任务，合法旧几何可能短暂使用原G；既有几何失效规则仍负责停用失效通路。
- Documentation impact: 更新thermal runtime的方向、混合、滞回、材料邻居保留和源队列行为；更新heat-production中的开关/局部混合说明；主计划同步完成结果，子计划标completed，单0.5候选明确归入历史。

## Validation

- `compileJava compileGameTestJava`通过。第一轮96/96 GameTests通过；追加多方向接触与稳态能量/成本检查后，最终完整98/98 GameTests通过。
- 新增8项回归：负坐标/部分面覆盖及重叠、质心偏移/端点顺序、同节点对三方向、源启停/移动/零功率/正功率更新、预算拒绝后续算、冷材料跨Page持续请求与释放、滞回及最新hot位图、稳定供热能量与无拓扑重建。
- 源-only更新验证节点数量、slot身份与H不变；重叠源移除一个仍增强，最后一个关闭恢复原G；混合更新队列不因预算拒绝丢失。
- 16源、2176节点的solver微基准：1024次预热、128次测量，最后一次完整运行p50=141.3 μs、p95=202.3 μs、测量区间分配0 B。只计solver，预热/测量期间不额外推进源账本，不代表持续供热完整cut或多人MSPT。
- 浮力单元测试通过4/4：使用临时init脚本只编译运行`BuoyancyConductanceTest`。普通JUnit目录仍有既有未迁移几何夹具引用，本轮未恢复其旧生产类或宣称全JUnit通过。
- 全量过程中发现两个既有夹具准备不足：空IR窗口可能与其他测试保存状态重叠；相变churn夹具没有显式清空完整Brick和Air出口。空窗口改用单独加载的位置，相变夹具显式初始化64格，并把“请求序号非零”加强为“序号存在且潜热能量完成”。相变测试曾单独运行通过，修正准备条件后的完整98项通过。没有靠增加超时或取消断言通过。
- 最终命令：`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`；目标JUnit为`test --tests '*BuoyancyConductanceTest'`加临时source include脚本。`git diff --check`通过。
- 保留必要回归测试；清理临时AirPhaseProbe源码/编译物、init脚本、验证日志及生成的run-gametest/world。用户实际run/saves与run/logs未操作；没有Python文件或源码重写脚本。

## Remaining

- 尚无大型在线多人、完整retained heap/MSPT或用户存档的实际客户端验证；不承诺所有房型、风况、运行时长都保持某一误差上限或全局最优。
