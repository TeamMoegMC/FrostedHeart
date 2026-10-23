# 冰锥振动来源与独立概率

- Time: `2026-10-23 12:34:33 +08:00`
- Author: `OpenAI Codex；实现与验证代理`
- Status: `completed`
- Scope: `IcicleEvents`、`ServerLevelMixin_IcicleVibrations`、冰锥／冰溜子声学触发

## Completed

- 将振动源参数改为 `Vibration(radius, crackChance, shatterChance)`。爆炸 32 格／10%／100%；敲钟及铁砧 12 格／1%／50%；冰块破碎 16 格／1%／50%；石头／矿石破碎 8 格／0.5%／50%；烟花 24 格／2%／100%。均为未衰减中心概率。
- 保留 `p=p0*max(0,1-d/R)` 的三维距离线性衰减，边界为零；冰溜子使用完整段概率直接坠落，候选原预警状态快照避免同事件再次抽立即破碎。
- 爆炸／敲钟复用原入口；新增服务端 mixin 转发 `levelEvent` 2001 的原方块状态与 1029/1030/1031 铁砧效果，以及火箭 `broadcastEntityEvent` byte=17。无需客户端声音监听或每 tick 声学扫描。
- 冰块按 `minecraft:ice` 标签识别，石质方块按 `STONE_SOUNDS` 或 `forge:ores` 识别；完整分类见[说明](../docs/climate/icicle.md)。明确跳过冰锥与冰溜子自身破碎效果，避免递归。烟花仅有 `Fireworks.Explosions` 时触发，每次爆裂一轮，不把无星助推火箭或各个客户端烟花声音重复计入。
- 文档影响：同步[冰锥](../docs/climate/icicle.md)、[冰溜子](../docs/climate/eave-icicles.md)，向原[讨论](../discussion/2026-10-17_23-27-52_ice-caves-and-thin-ice.md)追加用户定案；既有两组 GameTest 改用事件参数对象，新增六项 `IcicleVibrationGameTests`。

## Decisions

- 烟花半径由用户补充确认为 24 格；铁砧包含使用、落地、损坏及挖掉时的实际效果。普通玻璃与木头破碎不视为冰／石头。
- 所有改动限于悬冰玩法及其事件适配，不修改温度内部实现或整合包数据；不增加声源冷却、遮挡模拟或新配置系统。

## Validation

- 本地 Forge 1.20.1 / 47.3.0 字节码核对：铁砧的 level-event ID、方块破坏状态数据、烟花 byte=17 与非空爆裂数据路径。
- Java 与 GameTest 源码编译通过；Java 17 `runGameTestServer -PgameTestNamespaces=frostedheart_icicle_vibration,frostedheart_icicle --offline --no-daemon --max-workers=2` 在 2026-10-23 12:36:57 全部 **25/25 required tests 通过**，Gradle `BUILD SUCCESSFUL in 5m 40s`，退出码 0，日志为 `build/icicle-vibration-profiles-gametest.log`。
- 包含新增六项事件／衰减验证及原有 19 项冰锥、冰溜子、滴水与相变回归。验证实际服务端 level-event 和烟花爆裂消息均经过新 mixin，区分冰／石头／矿石与玻璃／木头／悬冰，验证无星火箭不触发、20 格外有星烟花可以触发、中心最大概率和边界归零、冰溜子无预警掉落。
- `git diff --check` 通过。16×5 旧爆发场景观测振动 7.1755 ms、60 步手动物理 20.0653 ms；仅为受控服务端夹具结果，不代表连续采矿、连续爆炸或完整整合包性能。
- 未打包 JAR、暂存、提交或部署；保留其他对话和用户的已有脏文件。

## Remaining

- 客户端操作体验、采矿／连续爆炸负载与整合包平衡需后续内测。
