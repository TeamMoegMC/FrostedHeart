# 温泉管理员调试定位命令

- Time: `2026-10-22 14:37:14 +08:00`
- Author: `OpenAI Codex / GPT-6；温泉定位子代理`
- Status: `partial`（实现及验证源码完成，主代理统一编译与运行验证）
- Scope: FH `HotSpringLocateCommand`、`HotSpringLocateGameTests`、`docs/worldgen/hot-spring-locator.md`

## Completed

- 独立 Forge 事件订阅类注册 `/fh locate hot_spring` 及现有 FH 长前缀别名，无需修改共用注册入口。
- 默认分 tick 只读已加载区块；提供明确的 `generate [radius_chunks]`、`status` 和 `cancel`。真实温泉源/流动格命中后输出维度与坐标，点击只填写传送建议。
- 定向测试源码覆盖源/流动泉及普通水、负区块/完整高度坐标、范围与预算、未加载只读不生成、生成首 tick 不阻塞、权限与原版 `/locate` 根保留。
- 文档影响：新增[温泉定位说明](../docs/worldgen/hot-spring-locator.md)，主代理统一维护世界生成入口；未修改生成参数或配套包数据。

## Decisions

- 不依赖新索引或温泉生成事件，兼容旧自然泉与手放温泉；使用 section palette 过滤减少无关逐格读取。
- `getChunkFuture` 在 Minecraft 1.20.1 服务端主线程也会 `managedBlock`；核对本地映射 JAR 字节码后，采用单个自有 FULL ticket + 后续 tick 轮询，不引入线程池。
- 全服单任务、半径 `0..32`、默认 `16` 区块；单 tick `16` 个目标/`64` 个 section，单目标 `600` tick 等待、任务 `12,000` tick 上限。显式生成可能推进原版邻区依赖，取消保留已有地形。

## Validation

- `javap` 核对 Minecraft 1.20.1 / Forge 47.3.0 的 `ServerChunkCache.getChunkNow/getChunkFuture/addRegionTicket/removeRegionTicket`、`LevelChunkSection.maybeHas`、`WorldBorder.isWithinBounds(ChunkPos)`、完整高度 section 转换与命令源 API。
- 静态检查命令仅注册 FH 前缀、默认路径只读、票据完成/取消/离线/换维度/停服释放、有限搜索范围与 section/chunk 预算。
- 按主代理分工未运行 Gradle，也未提交、打包或部署。新增命名空间 `frostedheart_hot_spring_locate` 需纳入统一 GameTest 验证。

## Remaining

- 主代理运行编译和本次 3 项 GameTest；实际整合包验证自然泉命中、远处加载取消、旧世界/手放泉、坐标建议与生成耗时。
- 世界生成入口由主代理统一链接；本条记录不作为运行测试已通过的证据。
