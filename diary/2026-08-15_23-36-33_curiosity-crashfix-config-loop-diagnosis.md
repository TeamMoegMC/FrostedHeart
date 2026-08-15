# Curiosity Boss：雪原卡死根因修复 + 服务器配置循环诊断（进行中）

- Time: `$(date "+%Y-%m-%d %H:%M:%S %z")`
- Author: `DeepSeek v4 (deepseek-v4-pro); coding agent for TeamMoeg`
- Status: `partial（卡死已修复；config 循环已定位机制并留诊断代码，等游戏内数据）`
- Scope: `content/world/entities/CuriosityEntity`；`infrastructure/command/DebugCommand`（临时诊断）

## Completed

- 雪原卡死根因：`CuriosityEntity.canSpawn` 在区块生成期间（`WorldGenRegion`）查询 pos±1 相邻区块的
  `getHeight`，越界抛 "We are asking a region for a chunk out of bound"，区块生成任务反复失败 → 地形
  不刷新、游戏假死。
- 修复：平整度检查前用 `level.hasChunk(cx+dx, cz+dz)` 守卫（WorldGenRegion.hasChunk 为纯边界检查，
  不抛异常；ServerLevel 上即区块已加载检查）；玩家距离检查在 `level instanceof WorldGenRegion` 时跳过。
  `./gradlew compileJava` 通过。
- 服务器配置循环（"Configuration file frostedheart-server.toml is not correct. Correcting" 每 2s 一次、
  .bak 内容恒定）：确认所有历史日志无此现象 → 非历史遗留；与 Boss 代码无关（无头服务器 + 新世界同样复现）。
  机制：nightconfig `ConfigSpec.isCorrect` 对 spec 缺失键/非法值/注释不匹配均判 false，Forge 的
  correct() 重写后文件字节不变 → 死循环。FHConfig.java 中疑似源：`environmentTempMinTicks` 等 14 个键
  被 define 两次（Town 相关一批 + Temperature 一个），以及 Town 动态键（attribute(...) 生成）需核对。

## Decisions

- 按用户要求不再自行修 config 循环；保留两处临时诊断，由用户在游戏内获取数据后移除：
  1. `DebugCommand.onServerStarted`（ServerStartedEvent）自动把 `[config_check]` 全部修正项
     （action + path + old→new）写入 latest.log；
  2. `/fh debug config_check` 命令在聊天栏输出同样内容。
- 无头服务器运行方案被用户拒绝，已停止相关后台任务。

## Validation

- `./gradlew compileJava` → BUILD SUCCESSFUL（含 canSpawn 修复与诊断代码）。
- WorldGenRegion.hasChunk 字节码确认：纯 firstPos/lastPos 边界判断，无异常路径。
- 无头服务器复现 config 循环（world 存档同样每 2s "Correcting"）。

## Remaining

- 用户游戏内运行后回传 latest.log 中 `[config_check]` 行 → 定位具体失败键 → 修复（预计为重复
  define 键或注释不匹配）→ 移除临时诊断代码。
- Boss 战斗全流程联测（生成/冷场/迷宫/火烧/掉落/RESET/音乐）。
- 发布前 `spawnWeight` 调低。
