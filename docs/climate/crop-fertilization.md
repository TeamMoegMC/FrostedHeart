# 作物温度与肥料

- 状态　当前实现。最后核对日期 2026-11-20，核对植株材料参数、材料优先的取温与服务端生长判定。
- 范围　速率、保温、存活条件、作物面板与生长期间的增产。
- 代码锚点　`MinecraftThermalProfiles.classify`、`WorldTemperature.getTempAtBlock`、`WorldTemperature.checkPlantStatus`、`SoilThermometerUpdatePacket.sample`、`PlantTempStats.getStats`、`ClimateCommonEvents.beforeCropGrow`、`CropFertilization`、`BlockFertilizerLootMixin`、`SugarCaneFertilizerMixin`。

## 生长温度

对准作物时，土壤温度计的作物面板显示 **生长温度**。服务端用摄氏度，通过 `WorldTemperature.getTempAtBlock(level, pos)` 读取作物所在格。作物生长、适宜温度下的加速和温度死亡都使用这一读数。优先读取作物本身已有的材料温度，读不到时使用同格的环境气温。右键测耕地采用同一规则，取样位置为准星命中的耕地格，聊天注明读数来自材料还是环境气温。

带 `#minecraft:crops` 标签的作物使用 `GameplayMaterial.CROP`。源码默认热容为 `720 J/K`，每面换热导纳为 `2.5 W/K`。植株放置时按所在格的自然温度初始化，随后与邻格换热。数据包可用 `frostedheart:state_transition` 配方的 `conductance_w_per_k` 覆盖默认导纳。

- 篝火直接加热上方空气，本体也向六面换热。作物有可读材料记录时，读数随材料换热而变。使用环境气温回退时，读数反映该格的空气场与解析热场。[篝火供热](heat-production-and-network.md#campfire-body-and-six-face-exchange)
- 能量塔的解析场在环境气温回退中提供保底，取当地自然温度加上该位置的塔温升。篝火和塔同时工作时，这个回退取当地空气温度与保底中的较高值。参见[能量塔供热](heat-production-and-network.md#generator-gameplay-floor)。
- 以默认小麦 `plant_temperature/wheat.json` 为例，−5～35°C 存活，5～30°C 可生长，15～24°C 最适合生长，边界值包含在范围内。光照和露天冰雪天气也会影响生长和存活。温度不适导致的随机死亡还要通过配方 `heat_capacity` 的概率判定，小麦每次符合条件的死亡检查有 1/10 概率枯死。保温肥料调整最低阈值，仍显示实际生长温度。

## 肥料与收成

`frostedheart:vertical_fertilizable_crops` 方块 tag 默认包含甘蔗和 IE 大麻。只有属于这个 tag 的连续同种方块会沿竖直方向向下寻找根部，根部下方的施肥泥土和施肥耕地均可提供肥料。tag 只声明根部定位规则，不为任意新作物自动提供成熟度适配。

增速肥料沿用生长时间乘数：基础 0.5、进阶 0.25、终极 0.125。保温沿用最低温度阈值分别降低 5、10、20℃。生长许可仍保留原有温度、光照和原作物自身的生长概率。实际年龄推进或新增节段后，才以 1/8 概率消耗一格储量；未变化、成熟或受阻不消耗。树苗原有规则单独保留。

增产不再在玩家破坏事件中取消收获并额外刷物品，而是复用 Chorda 的 `BlockDropsEvent`（公共 `Block.getDrops` 六参数入口），并补充四参数自然掉落入口，Create 当前版本的 `BlockHelper.destroyBlockAs` 也使用此入口。普通作物每推进一个年龄阶段，按本阶段增产肥料的额外倍率除以最大年龄记账；成熟后施肥不补记。完整受肥生长的总倍率分别为 2、3、4，部分受肥按阶段比例计算，物品数量采用随机舍入并按最大堆叠拆分。未成熟收获不给增产。

甘蔗按 16 次年龄推进计一个新节段，记录随新节段转移，已有节段不追溯增产。IE 大麻底部按年龄计账，新增上层作为一个生长步骤独立计账，收割上层后下次长出上层重新计账。

增产记录保存在本维度 SavedData `frostedheart_crop_fertilization`，包含位置、方块注册名和额外倍率。读取掉落时查询，作物替换、年龄回退时清除；无掉落移除在本 tick 结束时清除。不会把肥料储量转换成可追溯的收获收益。掉落预览不会消耗增产记录，真正收获导致的移除或重置负责清理。

专用验证入口：`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_fertilizer -PgameTestDirectory=build/fertilizer-gametest`。测试代码位于 `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/CropFertilizationGameTests.java`。

连续收割验证：`createConsecutiveHarvests` 调用 Create `HarvesterMovementBehaviour.visitNewPosition` 的收割、种子预留和补种逻辑，仅替换掉落接收端以计数。三轮依次为施肥、不施肥、重新施肥；小麦产量验证为 2、1、2，每轮补种为年龄 0 且旧增产记录为 0。生长阶段由测试推进并调用结算事件，不代表自然随机生长全周期或客户端运动装置验收。

`CropFertilization.temperatureData` 按 `5 * grade.preserve`（°C）降低最低存活、生长和适宜温度。作物与树苗检查、随机死亡和 `PlantTempStats.getStats` 共用这一调整规则。物品提示显示配方基础范围。

按住 Shift 展开时始终显示存活、生长、最快生长温度范围、光照范围与冰雪抗性；条件已满足也不隐藏，未满足的条件仍以红色标题提示，顶部保留当前状态。

## 温度验证

`ThermalCropGameTests` 使用真实耕地和小麦，覆盖无热源、只点篝火、只有能量塔解析场和同时供热。另用带热场的冷材料记录核对作物面板与服务端生长判定的一致性。保温肥料用例在同一读数下验证生长和死亡判断。运行 `./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_crop_temperature`。面板随服务端取温回包刷新，`FHNetwork.protocolVersion` 的 `soil-thermometer-3` 对应含位置、生长温度、天气暴露标志和可选树苗状态的读数包。
