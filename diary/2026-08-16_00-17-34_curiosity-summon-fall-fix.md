# Curiosity Boss：召唤后坠入虚空根因修复（Entity.load 无条件重置 noGravity）

- Time: `$(date "+%Y-%m-%d %H:%M:%S %z")`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed（修复+编译通过；待用户复测）`
- Scope: `content/world/entities/CuriosityEntity.java`

## Completed

- 症状：/summon 召唤后约 5s 触发 RISING，随后 BGM/Boss 条很快消失、实体消失。日志显示实体在
  DORMANT 阶段持续坠落（beginRising 时 y=-116，surfaceY=107），outOfWorld 伤害刷屏，最终 KILLED。
- 根因（栈追踪实证）：1.20.1 `Entity.load` 对 NBT 缺键无条件执行
  `setNoGravity(getBoolean("NoGravity"))`（getBoolean 缺键返回 false，同理 `setInvisible`）。
  `/summon` 流程 `EntityType.create → entity.load(空标签)` 把构造器里设好的 noGravity=true/invisible=true
  抹成 false → 实体受重力坠出世界。自然生成/生成蛋路径不经过 load()，所以此前未暴露。
- 修复：`readAdditionalSaveData`（load 末尾调用）按阶段重新断言：非 EXPOSED 阶段强制
  noPhysics=true + noGravity=true + invisible=true；EXPOSED 阶段反之。
- 顺带修复：`hurt()` 放行 outOfWorld 伤害——此前被拦导致坠入虚空的旧实体永不死亡（幽灵实体
  残留刷日志），现在会正常死亡自清理。
- 移除全部临时诊断日志与 FHMain import。

## Decisions

- 用 readAdditionalSaveData 重新断言而非 override load()：对 /summon 空标签与区块存档加载两条路径
  统一生效；存档标签里已有的 NoGravity/Invisible 键与阶段断言不冲突。

## Validation

- `./gradlew compileJava` → BUILD SUCCESSFUL。
- 栈追踪逐帧核对：Entity.load:1872 无条件 setNoGravity(getBoolean("NoGravity")) 为唯一重置源；
  全库 setNoGravity 调用方仅 FlyingMoveControl/Bucketable/Vex，均不涉及本实体。

## Remaining

- 用户复测：召唤后实体应潜伏在 surfaceY-1 不再坠落，RISING 正常开始并进入 HUNT。
- Boss 战斗全流程联测（冷场体感/追踪细雪/迷宫/火烧核心/掉落/RESET 还原/音乐）。
- 发布前 spawnWeight 调低；团队确认 FHConfig 重复 define 键的键名归属。
