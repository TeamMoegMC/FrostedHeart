# 世界温度重载、热源启动与自然刷新

- Time: `2026-09-09 21:05:45 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `WorldTemperature recipe caches / physical runtime bootstrap / natural refresh`

## Completed

- 配方表重建后清除维度/群系结果缓存，空配方集合也清除旧表。
- 有效机器输出及篝火点燃/区块加载启动同一runtime；启动和重载检查已加载篝火，沿用原发现队列。自然刷新从实际采样tick安排下一次200-tick截止时间。
- 更新[现有计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)与climate下现有world-climate、runtime、data-lifecycle、heat-production文档，纠正过时模型及显示描述。

## Decisions

- 无新轮询、长期缓存或第二套服务。首次点燃在现有LevelChunk mixin增加状态回调；有效热源驱动必要runtime驻留。海拔曲线与旧城镇模拟器公式保留。

## Validation

- Java17 `runGameTestServer --offline --no-daemon --console=plain -I run-gametest/infrared-test-scope.gradle`：最终29项全部通过，BUILD SUCCESSFUL；日志`run-gametest/world-temperature-lifecycle-final.log`。
- 真实成型T1/T2、喷泉、散热器无人启动及重载恢复；篝火首次点燃、区块加载待恢复NBT及重载；原生配方重建、空表移除、实际Page队列积压间隔均通过。
- 未运行JUnit；旧的未接通掉落物接口测试类仍按用户既定范围排除。git diff --check通过。

## Remaining

- 本轮不是整包完整测试或性能基准；客户端实机验证仍未覆盖。
