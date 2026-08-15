# Curiosity Boss：打火石右键点燃 + 掉落物火焰免疫

- Time: `$(date "+%Y-%m-%d %H:%M:%S %z")`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed（编译通过；待用户复测掉落）`
- Scope: `content/world/entities/CuriosityEntity.java`；`docs/curiosity-boss-design.md` §6.4/§16-6

## Completed

- 用户初测全流程通过；反馈两个问题：打火石右键点不燃核心；岩浆击杀把掉落物烧没。
- 查证：原版 1.20.1 `FlintAndSteelItem`/`FireChargeItem` 只有 `useOn`（方块交互），没有
  `interactLivingEntity`——右键实体本来就不会点火。`Mob.interact` 是 final，改用
  `mobInteract(Player, Hand)` 钩子：EXPOSED 阶段用打火石/火焰弹右键 → setSecondsOnFire(5)，
  打火石耗耐久、火焰弹消耗 1 个，播放 FLINTANDSTEEL_USE 音效。
- 掉落物 `ItemEntity.setInvulnerable(true)`（`ItemEntity.fireImmune()` = isFireResistant ||
  Entity.fireImmune()=invulnerable）——岩浆/火矢击杀不再烧毁矿霜球。

## Validation

- `./gradlew compileJava` → BUILD SUCCESSFUL。
- javap 确认 Mob.interact 为 public final、mobInteract 为 protected 钩子。

## Remaining

- 用户复测：打火石右键点燃 → 3s 燃尽 → 24 个矿霜球 + 经验 50 掉落且不被火烧毁。
- 发布前 spawnWeight 调低；团队确认 FHConfig 重复 define 键键名归属。
