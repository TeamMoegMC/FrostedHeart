# 城镇系统上线缺口检查

- Time: `2026-10-18 20:55:00 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation inspection`
- Status: `completed`
- Scope: `FH town source/tests/docs and read-only TWR recipes/quests`

## Completed

- 对照当前源码、整合包配方/任务、既有测试及验收记录，形成[上线缺口与验收建议](../plans/2026-10-18_20-50-00_town-release-readiness.md)。
- 确认货运站缺少已配置生存获取；住宅容量计床头和床尾、睡眠布局仅计床头；现行温度模型与历史离线模拟不一致；任务引导保留已移除猎人营地和旧机制说明。
- 区分实际缺陷、未验收项目与可延期内容；没有把已有招募、印章奖励或 P2P 误列为未实现。
- 文档影响：纠正 `docs/town/town-model.md` 与 `docs/climate/world-climate-and-temperature.md` 中的旧城镇温度接线，明确当前实际路径是 `RoomData.calculateTemperature` → `WorldTemperature.block`，并标明旧模拟结果的适用边界。没有修改生产行为。

## Decisions

- 首版建议范围为玩家城镇；T2、NPC 据点和非实体居民贸易按发布承诺单列，避免自动扩大上线范围。
- 床位结论来自源码和数据；没有将其标为已实机复现。历史自给率不当作当前全局失败概率。
- 保留现有测试并记录两项失败原因，本次检查不顺带修改测试或实现。TWR 只读，两个仓库原有修改不提交、不清理。

## Validation

- Java 17.0.2 定向城镇、印章、TownCommand 和 TownThermalProjection 测试：376 项，374 通过，2 失败；生产/测试源码编译通过。
- `TownHousingPlanTest` 的失败是配置未加载；`WarehouseBuildingCodecTest` 传入 `initialized=false` 却期待 true，属于夹具/预期不一致。详见计划中的测试记录。
- 首次 Java 8 环境问题改用进程级 Java 17 解决；共享编译目录冲突在其他构建退出后重跑。没有修改构建设置或停止其他任务。
- 本次 living-doc 差异 `git diff --check` 通过；未执行完整项目测试、实机多人/图形或长期数值实验。

## Remaining

- 按计划处理货运站获取、床位口径、测试维护、供给与供热标定、真实存档/多人回归、图形性能和任务引导。
- 本条状态表示调查完成，不表示城镇功能修复完成或正式上线验收通过。
