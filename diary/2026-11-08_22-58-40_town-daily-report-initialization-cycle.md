# 修复矿业与运输站日报静态初始化循环

- Time: `2026-11-08 22:58:40 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and validation`
- Status: `completed`
- Scope: `MineBaseBuilding.MiningDailyReport`, `TransportStationBuilding.TransportStationDailyReport`, initialization-order regression tests

## Completed

- 对照用户 22:38:32 崩溃日志确认：读档先访问 `MiningDailyReport.CODEC`，`EMPTY` 构造调用外层 `sanitize()`，触发外层 `MAP_CODEC` 初始化并反读尚未赋值的日报 `CODEC`，导致 `BuildingRecordsCodec.strict(null).optionalFieldOf()` 空指针。
- 将数值清理方法移入日报 record，断开外层初始化依赖。运输站日报具有同样路径，一并修复；运输预测显式复用日报内的方法。
- 不改变数值规则、字段名、默认值或存档版本；无需删除或迁移日报。
- Documentation impact: 更新 [building-data.md](../docs/town/building-data.md) 中的日报初始化约束与验证日期。未改动设计文件、整合包或玩家存档。

## Decisions

- 修复依赖方向，不通过吞异常、调整静态字段顺序或删除存档绕过问题。
- 四个 Forge GameTest 分别隔离加载目标建筑及其嵌套类，覆盖日报优先和建筑优先两种顺序，避免启动或其他测试预热掩盖循环。其他依赖使用真实 Forge 运行环境。

## Validation

- Java 17，主源码编译通过。
- 初次完整 `compileGameTestJava` 被已有 `DepositClientValidation` 缺少 `stateCounts` 及 `DepositGameTests` 泛型错误阻塞，未修改这些文件。临时 `build/report-init.init.gradle` 仅排除 deposit 测试目录。
- 最终命令：`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -I build/report-init.init.gradle -PgameTestNamespaces=frostedheart_report_initialization -PgameTestDirectory=build/report-init-run '-Dnet.minecraftforge.gradle.check.certs=false'`。
- **4/4 required passed**, `BUILD SUCCESSFUL`。覆盖两类日报两种加载顺序、缺省日报默认值、非空日报编解码以及建筑保存/重读后的日报一致性。日志：[report-init-test.log](../build/report-init-test.log)。
- `git diff --check` 通过。

## Remaining

- 尚未打包部署到 Prism，也未在图形客户端重开用户原崩溃存档；上述通过范围为源码编译及真实 Forge 环境中的初始化与 Codec 回归。
