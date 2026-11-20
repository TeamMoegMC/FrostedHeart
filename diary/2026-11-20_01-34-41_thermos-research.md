# 基础隔热容器研究改版

- Time: `2026-11-20 01:34:41 +08:00`
- Author: Codex（OpenAI，实现与验证）
- Status: completed
- Scope: FrostedHeart 饮用事件与旧研究领奖，以及 TheWinterRescue 保温杯研究、配方和 T0/T1 任务。

## Completed

- FrostedHeart 在饮用完成后发出 `frostedheart:drank_cold_water`，并让已被新项目接替的旧档案继续领取未领取的实物奖励。保温杯一级旧档沿用原领奖效果回执。
- TheWinterRescue 新增便携热饮构想、木杯联络、基础隔热容器项目和保温杯设计。普通杯使用固定配方 ID，实验台产出一只空杯，绘图台完成后才授予制造和去染色配方。二级读取旧完成或新项目的有效完成状态。
- 更新[玩家流程](../docs/knowledge/thermos.md)、知识系统入口和旧研究处置表。与手炉对话按配方专属 ID、任务 ID 和翻译键协调，共享修改范围见留言板 CUP002。

## Decisions

- 冷水包括已有容器中的原水和净水。原水可以直接接取，容器允许灌装的流体沿用原标签。基准 5%，任意一条保温杯观察入档后固定 50%。
- 实验用一张铜板、一块木板、200 tick，成功后在台面领取。T1 任务按普通杯物品 ID 匹配，实验产物的空杯可满足物品条件。

## Validation

- Forge 47.3.0 的 Java 17 编译通过，`frostedresearch_thermos,frostedresearch_m3b` 共 7 项必需 GameTest 通过。正式知识数据加载，三种容器的原水完成事件、打断和热饮排除、5% 和 50%、主动联络、实验发杯、两个配方权限、二级父项和旧赠杯一次性领奖均完成服务端检查。日志位于 `build/thermos-verification/isolated.log`。
- TWR 80 份旧研究定义预检通过。Node 执行配方脚本并捕获唯一普通杯配方的 ID 和原材料，输出等价原版配方供 Forge 验证。中英文引用检查和两个仓库的 `git diff --check` 通过。
- 净水不在当前通用饮料流体标签，普通灌装未走通。GameTest 用原水检查三种容器，用枸杞茶检查热饮排除。净水触发分支仅从源码核对。

## Remaining

- 整包客户端、旧奖励按钮显示及 FTB 任务实际完成待实测。未打包生产 JAR，未同步 Prism。
