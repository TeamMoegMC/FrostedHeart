# 准备研究改版独立整合包测试实例

- Time: `2026-11-12 14:03:16 +08:00`
- Author: `Codex（OpenAI GPT-6 Astra），主协调`
- Status: `partial`
- Scope: 本地 Prism 测试目录与正式包数据准备；尚未运行客户端。

## Completed

- 从现有 `The-Winter-Rescue-Development` 的运行文件建立独立实例 `D:\games\minecraft\prismlauncher\instances\The-Winter-Rescue-Research-Playtest-20261112`，显示名为 `The Winter Rescue - Research Playtest`。
- 复制模组、配置和资源目录；没有复制玩家 saves。新目录实际含60个模组JAR。
- 使用TWR工作区的完整kubejs目录（57个服务端脚本）及81项研究配置；正式研究文件名集合比对无差异。原实例和玩家世界未修改。
- 生成路径记录 `build/research-playtest-instance-path.txt`，供后续阶段直接定位；测试目录不纳入Git。

## Decisions

- 只准备一个可重复使用的测试实例，后续更新当前FH产物与相关正式数据，不为每次检查重建整份实例。
- 当前副本中的FH JAR来自原实例，尚不是当前改版源码构建。后续首次启动前必须换入经过阶段验证的当前产物。

## Validation

- 文件复制完成，目标实例没有saves目录；实际目录计数和研究配置文件名集合已检查。
- 尚未构建或部署当前FH JAR，未启动游戏，不能据此认定正式包启动、材料可达性或生存流程通过。

## Remaining

- M3阶段换入当前FH JAR及新增正式内容，启动新测试世界检查实验与生产链。后续定义数量以届时实际正式目录为准。
- 其他环境信息见[验收环境说明](../plans/2026-11-12_13-05-54_research-playtest-environment.md)。
