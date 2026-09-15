# 材料变更热路径：批内复用数据与无回调容器

- Time: `2026-09-14 15:48:25 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `保存材料编辑、变更日志与两个setBlockState入口；验证完成后补记文档`

## Completed

- `MaterialSectionState.Editor`仅存在于有材料数据的Section。用写时复制保护已发布快照；批内数据复用，删除标记到snapshot导出时才压紧，参数引用计数避免重复全段扫描。
- `DormantChunkThermalState`通过Editor维护材料；元数据/单点温度读取不导出快照。删除旧不可变逐次update/remove API和`applyStoredMaterialChange`适配器，没有保留并行编辑路径。
- checkpoint记录缩为position/nextState/cause三个int加revision；剔除无用旧状态ID读取，裁剪频率降至每段每tick一次，批量checkpoint投影仅使用一个Editor。
- 两个`setBlockState`边界仍分别覆盖活动Section与没有活动模拟的保存状态。改用MixinExtras `ModifyReturnValue`，原样返回结果并移除CallbackInfoReturnable分配。运行库由既有LDLib携带；build.gradle仅增加编译/API处理器依赖声明。
- 没有给所有Section新增Chunk引用、另建异步调度或改变材料替换/质量变化/相变能量关系。必要的自然温度查询仅限已有保存物体替换/物质量变化且目标仍有材料的情况。
- Documentation impact: 更新数据生命周期、runtime文档与材料主plan。没有改design、渲染格式或既有diary。

## Validation

- 新增实际`LevelChunk.setBlockState`成本与快照隔离GameTest，4096条合法存储记录、384次预热、32组×32次更新；同一方块在STONE/ANDESITE间切换，保留原快照并验证新物体初始化温度。
- 优化前：p50 12,734.375 ns/次，p95 13,203.125 ns/次，53,344 B/次。写时复制阶段：971.875 ns/次、2,246.875 ns/次、48.0703125 B/次。最终返回值钩子：1,050 ns/次、1,181.25 ns/次、0 B/次。日志分别为`build/material-mutation-before.log`、`build/material-mutation-after.log`、`build/material-mutation-final.log`。
- 最终Java17 `runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false` BUILD SUCCESSFUL，74/74测试通过。覆盖相变、冻结checkpoint、替换身份、A-Air-A、红外连续性、参数复用、旧快照不可变、删除压紧后继续编辑、NBT往返。
- `git diff --check`通过；未运行无关GPU测试。额外普通JUnit目标未能运行：`compileTestJava`仍引用已移除的`ConservativeAirGeometry`、`ComponentBrickCompiler`等旧几何类型，共26个编译错误，日志`build/material-storage-unit.log`。没有为使旧测试编译而恢复兼容类。

## Remaining

- 首次共享快照后的写入、导出时的删除压紧仍是O(N)，不承诺所有单次写入均零分配或1 μs。性能结果不是完整服务器tick或大型整合包负载。
- 普通JUnit旧测试源码需另行迁移；当前Forge功能回归已通过。原计划的长期负载验证与可读性专项仍保留原范围。
