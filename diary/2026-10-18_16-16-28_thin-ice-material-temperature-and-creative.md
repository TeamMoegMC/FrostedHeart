# 薄冰统一相变温度与创造模式交互

- Time: `2026-10-18 16:16:28 +08:00`
- Author: `OpenAI Codex；主实现与验证代理`
- Status: `completed`
- Scope: `薄冰热裂/回冻温度、创造模式踩踏与冲击、单层落地目标、GameTest 与文档`

## Completed

- `MinecraftPhaseController.temperature` 优先读取活动/休眠材料本体；真正无材料记录时与原随机相变共用原位 `naturalBlock + gameplayPassiveEnvironment` 回退。未发布的材料归属或不匹配记录暂停读取，不退到空气。
- `ThinIcePhaseData` 的激活、温度档位和每秒预算推进均改用上述入口，移除冰面上方空气温度查询，保留现有进度存档。
- `ThinIceBlock.canCrack` 允许创造玩家；旁观玩家仍不参与物理踩踏。创造模式也阻止脚下裂纹回冻。
- `EntityMixin_ThinIceLanding` 更正 `Entity.checkFallDamage` 内单层薄冰的 `Block.fallOn` 目标，代替会被创造免伤跳过的 `LivingFallEvent` 补丁。只调用一次真实落地回调，防止同时冲击下方冰。
- 新增本体/环境温度相反、陈旧材料记录、创造起跳/落地与上下叠冰断言。复用现有真实材料 checkpoint 夹具。
- 文档影响：更新 [薄冰说明](../docs/climate/thin-ice.md)、气候 README 与 [数据生命周期](../docs/climate/data-lifecycle-and-integration.md)，向 [讨论文档](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md) 追加确认规则和验证结果。

## Decisions

- 用户明确要求热裂/修复与相变统一温度，并允许创造模式交互；此前使用上方环境空气的实现说明已由本轮修正替代。
- 继续使用每级热裂/修复为对应完整相变预算的 1/4。部分裂纹预算 W 仍保存在薄冰 SavedData，不把它描述为材料本体 H 的真实热流或新增 worker 多分支相态。
- 落地更正位于摔落伤害判断之前，因此创造免伤不会吞掉冲击；后续伤害处理仍走原回调。

## Validation

- Java 17：`compileGameTestJava '-Dnet.minecraftforge.gradle.check.certs=false' --offline --no-daemon --console=plain` 通过；日志 `build/thin-ice-temperature-compile.log`。
- `runGameTestServer '-PgameTestNamespaces=frostedheart,frostedheart_thin_ice' '-Dnet.minecraftforge.gradle.check.certs=false' --offline --no-daemon --console=plain`：110/110 required 通过；日志 `build/thin-ice-temperature-gametest.log`。这是最后一次单层落地目标细化之前的回归结果。
- 最终落地更正后复跑 `runGameTestServer -PgameTestNamespaces=frostedheart_thin_ice '-Dnet.minecraftforge.gradle.check.certs=false' --offline --no-daemon --console=plain`：7/7 required 通过，BUILD SUCCESSFUL；日志 `build/thin-ice-temperature-final-gametest.log`。
- 温度反差用例验证：暖本体/冷环境会热裂且不回冻；冷本体/暖环境不热裂且可邻水回冻；记录不匹配时与原相变同样等待。
- 创造用例验证：起跳可开裂、完整薄冰落地绕过冷却、单层落地可破碎且下方薄冰保持完好，旁观模式仍排除。

## Remaining

- 尚无客户端人工试玩、音效试听或大片薄冰性能实测；讨论中明确记录为待反馈。
