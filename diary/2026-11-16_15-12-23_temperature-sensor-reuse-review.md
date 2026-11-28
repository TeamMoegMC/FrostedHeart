# 温度传感器复用核查与精简

- Time: `2026-11-16 15:12:23 UTC`
- Author: `OpenAI Codex`
- Status: 完成源码核查、精简、离线打包和真实世界回归验证。
- Scope: 仅温度传感器，避免修改其他系统或抽象出仅有一个使用者的新框架。

## 核查与改动

| 项目 | 依据与结果 |
| --- | --- |
| 方块 | 已继承 `CGuiBlock`，其 `use` 复用 NetworkHooks 打开 GUI；`CEntityBlock` 复用工厂和 `CTickableBlockEntity.tick` 分发。不再实现独立打开/网络协议。 |
| 方块实体/保存 | 本轮改继承 `CBlockEntity`，将原版 `saveAdditional/load` 重复生命周期接线改为 `writeCustomNBT/readCustomNBT`。TargetTemperature、Inverted 字段保持兼容。 |
| 菜单 | 已继承 `CBlockEntityMenu`。本轮删除自写距离/维度/移除检查，调用其 `buildValidator(new Validator())` 并复用 test；不能直接用 CBlockEntityMenu.stillValid，因为该重写仅判断移除，未调用父类完整 Validator。 |
| 玩家引用/消息 | 删除菜单重复 player 字段，改用 `CBaseMenu.getPlayer`；温度设置改用现有 sendMessage(int,float)，删除调用处手写浮点位编码。 |
| 数据槽 | 反转状态改用 SLOT_BOOL；功率继续 SLOT_INT。保留 MultipleDataSlotConverter<Float> 的双 16 位适配：实查当前开发 JAR 的 ClientboundContainerSetDataPacket 为 readShort/writeShort，SLOT_FLOAT 只提供单 int 属性，会截断。未修改其他机器的槽实现。 |
| 界面 | 已复用原版 AbstractContainerScreen、EditBox、Button。检查 CUI RealBox/LabeledTextBox：需要 UIElement 父节点和 CUI adapter，非当前原版屏幕的可直接替换控件；不为替换少量解析代码而改写已验收界面。 |
| 注册 | 已复用 REGISTRATE、FHBlockStateGen.existed/existingItemModel、FHBlockEntityTypes.makeType、FHMenuTypes.register、MenuScreens.register。保留注册项本身。 |
| 温度 | 已复用 WorldTemperature.block；没有复制气候/热场计算。 |
| 红石 | 搜索 Chorda block/util 和 FrostedHeart util，未发现符合六面强/弱输出的通用基类；WarehouseLevelEmitterBlock 附带仓库授权/方向/城镇绑定，不能让传感器继承。CBlockEntity.syncData 只通知中心邻居，不能替代外围强充能通知。继续复用原版 POWERED、setBlock/updateNeighborsAt 和红石查询接口，仅保留所需薄逻辑。 |

## 验证

- `./gradlew --offline assemble compileGameTestJava`：通过（BUILD SUCCESSFUL in 1m 33s）。
- 传感器专用 GameTest：通过（All 1 required tests passed）；增加现成 Validator 的近距离有效/超过8格失效检查，既有 NBT 往返和红石检查继续覆盖基类替换。
- 命令：`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_temperature_sensor -PgameTestDirectory=build/temperature-sensor-reuse-gametest`。
- `git diff --check`：通过。
- GUI 源码、资源、配方未改；此前真实客户端截图保持为该界面布局的证据，本轮未声称重跑客户端。

## 文档影响与文件

更新 `docs/climate/world-climate-and-temperature.md` 的实现复用说明。

- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorBlockEntity.java`
- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorMenu.java`
- `src/gametest/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorGameTests.java`
- `docs/climate/world-climate-and-temperature.md`
- 本开发日记。
