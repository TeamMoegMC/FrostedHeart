# Curiosity Boss 实现（M1–M5 全量代码）

- Time: `$(date "+%Y-%m-%d %H:%M:%S %z")`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed（代码完成并编译通过；未做游戏内联测）`
- Scope: `content/world/entities/*`；`bootstrap/common/FHEntityTypes`；`events/FHClientEventsMod`；`content/world/FHBiomeModifiers`；`infrastructure/config/FHConfig`

## Completed

- 按 docs/curiosity-boss-design.md 实现全部阶段：DORMANT（匍匐+逗留触发）→ RISING → HUNT（地下追踪
  60s+地表隆起 mound+雪雾+细雪斑块滚动窗口）→ MAZE（确定性递归回溯雪墙迷宫 43×43，波次升起，
  入口=玩家所在格+边界开口，核心=最远格）→ EXPOSED（仅火伤，点燃后 coreBurnTicks 燃尽消散）→
  DISPERSED（矿霜球掉落+经验+清场）/ BURROW（round+1，冷场阶梯更新）。
- 温度联动：负值 `SphereHeatArea` 冷场，tier1/tier2/每轮-15/上限-75，RESET/DISPERSED/卸载时移除。
- 逃脱与死亡：离场 10s 或战斗范围内所有玩家死亡 → 立即 RESET 回 DORMANT 并完整恢复方块。
- 音乐：客户端循环 `the_fall_of_arcana`（`FHSoundEvents.TFOA`），`AbstractSoundInstance`+
  `TickableSoundInstance`，isStopped 随实体移除/阶段结束自动停。
- 掉落：`FHTags.Items.CONDENSED_BALLS` 标签池随机 24 个矿霜球 + 经验 50。
- 配置：`FHConfig.SERVER.CURIOSITY` 全参数节（含 `spawnBiomes` 群系 ID 列表、测试期 `spawnWeight=10`）。
- 生成：`FHBiomeModifiers` 权重进配置；`canSpawn` 群系白名单+3×3 平整度+雪面+距玩家>32。

## Decisions

- 迷宫几何：11 格 × 步长 4，占地 43×43（=11×4−1 含外墙）；入口最近边界开 3 格缺口，保证迷宫期可逃脱。
- 追踪速度默认 0.24 b/t（玩家疾跑 ≈0.28），每轮 +0.03，上限 0.45。
- 方块安全：细雪/雪墙仅替换空气或雪层，快照（NbtUtils.writeBlockState）+指纹校验恢复；细雪滚动
  窗口上限 40。
- 1.20.1 API 事实（踩坑记录）：`Level` 无 `getNearestPlayer`/`holderLookup`（用
  `getEntities(EntityTypeTest,...)` 与 `registryAccess()`+`lookup(Registries.BLOCK)`）；
  `SoundInstance` 是接口（用 `AbstractSoundInstance`）；`getHeight` 只收 int 坐标；
  `renderSingleBlock` 需 `MultiBufferSource`+`ModelData`。

## Validation

- `./gradlew compileJava` → BUILD SUCCESSFUL（多次，含全部新类 CuriosityPhase/Arena/Maze/
  MoundEntity/MoundRenderer/ClientEffects/Entity/Model/Renderer）。
- 全部关键原版签名经 javap 对 1.20.1 Parchment recomp jar 核对。

## Remaining

- 游戏内联测（runClient）：生成、逗留触发、冷场体感、迷宫升起、火烧核心、掉落、RESET 还原、
  多玩家死亡规则、Boss 条与音乐。
- 平衡调参（速度/冷场/时长）；发布前 `spawnWeight` 调低。
- 音效均为原版复用（SNOW_HIT/SNOW_STEP/FIRE_EXTINGUISH/WIND），专属音效素材待补。
