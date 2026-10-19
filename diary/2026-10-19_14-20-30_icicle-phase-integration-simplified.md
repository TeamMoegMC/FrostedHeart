# 冰锥相变接入收敛

- Time: `2026-10-19 14:20:30 +08:00`
- Author: `OpenAI Codex；主实现与验证代理`
- Status: `completed`
- Scope: 冰锥融化结果、温度公共代码接入范围、相关 GameTest 与气候文档

## Completed

- 按用户确认的方案，删除 `MaterialSectionState.withMaterial`、冰锥专用缺失记录补建、`DormantChunkThermalState` 的冰锥环境投影、`MinecraftMaterialLawCompiler` 的薄冰预算动态推导，以及两处保证温度场的冰锥例外。这三个公共存档/编译类已无本对话代码差异。
- 温度侧仅在 `MinecraftPhaseController` 保留一个共享的 `meltIcicle` 结果处理方法，由活动请求、休眠提交和缺失记录回退路径调用：删除连续链末端；存活源段翻转普通 BlockState 的 `melt_cycle`，通过既有 `GAMEPLAY_TRANSITION` 迁移消费已完成的潜热。没有专用能量扣减 Mutation、材料插入 API、休眠投影、ACK 或存档字段。
- 潜热预算由既有冰锥 JSON 配方 `heating.latent_heat_j=126666.6666666667 J` 提供，仍约为当前完整薄冰的 1/3；材料熔化平台保持 2°C，环境热裂保持 5°C。
- 删除仅验证专用插入 API 的 `IcicleMaterialSeedGameTests`。既有冰锥夹具改用通用 solver checkpoint 和 Editor；修改缺失记录测试为通用冷/暖回退、源段保留及单段融化，增加被删除末端 30% 潜热不转交源段的检查。
- 文档影响：同步[冰锥说明](../docs/climate/icicle.md)、[热源和材料说明](../docs/climate/heat-production-and-network.md)、[数据生命周期](../docs/climate/data-lifecycle-and-integration.md)；不改写历史讨论和日记的旧实现记录。

## Decisions

- 不交换源段与末端的能量，也不在热量推进期间持续转移。末端部分潜热随删除失去；源段按通用材料更新恢复到当前温度的显热起点。
- 存在材料记录时继续按潜热预算推进；确实缺失记录时遵从通用环境平衡回退，可在满足条件的随机刻直接缩短末端，不额外补建潜热起点。保证温度场与休眠同样接受温度系统的统一行为。
- 仅逐块撤回本对话此前新增的分支/方法，未整文件回退，也未改动其他对话维护的世界温度/城镇等改动。
- 遵循用户偏好不打包 JAR，只编译和运行相关回归。

## Validation

- Java 17 的 `runGameTestServer -PgameTestNamespaces=frostedheart,frostedheart_thin_ice,frostedheart_icicle --offline --no-daemon --max-workers=2 --console=plain -Dnet.minecraftforge.gradle.check.certs=false` 通过；2026-10-19 14:29:23 完整 124/124 required tests 通过（冰锥 13 项），Gradle 正常退出 0。日志 `build/icicle-phase-simplification-gametest.log`，源码与 GameTest 编译均通过。
- 验证预算完成后只移除末端、受热段保留预警/倒计时并复位潜热、末端 30% 进度不转交、普通存档回读与下一份预算、真实 worker 迁移/重复 ACK，以及缺失材料记录时的冷/暖环境回退和单段删除。旧温度与薄冰用例全部通过。
- 命令级 `JAVA_TOOL_OPTIONS=-Xmx1536m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8`，Gradle JVM `-Xmx1024m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8`；未修改持久构建配置。
- `git diff --check` 通过；`thermal/` 下仅 `MinecraftPhaseController` 留有源码差异，生产源码不再引用 `withMaterial`。
- `build/libs/frostedheart-1.20.1-0.7.7.jar` 仍为 2026-10-18 21:27:10 的旧包，本轮未运行打包任务。

## Remaining

- 新阈值与通用回退下的游戏观感仍需试玩；完整薄冰预算未来变化时应同步冰锥配方。
