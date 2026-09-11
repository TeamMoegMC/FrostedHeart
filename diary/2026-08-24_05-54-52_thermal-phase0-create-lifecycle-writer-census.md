# Thermal Phase 0 Create、区块生命周期与 writer census

- Time: `2026-08-24 05:54:52 +08:00`
- Author: `Codex; OpenAI main engineering agent`; assisted by `phase0_writer_census` (`gpt-5.6-sol`, `ultra`)
- Status: `partial`
- Scope: `content.climate.thermal.phase0`、初版 `content.climate.thermal.geometry`、Phase 0 GameTest、温度架构计划与生命周期文档

## Completed

- 用真实 region ticket 验证 loaded chunk 的 load -> unload -> reload，覆盖旧 section identity、lifecycle generation、stale write 和 publication rejection。
- 用真实 Create mechanical bearing 验证 assemble 的 `stone -> air`、移动期零 world geometry delta、disassemble 目标位置的 `air -> stone`。
- 删除 Create movement adapter/Mixin/AABB exclusion 方案；保留与 Create 无关的通用 dynamic exclusion prototype。
- 增加 Vanilla、Forge、Create 和 Frosted Heart block/biome writer census，以及同 tick normalization、duplicate/discontinuity、mapped/unmapped 和 adapter-gap 合同。
- 增加 Phase 0b benchmark evidence envelope；没有完整 artifact 和冻结环境时，数值不能标为 measured pass/fail。
- 实现 Phase A 首个 pure-Java conservative air raster/component 与 `4^3` Brick compiler slice。
- 更新权威计划和 `docs/climate/data-lifecycle-and-integration.md`；玩家可见行为、持久化、配置和网络未改变，其他 living docs 无需更新。

## Decisions

- Create contraption 只在静态 `Level` world mutations 上参与热几何；移动实体固定按空气，不携带 V1 热状态。
- 五参数 `LevelChunkSection#setBlockState(..., boolean)` 仍是正常 Vanilla/Forge/Create block writer 的共同 capture point；四参数 overload 不重复注入。
- `FastNoiseEngine` raw block data 写入只能暂由 active-section fingerprint 恢复，不能宣称 primary capture。
- `FastNoiseEngine` raw biome data replacement 和 `DebugCommand restore_backup` whole-section identity replacement 是明确的 adapter gaps；现有 block fingerprint 不能覆盖它们。
- Phase 0 和 Phase A 均保持 `partial` / `in-progress`，legacy 温度系统继续是唯一 gameplay authority。

## Validation

- Java 17 `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --no-daemon --console=plain`: `60/60` passed。
- Java 17 `./gradlew.bat compileJava --no-daemon --console=plain`: passed。
- Java 17 `./gradlew.bat runGameTestServer --no-daemon --console=plain`: `8/8` required passed，其中 Phase 0a `7/7`。
- `git diff --check`: passed；仅报告工作区既有 LF/CRLF 提示。

## Remaining

- 完成 enabled-mod writer census，并给 source-inspected writer 增加执行级证据。
- 为 whole-section replacement 实现 owner rebind + full resnapshot，为 raw biome replacement 实现独立 revision/resnapshot adapter。
- 测量 active-section fingerprint 的覆盖率与成本，完成 Phase 0b JFR/JMH、retained heap、真实 workload threshold 和候选对照。
- 继续 Phase A Forge `BlockState + FluidState` resolver census、真实 shape adapter、完整 fixtures 与性能证据。
