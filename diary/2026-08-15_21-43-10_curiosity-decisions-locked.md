# Curiosity Boss 设计稿：六项待确认决策落地

- Time: `2026-08-15 21:35 +08:00`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed`
- Scope: `docs/curiosity-boss-design.md`

## Completed

- 用户答复原 §16 全部 6 个待确认问题，设计稿已更新（§2/§3/§6.3/§6.4/§6.5/§7/§8/§10/§11/§12/§15/§16）。

## Decisions

- 掉落：不掉粉末（无材质）。掉落大量已注册「矿霜」凝缩矿石球，从 `FHTags.Items.CONDENSED_BALLS`
  标签池（9 种 `condensed_ball_*_ore`）随机抽取，总数 24（配置），+经验 50。
- 音乐：`the_fall_of_arcana.ogg`（OGA 开源，已注册 `FHSoundEvents.TFOA`，sounds.json 已有
  stream 条目）；若 `ServerBossEvent` 音乐为硬编码则自实现 Boss 音乐 SoundInstance。
- 生成：`spawnWeight` 配置化，测试期默认 10（发布前调低）；`spawnBiomes` 做成
  `List<String>` 群系 ID 配置，默认仅 `minecraft:snowy_plains`，包侧可增补自定义雪原群系。
- 迷宫：核心位置无任何提示。
- 死亡重置：战斗范围内所有玩家死亡才回 DORMANT，否则战斗继续。
- 点火：所有 `DamageTypeTags.IS_FIRE` 伤害源均有效。

## Validation

- 核对 `FHItems` 九种矿霜球注册与 `FHTags.Items.CONDENSED_BALLS` 标签存在；
  核对 `the_fall_of_arcana.ogg` 文件、sounds.json 条目与 `FHSoundEvents.TFOA` 注册。
- 全文检索确认设计稿无残留「待确认/nanite_powder/旧配置值」引用。

## Remaining

- 按文档 §14 里程碑 M1–M5 开始实现（下一轮工作）。
