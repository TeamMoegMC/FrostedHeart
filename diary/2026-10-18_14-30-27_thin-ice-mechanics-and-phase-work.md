# 薄冰踩踏、冲击与相变预算原型

- Time: `2026-10-18 14:30:27 +08:00`
- Author: `OpenAI Codex；主实现与验证代理`
- Status: `completed`
- Scope: `thin_ice / layered_thin_ice`、阶段模型、GameTest、薄冰文档与讨论；不包含冰锥或世界生成

## Completed

- 实现潜行/站立/走路/跑步 N=96/24/12/6、每 tick 概率 1/N、成功后冷却 N ticks；同块同 tick 合并普通接触，不叠加人数。
- 实现起跳 1 格冲击、落地高度一半加小数概率的多级冲击，绕过冷却并在冰仍存在时重设 24 ticks 冷却。LivingTickEvent 补齐静止服务端实体；LivingFallEvent 补齐单层 legacy 落地采样。
- 分层碰撞与可见高度一致，厚度每两层增加一次承受次数，堆叠损伤比例向上取整。两种薄冰机械破碎六面邻水变水源，否则空气，不掉落、不连锁。
- 实现温度档位热裂与低温邻水逐级修复，使用实际编译相变预算的 1/4：95000 J / 90800 J。稀疏 SavedData 保存部分工作进度，卸载暂停，只有随机刻激活的有效位置每秒更新。
- 保留五种随机冰面，叠加 vanilla destroy_stage 顶面纹理；1/2 级音量 0.15、3 级 0.9。生成资源与默认 crack=0 的相变数据序列化更新一并保留。
- 薄冰测试 namespace `frostedheart_thin_ice` 加入 build.gradle 默认列表。平台、依赖与全局 Gradle 配置未改。
- 文档影响：更新 [薄冰实现说明](../docs/climate/thin-ice.md) 和气候 README；将 [交接计划](../plans/2026-10-16_12-00-10_thin-ice-cracking-handoff.md) 标为原型完成；向 [讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md) 追加最终规则与实际验证反馈。

## Decisions

- 裂纹使用薄冰专属玩法相变工作进度，与材料本体 H 分开；采用现有休眠热交换速率计算工作率，不扩展 thermal worker 的多分支材料相态。预算为 1/4，不承诺相同环境下实际耗时严格为 1/4。
- 保留现有完整薄冰 -20°C 变普通冰、9°C 融化成水及分层薄冰 9°C 融化为空气的材料相变规则。这里只为裂纹加热/修复增加预算进度。
- 3°C、6°C 分别进入第二/第三温暖档，0°C 没有净热裂，-5°C 没有净修复功率，9°C 及以上交给原融化路径。
- 分阶段声音复用现有 ICE_CRACKING；最终破碎使用标准 block break event。没有新图片、音频素材、冰锥或通用框架。
- Companion TheWinterRescue 仅只读检索标识符与用途，未找到 AGENTS.md，未修改该仓库；FH 注册 ID 保持兼容。

## Validation

- Java 17：`compileGameTestJava` 通过；`compileGameTestJava runData --offline --no-daemon --console=plain` 通过。日志：`build/thin-ice-phase-data.log`。
- 生成模型状态检查：完整薄冰 4 个状态与分层全部 8×4 个可表示状态均能解析模型引用。生成器保留随机基面，阶段纹理引用通过 ExistingFileHelper 验证。
- `runGameTestServer -PgameTestNamespaces=frostedheart --offline --no-daemon --console=plain`：107/107 required 通过，包含当时的 4 个新薄冰用例。日志：`build/thin-ice-final-gametest.log`。
- 单层修正后的最终专项命令：`runGameTestServer -PgameTestNamespaces=frostedheart_thin_ice '-Dnet.minecraftforge.gradle.check.certs=false' --offline --no-daemon --console=plain`：5/5 required 通过。覆盖冷却不自动推进、冲击绕过冷却、六面邻水、全部厚度承受次数与形状、堆叠比例、随机冲击小数、温度边界、编译能量预算、进度存档回读、邻水修复和单层真实碰撞落地路径。日志：`build/thin-ice-verified-gametest.log`。
- 过程限制如实保留：首轮 namespace 筛选为 0 用例，不计为通过；首次下落实体夹具未移动，修正为 Entity.move 驱动实际碰撞/fallOn 路径后通过。离线构建曾卡在 ForgeGradle 初始化联网探测，最终仅用命令行官方开关跳过该探测，没有持久修改网络或证书设置。
- `git diff --check` 通过。伴随整合包没有本轮改动。

## Remaining

- 尚无客户端人工试玩或音效试听；裂纹透明排序、实际动作节奏、多人体验和大量热裂冰面的性能待实测。
- 当前裂纹热交换是玩法工作预算近似，未使用活动材料节点的实际逐帧热流；热学更深入的耦合不属于本原型。
