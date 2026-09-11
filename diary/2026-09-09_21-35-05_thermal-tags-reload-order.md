# Thermal tags重载与单人游戏配方同步

- Time: `2026-09-09 21:35:05 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `MinecraftThermalEvents / FHClientEvents / runtime reload`

## Completed

- 将重载后的篝火runtime恢复移到服务端TagsUpdatedEvent，避免提前按旧tags编译profiles。
- 集成客户端跳过共享静态配方表及温度缓存的重复重建；远程客户端正常同步。
- 更新[现有计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)及现有climate runtime、生命周期、热源文档。

## Decisions

- 复用事件和Minecraft单人游戏标识，无新持久状态、缓存、锁或调度器。

## Validation

- Java17 runGameTestServer加原范围脚本：30项全部通过，BUILD SUCCESSFUL。实际临时tag数据包经过完整reloadResources添加/移除，自动恢复runtime的材料分类正确变化并恢复；数据包已清理。
- 日志：run-gametest/world-temperature-tags-verification.log。git diff --check通过。未运行JUnit；含未接通掉落物接口的旧测试类仍排除。

## Remaining

- 客户端分支已编译及静态核对，集成客户端并发尚未实机验证。
