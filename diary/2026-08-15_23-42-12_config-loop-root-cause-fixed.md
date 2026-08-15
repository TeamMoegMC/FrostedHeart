# 服务器配置死循环根因修复：VAWT.vawtDurability Float 类型

- Time: `$(date "+%Y-%m-%d %H:%M:%S %z")`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `completed`
- Scope: `infrastructure/config/FHConfig.java`（VAWT 节）；`infrastructure/command/DebugCommand.java`（临时诊断已移除）

## Completed

- 用户游戏内回传：`isCorrect=false corrections=1: REPLACE VAWT.vawtDurability: 1.0 -> 1.0`。
- 根因：`vawtDurability` 是 `ConfigValue<Float>`（`define("vawtDurability", 1f)`）。TOML 无 float 类型，
  Forge 写入 1.0、读回 Double；`Float.class.isAssignableFrom(Double.class)` 恒为 false → 每次校验失败，
  "修正"又把 Float 1f 写回成同样的 1.0 → 文件字节不变 → watcher 每 2 秒死循环（"not correct. Correcting"）。
- 修复：`vawtDurability` 改为 `ConfigValue<Double>`（默认 1.0D）。该字段代码库内无任何使用方，改类型无副作用。
  已有存档的 `vawtDurability = 1.0` 直接通过新校验，无需重置配置。
- 移除全部临时诊断代码（ServerStartedEvent 处理 + /fh debug config_check + 新增 imports）。

## Decisions

- Forge 配置禁止 Float 值（TOML 往返变 Double）——已写入 FHConfig 注释备忘。
- 不顺手改 `environmentTempMinTicks` 等 14 个被 define 两次的键：不影响 isCorrect（不产生循环），
  但存在两个字段共享同一存储值的语义问题（如 envTempUpdateIntervalTicks 实际读到 2 而非默认 20），
  键名归属需团队确认，已列入 Remaining。

## Validation

- `./gradlew compileJava` → BUILD SUCCESSFUL。
- 逻辑推导与用户实测数据一致（corrections=1 且 REPLACE 前后值文本相同）。

## Remaining

- 用户重进游戏确认不再出现 "not correct. Correcting" 刷屏。
- 团队确认重复键（environmentTempMinTicks 及 Town 的 13 个键）的正确键名后另行修复。
- Boss 战斗全流程联测与调参；发布前 spawnWeight 调低。
