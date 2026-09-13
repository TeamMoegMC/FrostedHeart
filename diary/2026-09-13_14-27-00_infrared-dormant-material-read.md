# 红外读取既有休眠材料与增量同步

- Time: `2026-09-13 14:27:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `修复红外遗漏休眠材料、清理复查确认的冗余接口；不增加另一套休眠模型`

## Completed

- `InfraredCapture`从已加载Chunk的原`DormantChunkThermalState.materials`读取既有材料记录，复用温度计的`MaterialSectionState.read`。已解析的活动Brick优先；更新中的活动Page不被旧保存记录覆盖。
- 保存记录核对当前BlockState，使用当前材料law。当前law缺失时不再用旧law继续返回材料温度。保存值进入原解析场合成，再量化一次并使用原Brick编码、单纹理和shader。
- 请求/响应增加一个`storedEpoch`；Chunk替换/清空/加载以及Section材料内容变化提供瞬时编号。客户端只在最后一片响应提交后确认。未改变的保存材料不重复发送；同Page里活动材料仍在时，删除保存记录也能清除旧热色。
- `MaterialSectionState`缓存一个Brick mask；相同内容的checkpoint不推进变化编号。编号不进入NBT，不保留旧网络格式或v2存档读取。
- 删除无调用的`WorkerPageStore.publicationGenerationAtSlot`、与`isSurfaceCell`重复的`isMaterialPole`及Air恢复中的冗余材料排除判断，更新必要测试调用。
- 用户询问数组后，删除最初加入的`storedPages`/`storedRevisions`两个全窗口缓存。直接通过81个已加载Chunk引用访问Section数据，无服务端玩家历史缓存。

## Decisions

- `stored`是保存状态这一读取来源，不是新的Page、新的休眠系统或第二份H。空气休眠残差仍不作为红外材料温度。
- 新增capture数组载荷：96字节工作mask、81个Chunk引用和4096个double温度工作值，合计约33 KiB，单份共用并复用；每次返回清除Chunk引用。每次只展开一个变化Section，不复制整个红外窗口的材料记录。
- 有材料历史的Chunk按Section预留8字节变化编号（普通24-Section Chunk约192字节，不含数组头）；Chunk另有8字节替换/加载编号，每个材料快照缓存8字节Brick mask。这些都不是每观察者分配。
- 首条进度中“沿用现有同步协议”指保留原分块传输方式；实际为增量确认新增了一个VarLong头字段，客户端与服务端同时更新，未添加旧协议兼容层。
- Documentation impact: 更新[红外来源](../docs/climate/world-climate-and-temperature.md)、[runtime架构与开销](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[生命周期](../docs/climate/data-lifecycle-and-integration.md)和[主plan](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)。

## Validation

- Java 17；`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，包含生产/GameTest编译，71/71必需测试通过。日志：`run-gametest/infrared-stored-material-tests.log`。
- 新增覆盖：休眠材料与温度计一致、稳定零响应、保存值加解析场后一次量化、更新和拆除清色；真实活动runtime关闭后保留红外温度；活动和保存Brick同Page时的优先级、保存记录删除和整个保存容器清空；所有响应分片及请求中的storedEpoch往返。
- 保存材料小型夹具100次稳定增量请求：0响应、0字节，中位数0.0062 ms、p95 0.008 ms。强制全量100次共13300字节，中位数0.0251 ms、p95 0.0659 ms。此为指定夹具，不代表所有场叠加或大型存档的性能。
- 生产与测试源码不再引用上述已移除接口和两个撤回数组；`git diff --check`通过。渲染shader未修改，不重复此前GPU栅格验证。

## Remaining

- 本次复查确认的红外读取缺口及冗余已处理。主计划的大型存档负载、全面性能比较和独立可读性整理不在本次闭环修复的完成声明中。
