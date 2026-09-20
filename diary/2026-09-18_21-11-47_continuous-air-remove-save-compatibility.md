# 取消连续空气计划中的旧存档兼容

- Time: `2026-09-18 21:11:47 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `连续空气R4工程计划的持久化范围；未修改生产代码或实际存档`

## Completed

- 按用户明确要求修订[工程计划](../plans/2026-09-18_20-53-28_continuous-air-r4-cpu-engineering.md)，取消v4空气/材料导入、双root、旧空气预览双写、新旧格式优先级及旧后端存档往返验收。
- 新计划采用单个 `FrostedHeartThermal` root版本5，只读写新版空气和材料；保留新格式自身的chunk卸载、dormant、重启和热历史恢复。
- 旧热学记录按缺失处理，从当前世界状态和自然温度初始化thermal派生状态；不影响世界方块或其他游戏数据。

## Decisions

- 旧后端只承担开发基线比较，不因存档兼容保留兼容期。
- 本条更新此前日记的兼容设计决定；原日记保留历史，不改写。
- Documentation impact: 只修订计划；生产行为尚未改变，活文档无需更新。

## Validation

- 检查计划中所有旧格式、兼容、预览、导入及存档往返相关条目，同步修改阶段交付和测试矩阵。
- 文档检查；本次未执行编译或游戏测试。

## Remaining

- 按修订后的P0–P6实施，旧存档兼容不再属于交付范围。
