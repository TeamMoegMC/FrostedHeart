# Excel相变源表适配并接回JSON生成器

- Time: `2026-09-15 00:54:09 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `state_transition.xlsx与FHRecipeProvider的人工编辑入口`

## Completed

- `FHRecipeProvider.materialTransitions`重新读取`src/datagen/resources/data/frostedheart/data/state_transition.xlsx`，使用现有POI/ExcelHelper、BlockStateParser和StateTransitionData生成配方。
- 原数据页保留名称及原41种材料顺序，补入水/岩浆液位等定义，总计74行。列适配C/O/导热系数、独立heating/cooling目标与阈值、终止边潜热、固定世界条件、效果和稳定recipe_id。冻结首行和标识列，提供中文字段说明页。
- 核实原Excel中铝土、高岭土、泥炭、沙和红沙的will_transit明确为false。新表保留这5个降温方向的原目标和−10°C阈值，cooling_enabled明确为FALSE，不自动开启玩法。
- `*_enabled`仅存在于人工输入端，FALSE使生成器省略该边。目标非空且开关空白时默认启用；目标空白则无边。数值空白采用缺省值，0保留。
- 移除中间人工输入material_transitions.json，运行时仍只读取生成配方。没有修改热算法、tick调度、静态辐射或新增运行时兼容字段。
- Documentation impact: 更新热源/相变、数据生命周期和plan中的权威输入/编辑流程。伴随仓库检查无相关覆盖，本轮未修改，其36项原工作区变更保留。未改design。

## Decisions

- Excel是人工维护源，JSON是生成结果。保留未启用的人工声明，避免生成时过滤后无法再编辑。
- 复用现有生成器和小型读列方法，不引入独立规则引擎或额外生成器运行模式。

## Validation

- `compileDatagenJava`通过。
- 在Forge注册表环境实际调用`FHRecipeProvider.materialTransitions`，74份输出逐项经Codec解析后与发行配方一致。
- 在内存工作簿副本中打开铝土冷却方向并将阈值改为0°C，实际输出包含对应目标及0°C；再验证空白开关默认启用、空白热容使用缺省值。未把测试改动写回最终工作簿。
- 工作簿经Artifact Tool检查、重算、导出和视觉检查，数据页与中文说明页可读。POI实际读取验证布尔、数值、状态属性及各条件字段。
- 临时包含datagen源码的验证环境中，表格专项通过，但一项原异步拓扑等待测试超时。移除临时验证环境后，项目原环境`runGameTestServer`为**90/90通过**。
- git diff --check通过。按用户清理要求，删除临时验证源码、Gradle脚本、Node工作簿脚本、预览、日志、导出侧文件、依赖junction及测试世界；不保留本轮临时文件。本轮未使用Python。

## Remaining

- 完整runData原有warm_stone缺失掉落表问题与普通JUnit旧类型引用问题仍为独立事项。本轮确认表格入口/真实配方生成正确，没有声称这些既有全量任务已修好。
