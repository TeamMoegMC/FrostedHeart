# 实验完成后的固定产物与原物返还

- Time: `2026-11-18 23:39:14 +0800`
- Author: Codex（OpenAI，开发与验证）
- Status: completed
- Scope: Frosted Research 实验台、实验需求定义和设备 GameTest

## Completed

- 实验需求可配置固定产物和逐项归还投入的物品。四格领取区容纳三格原物与一组固定产物。开工先预留空间，成功登记实验次数时才发放。玩家可在实验期间领取此前的产物。
- 更新了[定义与 JSON 示例](../docs/knowledge/definitions.md)、[台面操作](../docs/knowledge/workbench.md)和[研究流程](../docs/research/gameplay-and-integrations.md)。

## Decisions

- 保存每次开工时预计交付的实际物品堆与各输出槽增量。中途取走旧产物、重新加载方块或重载实验定义，都不会重建已经投入的工具。主动停止、切换目标、拆台或任务失效会取消本次交付。
- 本次提供按实验启用的功能。正式整合包没有指定需要配置的实验。

## Validation

- Java 17 编译改动源码和测试。Gradle Wrapper 因外部缓存锁无写权限未能启动，使用本地缓存依赖与 Forge 47.3.0 的 GameTest 服务端运行设备测试。
- 最终运行命名空间 `frostedresearch_m6_devices` 全部 6 项通过，包括新增批次 `experiment_returns`。两张实验台竞争同一需求时共返还一把保留名称与耐久的剪刀、两个瓶子。验证还覆盖满槽不扣料、领取槽拒绝放入、运行中 Shift 领取上批产物和方块 NBT 重载。仅运行五项旧用例的早期结果未计入本次验证。

## Remaining

- 未进行客户端画面验收或独立进程重启测试，未部署至整合包实例。
