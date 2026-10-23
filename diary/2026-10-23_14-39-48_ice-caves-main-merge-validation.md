# 冰洞分支合入两库主分支与回归验证

- Time: `2026-10-23 14:39:48 +08:00`
- Author: `OpenAI Codex；主代理`
- Status: `completed`（合并与验证完成；全量单元测试存在三项既有失败）
- Scope: FH master、TWR 1.20

## Completed

- 拉取远端，FH master 先快进至1277e4a68，再合并ice-caves 1a5a0a4cc：合并提交1eebd84af。
- TWR 1.20从e47f7df8合并ice-caves f7c868e5：合并提交274beba7，提交树与冰洞分支一致。
- 两库无文本冲突；FH自动合并的PlayerThermalModel、PlayerTemperatureComputation、MinecraftPhaseController，相对冰洞分支仅增加许可证头。
- 临时stash恢复后核对原暂存补丁完全一致；FH两份文档修改、未跟踪文件和TWR原11项暂存保留，临时stash已清除。
- 文档影响：系统行为未额外改变，本条记录合并结果，无需修改现有系统说明。

## Decisions

- 不混入其它未提交任务，不删除冰洞分支。
- 通过源码与origin/master比较判断既有失败；未复制项目并重编译基线，也未更改无关系统来消除测试失败。

## Validation

- 主代码、单元测试和GameTest编译成功。
- 全量单元测试722项：719通过，3失败。日志build/ice-caves-main-merge-tests.log，报告build/reports/tests/test/index.html。
  - DormantChunkThermalStateTest.savingDoesNotAdvanceOrQuantizeDormantCoolingAgain：测试期待存档版本4，实现为5。
  - WarehouseBuildingCodecTest.codecPersistsOnlyWarehouseStructureAndCapacityState：传入initialized=false，却期待解码为true。
  - TownHousingPlanTest.normalizationAddsBetterHousingFirstAndEditsAreBounded：住房评分读取未加载的Forge配置。
  - 上述测试、温度持久化、城镇实现、配置和CUtils均与拉取的origin/master一致；源码核对未发现合并引入原因，未单独重跑合并前基线。
- runGameTestServer，命名空间frostedheart_ice_cave,frostedheart_ice_cave_body,frostedheart_ice_caves,frostedheart_icicle,frostedheart_icicle_vibration,frostedheart_hot_spring：54/54通过，3m33s。日志build/ice-caves-main-merge-gametests.log。
- TWR合并的16份JSON解析成功。使用Scripts/ice_cave_sample_validation.init.gradle及-PiceCaveSampleChunks=2，加载合并后TWR数据：种子111/222/333 × 冰洞/热液固定群系，共6组，每组4内部区块加12边界区块。采样GameTest 1/1通过，3m47s，cleanup成功。
- 采样日志build/ice-caves-main-merge-pack-sample.log，结果build/ice-cave-natural-samples.json。验证实际噪声/雕刻/装饰兼容性，不证明普通预设出现频率或视觉效果。
- Gradle使用Java17、离线缓存、最多2worker。日志存在既有可选依赖/资源警告，不宣称零错误日志。

## Remaining

- 三项主分支单元测试问题仍保留；未发现本次合并引入的回归，但不能报告全量测试全绿。
- 未推送主分支、打包或同步Prism；未运行客户端视觉验收。
