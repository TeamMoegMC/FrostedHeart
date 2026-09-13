# 实施后复查：参数表增长与旧包装清理

- Time: `2026-09-13 14:56:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `红外/材料改动的调用与保存生命周期复查；发现的问题直接修复`

## Completed

- 发现保存材料反复切换参数时，`MaterialSectionState.update`只追加新参数项，失去引用的旧项继续留在表中。更新现在复用空闲位置；编码只写引用中的参数并重映射索引。没有新增永久计数数组或历史管理器。
- 新增参数项时使用一次临时BitSet扫描当前引用，复杂度与已有数组复制同为线性；已有参数命中的路径不创建它。编码仅在有空闲项时分配重映射数组。材料H和已有不可变快照不被修改。
- 删除只包装`ArenaSpan`的`ThermalCellArena.BrickAllocation`；`stageBrickCells`直接返回范围，主调用和测试随之调整。删除无调用且仍引用旧相变池的`ThermalTestFixtures.phaseBrick`，没有恢复旧API或增加兼容包装。
- 红外场Page检查复用当次捕获的已加载Chunk引用，删除重复`getChunkNow`。
- 没有调整方块热行为、色标、shader、物理驻留范围或休眠数据模型。

## Decisions

- 只处理可证明的增长问题、无用外壳和重复读取；没有按名称把正常缓存、异步交接或当前数据格式误删成“兼容层”。
- Documentation impact: 更新[存档说明](../docs/climate/data-lifecycle-and-integration.md)、[runtime架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)及[主plan](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)。

## Validation

- Java 17；完整`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，包括生产/GameTest编译，72/72必需测试通过。
- 新回归连续128轮改变两块材料的参数：参数表保持有界，改变一块不破坏另一块，旧快照不变；最终NBT只包含仍在引用的一项参数，读回两块温度正确。
- 日志：`run-gametest/thermal-review-cleanup-tests.log`。`git diff --check`通过。
- 修复后检查生产/GameTest及相关测试引用，未再发现`BrickAllocation`、`cellSpan()`、`phaseBrick`、`addPhaseReservoir`及此前移除的旧相变接口。未增加JUnit或测试排除项。

## Remaining

- 本轮确认的问题已修复；本次检查范围内未发现其他需要修改的兼容执行路径。
- 主计划的大型存档性能验收仍未完成；不以本次复查保证整个项目绝无问题或已经实测全局最优。
