# 温度传感器方块

- Time: `2026-11-16 14:56:00 UTC`
- Author: `OpenAI Codex`
- Status: 实现、离线打包和专用 GameTest 串行复验完成；客户端视觉验收未执行。
- Scope: `frostedheart:temperature_sensor`，不修改配套整合包。

## 决策与行为

按用户要求添加温度控制红石方块及 GUI。默认 0℃、正常模式，`WorldTemperature.block` 在自身位置每 20 tick 查询；正常 `>=`、反转 `<`，六面强/弱信号为 15 或 0。修改配置立即重算；保存负小数阈值与反转模式；拆除通知邻居。沿用 Chorda 方块/菜单框架；浮点菜单属性拆成两个 16 位值，避免原版属性包截断。模型复用原版纹理，无额外位图依赖。

配方形状 `PPP / TCT / IVI`，铁/钢板和铁/钢锭采用逐格替代标签，铜板、殷钢板采用 Forge 标签。已查看 FrostedHeart 和 TheWinterRescue 的注册/金属配方；配套仓库未发现 AGENTS.md，且未修改。

## 验证

- `./gradlew --offline compileJava processResources`：通过。
- `./gradlew --offline assemble`：通过，产物 `build/libs/frostedheart-1.20.1-0.7.16.jar`。
- Python JSON 解析、配方形状/替代项检查：通过。
- 生产 JAR：确认传感器类、配方/模型/掉落资源、中英文合并文本、金属机器标签存在，GameTest 不进入生产 JAR。
- `git diff --check`：通过。
- `./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_temperature_sensor -PgameTestDirectory=build/temperature-sensor-gametest`：首轮服务器启动时缺少 AITownManager 类；期间并行 assemble 重编译共享 classes，可能干扰运行。打包完成后串行重跑通过：`All 1 required tests passed`、`BUILD SUCCESSFUL in 3m 31s`。覆盖默认值、等号阈值、正常/反转、六面强弱信号、负小数配置 NBT、非有限输入与定时重算。首轮失败不作为功能失败结论。
- GUI 编译通过，实际渲染、输入与鼠标操作尚未进行客户端验收。

## 文档影响

已在 `docs/climate/world-climate-and-temperature.md` 增加传感器规则、温度口径、保存、配方和验证入口；在 `src/gametest/README.md` 增加专用入口。

## 改动文件清单

- `diary/2026-11-16_14-56-00_temperature-sensor.md`
- `src/gametest/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorGameTests.java`
- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorBlock.java`
- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorBlockEntity.java`
- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorMenu.java`
- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorScreen.java`
- `src/main/resources/assets/frostedheart/blockstates/temperature_sensor.json`
- `src/main/resources/assets/frostedheart/models/block/temperature_sensor.json`
- `src/main/resources/assets/frostedheart/models/block/temperature_sensor_on.json`
- `src/main/resources/assets/frostedheart/models/item/temperature_sensor.json`
- `src/main/resources/data/frostedheart/loot_tables/blocks/temperature_sensor.json`
- `src/main/resources/data/frostedheart/recipes/temperature_sensor.json`
- `docs/climate/world-climate-and-temperature.md`
- `src/gametest/README.md`
- `src/generated/resources/data/frostedheart/tags/blocks/machines/metal.json`
- `src/main/java/com/teammoeg/frostedheart/bootstrap/client/FHScreens.java`
- `src/main/java/com/teammoeg/frostedheart/bootstrap/common/FHBlockEntityTypes.java`
- `src/main/java/com/teammoeg/frostedheart/bootstrap/common/FHBlocks.java`
- `src/main/java/com/teammoeg/frostedheart/bootstrap/common/FHMenuTypes.java`
- `src/main/resources/assets/frostedheart/lang/en_us/en_us.json`
- `src/main/resources/assets/frostedheart/lang/zh_cn/zh_cn.json`

最终 GUI 焦点微调：仅在输入框未获焦点且用户尚未编辑时刷新目标文本，避免重置光标/选区。此修正不改变服务端逻辑；最终串行 `./gradlew --offline assemble` 重新编译和打包通过（`BUILD SUCCESSFUL in 50s`）。
