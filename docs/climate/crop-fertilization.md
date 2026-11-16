# 作物肥料

状态：已实现，专用 GameTest 2/2 通过；客户端整机和独立进程重启未验收。最后核对：2026-11-16。
范围：作物增速、保温、生长期间增产；不改动树苗肥料规则。
代码锚点：`CropFertilization`、`ClimateCommonEvents.beforeCropGrow`、`BlockFertilizerLootMixin`、`SugarCaneFertilizerMixin`。

`frostedheart:vertical_fertilizable_crops` 方块 tag 默认包含甘蔗和 IE 大麻。只有属于这个 tag 的连续同种方块会沿竖直方向向下寻找根部，根部下方的施肥泥土和施肥耕地均可提供肥料。tag 只声明根部定位规则，不为任意新作物自动提供成熟度适配。

增速肥料沿用生长时间乘数：基础 0.5、进阶 0.25、终极 0.125。保温沿用最低温度阈值分别降低 5、10、20℃。生长许可仍保留原有温度、光照和原作物自身的生长概率。实际年龄推进或新增节段后，才以 1/8 概率消耗一格储量；未变化、成熟或受阻不消耗。树苗原有规则单独保留。

增产不再在玩家破坏事件中取消收获并额外刷物品，而是复用 Chorda 的 `BlockDropsEvent`（公共 `Block.getDrops` 六参数入口），并补充四参数自然掉落入口，Create 当前版本的 `BlockHelper.destroyBlockAs` 也使用此入口。普通作物每推进一个年龄阶段，按本阶段增产肥料的额外倍率除以最大年龄记账；成熟后施肥不补记。完整受肥生长的总倍率分别为 2、3、4，部分受肥按阶段比例计算，物品数量采用随机舍入并按最大堆叠拆分。未成熟收获不给增产。

甘蔗按 16 次年龄推进计一个新节段，记录随新节段转移，已有节段不追溯增产。IE 大麻底部按年龄计账，新增上层作为一个生长步骤独立计账，收割上层后下次长出上层重新计账。

增产记录保存在本维度 SavedData `frostedheart_crop_fertilization`，包含位置、方块注册名和额外倍率。读取掉落时查询，作物替换、年龄回退时清除；无掉落移除在本 tick 结束时清除。不会把肥料储量转换成可追溯的收获收益。掉落预览不会消耗增产记录，真正收获导致的移除或重置负责清理。

专用验证入口：`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_fertilizer -PgameTestDirectory=build/fertilizer-gametest`。测试代码位于 `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/CropFertilizationGameTests.java`。
