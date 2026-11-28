# 城镇任务书按当前实现重编

- Time: `2026-10-23 20:58:02 +08:00`
- Author: `Codex; OpenAI GPT-6; quest authoring and source verification`
- Status: `completed`
- Scope: `TheWinterRescue 城镇 FTB Quests 章节与中英文；FrostedHeart 上线调查 R6 状态`

## Completed

- 重编 [城镇章节](../../TheWinterRescue/config/ftbquests/quests/chapters/building_town_expansion.snbt) 和 [中文](../../TheWinterRescue/kubejs/assets/twr_quests/lang/zh_cn.json)、[英文](../../TheWinterRescue/kubejs/assets/twr_quests/lang/en_us.json) 文案。20 个任务按建镇、公共仓储与住宅、招募、供餐与照护、岗位生产、物流扩展和晨间检查组织。
- 保留原有 12 个任务、其全部子任务和奖励 ID，保留印章可重复领取与面包奖励。新增 8 个任务：住宅保障、照护法律、货运站、仓库接口、仓库发信器、点对点货运、双向货运、晨间结算检查。
- 删除旧猎人营地要求、矿井铁轨连接要求、旧粮食分类标签、无限属性成长和“智力不影响工作”等说明，终章不再承诺下一次更新才开放玩家城镇或已有 NPC 据点贸易/居民研究。
- 更新 [上线调查 R6](../plans/2026-10-18_20-50-00_town-release-readiness.md) 的结果。文档影响：运行时行为、配方、配置和存档协议均未改动，已有系统说明无需重写；本条记录任务内容与教学依赖的变更。

## Decisions

- 按用户要求重新设计任务格式，不沿用旧章的嵌入式 JSON 文本。正文使用本地化段落、空白分隔和独立 `{@pagebreak}`；依赖使用默认完成条件，任务图承担导航。
- 格式依据本机 `FTB Quests 2001.4.9` 与 `FTB Library 2001.2.4` 的实际解析代码。`all_started` 不是“前置已可见”，旧章内嵌任务跳转文本也不再复用。
- 印章提前为入门旁支，后续任务不依赖该可重复任务。`TeamData.claimReward` 会触发 `Quest.checkRepeatable` 并重置已领完奖励的可重复任务，不适合用作固定教学门槛。
- 结尾任务 `2E165BB1FB0CE6A0` 删除旧 `invisible: true`，保留 `hide_until_deps_complete: true`。该版本 `Quest.isVisible` 会先把未完成的 invisible 任务隐藏，后续前置检查无法让它显示；其唯一任务又是手动勾选，故改为满足原有前置后可见、可完成。
- 明确物品检测、观察难民和手动勾选各自证明的范围，不把它们写成自动验证房屋有效、招募成功或经营稳定。
- 主要源码依据：`AbstractTownBuildingBlock.setPlacedBy`、`BlockScanner.scanRoomDataFromBlock`、`HouseBlockEntity.scanStructure`、`HouseDailyModel`、`TeamTownData.tickMorning/linkMinesToBases/assignWork`、`TownAssignmentModel`、`TownHousingMealService`、`ResidentPublicMenuModel`、`TownFoodAllocationModel`、`TownResidentCareModel`、`ResidentDailyModel`、`ResidentAttributeModel`、`TownPolicyState`、`TransportStationBuilding`、`TransportReservationModel`、`WarehouseInterfaceTransfer`、`WarehouseLevelEmitterBlockEntity`、`P2PTerminalBlockEntity` 和 `FreightRouteCardItem`。
- UI 名称与操作路径核对 `HouseScreen`、`MineBaseScreen`、镇长印章界面和模组语言资源；整合包获取核对 `kubejs/server_scripts/src/recipes/shaped/new.js`，本次不修改配方。
- 已查找伴生仓库及父级适用的 `AGENTS.md`，只发现 FH 自身规则；遵守 TWR `kubejs/README.md` 中 UTF-8、本地化和最小差异约定。用户明确允许 SubAgent 后，用两个只读子代理分别核对玩法和真实格式。

## Validation

- 使用现有 Java 17 和缓存依赖，直接调用 FTB Library `SNBT.tryRead(Path)`；15 个章节解析通过，无需构建 JAR 或启动游戏。临时解析助手与报告仅放在忽略目录 `build/town-quest-rewrite/`。
- 最新城镇章 20 任务、46 对象 ID；18 个新增 ID 在全任务库中无碰撞。原任务/子任务/奖励 ID 全保留，旧奖励数据相同；无缺失前置、依赖环、空白页或坐标重叠。
- 中文和英文的 126 个引用键均存在，无嵌套或递归占位符错误；每种语言新增 114 键、更新 8 个既有键，其余语言内容保持不变。15 个 `frostedheart` 物品/实体引用均可在当前注册中找到。
- TWR 三个修改文件 `git diff --check` 通过。FH 仅维护计划结果及本条日记；已有床位修复、其他文档修改与 TWR 暂存的 `config/.gitignore` 保留，未提交或推送。

## Remaining

- 尚未启动客户端检查真实任务图、GUI 缩放与中英文分页，也未跑新存档生存教学流程；静态数据与格式检查不替代这些验收。
- 其他城镇上线缺口仍按原调查独立处理，本次不宣告整个城镇可正式发布。
