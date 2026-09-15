# 热架构复查与延迟cut时间对照

- Time: `2026-09-15 23:12:09 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `架构审查与最小复现；没有修改生产Java`

## Completed

- 核对热源索引/账本/混合更新、拓扑与两层材料接触、热标记/驻留、材料迁移、休眠投影、相变和读取路径。
- 本次未发现新的正常路径断链或为本轮空气修复残留的高度方向兼容接口。
- 确认已有过载策略：源按完整elapsed供能，但传热仅固定推进1秒；timeDegraded只抑制休眠，完成状态仍为COMPLETED。
- 生产engine最小两节点对照：每20/40/100/200 tick提交，均推进600游戏秒、增加3840000 J。热源温度依次-13.9790/-12.9581/-9.9372/-5.7165°C，证实时间策略会改变分布而不破坏总能量。
- 将该时间一致性问题列为后续优先项，区分源码缺口、已约定近似和真实负载验证不足。详细结论见[复查报告](../plans/2026-09-15_23-12-09_thermal-architecture-review.md)。

## Decisions

- 本轮是检查，不直接改过载策略。后续验证实际dt与有界分步，不扩大空间模型或无界追赶。
- 不把静态辐射、两层材料上限、原水地表抽样当成待修复问题。
- Documentation impact: runtime文档补充时间降级的真实可观察范围与实测后果；主计划增加复查入口。已完成的空气实现子计划保持原完成状态。

## Validation

- 临时namespace thermaltimingreview，`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false --init-script build/thermal-review/init.gradle` BUILD SUCCESSFUL。
- 四种间隔的能量断言均通过；无物质转换/外界热汇/拓扑改变。不是在线多人MSPT或实际延迟频率测试。
- 既有98项GameTests及4项目标JUnit为上轮基线，本次没有无依据重跑全量回归。
- 临时测试源码/编译类、Gradle脚本、日志和生成世界已清理；生产代码保持检查前状态，用户实际run/saves及run/logs未操作。

## Remaining

- 过载时间尺度策略与诊断输出改进；真实多人、捕获预算、GC、完整heap及网络联合验证。
