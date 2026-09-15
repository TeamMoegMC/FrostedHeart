# 延迟cut按实际时间推进换热

- Time: `2026-09-16 00:00:26 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `ThermalDimensionEngine时间步、合并cut日志及必要回归`

## Completed

- `executeTransport`使用`elapsedTicks / 20.0`。源能量和传热使用同一段游戏时间，普通20 tick仍为精确1秒参数，保留既有编译系数快路径。
- 每cut仍只有一次solver遍历，不引入追赶队列、子步循环、临时温度数组或兼容执行路径。零时间和未改变的休眠状态仍跳过传热。
- 原timeDegraded改为coalesced语义，保留合并cut不立即进入休眠的行为。仅在合并cut且DEBUG日志启用时输出维度代次、序号、elapsedTicks与transportSeconds。
- 新增`ThermalTimingGameTests`，复用现有测试Fixture，覆盖延迟供热、普通Air/材料交换、FarField以及潜热等待ACK。

## Decisions

- 选择现有内核的一次实际dt大步，保持工作量有界。没有修改源事件积分、拓扑提交顺序、材料潜热、静态辐射、1/0.5滞回或近源4倍规则。
- 大步不是历史重放：源先结算、再按最终拓扑交换；中途几何和持续供热的精确交错仍为近似。不能把本次两节点误差当作任意网络保证。
- Documentation impact: 更新runtime及热源文档的时间语义和诊断信息，复查报告/主计划记录已修复状态；历史修复前数据保留。

## Validation

- 完整`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false` BUILD SUCCESSFUL，102/102通过（原98项加4项时间回归）。
- 相同600游戏秒、同样3840000 J Air输入：20/40/100/200 tick间隔的源温度分别为-13.97903/-13.99944/-14.05903/-14.15281°C。200 tick对正常cut的差异约0.17379°C，修复前约8.26254°C。
- 空气和普通材料的10秒交换与独立指数衰减公式一致，总H守恒；零时间cut不产生交换。FarField按完整10秒冷却。
- 相变材料大步准确停在latent target H并产生一个请求；继续等待ACK时不吸收越界能量，总H守恒。
- 实际debug.log确认200 tick输出transportSeconds=10.0，40 tick输出2.0；普通cut无该诊断输出。
- `git diff --check`通过；未重复运行不相关的目标浮力JUnit。保留必要回归，没有新增Python或临时源码脚本。本次验证日志和生成测试世界的删除被工具策略拒绝，尚未清理。

## Remaining

- 清理被工具策略阻止：`run-gametest/world`、`build/thermal-timing-fix.log`、`run-gametest/logs/debug.log`及`run-gametest/logs/latest.log`仍保留。
- 完整在线多人压力、极大延迟和复杂几何在合并时间步中的精度仍未做全量实测；不声称完全等价逐秒模拟。
