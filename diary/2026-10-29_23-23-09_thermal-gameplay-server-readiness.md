# 温度玩法接线与百人服务器资格核查

- Time: `2026-10-29 23:23:09 +08:00`
- Author: `Codex; OpenAI GPT-6.1 Sol; source and validation review`
- Status: `completed for inspection; hundred-player production qualification remains unverified`
- Scope: `温度源、玩法消费者、保存生命周期和负载证据；城镇仅读取取温边界`

## Completed

- 从实际调用者核对玩家tick→PlayerTemperatureUpdate→PlayerTemperatureComputation→gameplayPlayerEnvironment及状态同步；篝火捕获、GeneratorLogic/RadiatorLogic/FountainTileEntity实际调用源入口。
- 核对WorldTemperature.block/air与作物、动物等环境消费，蓄热方块/掉落物/佩戴换热，材料phase随机更新入口，以及Forge区块读写、卸载、维度和服务器关闭。
- gameplayTownEnvironment无直接调用者；当前房屋和狩猎建筑经BlockScanner.sampleTemperature→gameplayPassiveEnvironment取温，未发现该边界漏接。其逐格采样成本未包含在最新双基地负载中。
- 最新100消费者夹具直接调用环境查询，不能代表100完整联网玩家。旧32基地、完整玩法及独立进程重启记录保留各自版本资格。
- Documentation impact: 更正[生命周期说明](../docs/climate/data-lifecycle-and-integration.md)仍称continuousAir默认关闭的旧信息；[部署说明](../docs/climate/thermal-regional-server-validation.md)明确当前128MiB/1000ms活动额度及百人资格边界，保留历史试验数据。

## Decisions

- 当前可进行真实服务器试运行，尚不能承诺百人整包稳定20TPS。接线存在与当前产物的完整玩法验收分别说明。
- 后续重点是固定产物下的分散基地、完整玩家及城镇/机器负载、生产世界保存和跨进程重启；无需以新架构优化作为前置条件。

## Validation

- 只读源码检索和现有JSON核查；更正文档。未修改生产Java、城镇或旧粒子，未构建、启动服务器、运行JUnit或重跑矩阵。

## Remaining

- 当前产物的百人完整负载及整合包生产世界重启验收；主要玩法本轮没有全部重新实测。
