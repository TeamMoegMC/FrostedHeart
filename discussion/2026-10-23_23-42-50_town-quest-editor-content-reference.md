# 城镇任务书编辑素材：建议覆盖的内容与源码依据

- Time: `2026-10-23 23:42:50 +08:00`
- Author: `Codex; OpenAI GPT-6; 辅助编辑与源码核对`
- Status: `供作者取舍的编辑建议；机制说明按当前源码核对，章节设计未定`
- Last verified: `2026-10-23`
- Scope: `玩家建立、经营城镇需要知道的机制、操作与反馈；不规定任务数量、顺序、剧情或奖励`
- Code anchors: `TeamTownData.tickMorning`、`HouseBlockEntity.scanStructure`、`TownHousingMealService.settle`、`TownAssignmentModel.plan`、`TownPolicyState`、`WarehouseInterfaceTransfer.balance`、`P2PTerminalBlockEntity`
- Related: [已撤回方案与完整中文稿](2026-10-23_23-42-50_town-quest-withdrawn-design.md)、[城镇文档入口](../docs/town/README.md)、[上线缺口调查](../plans/2026-10-18_20-50-00_town-release-readiness.md)

主要设计由用户决定。本文的“建议”是编辑取舍意见，“源码事实”是机制核对结果，两者分别说明。作者可以把若干主题合成一个任务、放进旁支、思索或提示框；这里没有要求恢复上一版的 20 个节点。

核对基于 FH 当前工作区与 TWR 源码。住宅床位计数包含当前未提交修正；上次 Prism 只更新任务书，没有更新 FH JAR 或其他配方。以下内容不代表 Prism 运行版本已逐项实测。参数若写进正文，应标明默认值并让玩家以当前配置、界面为准。

## 建议优先讲清的玩家问题

| 优先程度 | 玩家实际要解决的问题 | 建议交代到什么程度 |
|---|---|---|
| 基础必备 | 我怎样开始建镇、查看全镇？ | 同队城镇、基础建筑、镇长印章入口；先备住房和食物再扩大人口 |
| 基础必备 | 为什么房子有床却不能招人？ | 结构、空间、床位、温度和空房分别检查 |
| 基础必备 | 食物放哪里、每天吃多少？ | 公共库存、食物资源单位、口粮和四营养的区别 |
| 基础必备 | 岗位和住房列表如何使用？ | 队列顺序、保障人数、实际分配；保障不是固定人数上限 |
| 基础必备 | 为什么早晨过后人数、库存和产量变了？ | 晨间结算；用一次实际检查连接各系统 |
| 生产入口需要 | 狩猎和采矿怎样开始工作？ | 当前需要的建筑、人员与资源条件，清除旧的营地/铁轨要求 |
| 首次接触物流需要 | 为什么物流不能设速度或不搬货？ | 运力来源、线路预约、方向、过滤、库存与区块加载 |
| 可放进补充说明 | 照护法律、住宅选餐细节、库存发信器、双向终端 | 玩家准备使用时再解释，避免开局一次灌输全部规则 |
| 留在编辑资料即可 | 完整公式、配置键、内部扫描边界和类名 | 支撑核对与后续修订，不必进入玩家正文 |

## 1. 建镇入口与镇长印章

**源码事实。** 城镇按队伍共享；建筑放置时通过 `AbstractTownBuildingBlock.setPlacedBy` 登记到放置者队伍。不能把“必须在塔周围某个半径内才登记”写成通用建筑规则。镇长印章用于打开居民、建筑、库存以及岗位、住房、政策等管理入口。[建筑登记源码][S1]；[城镇日常管理][S2]。

**建议交代。** 给玩家一个清晰入口：先准备公共仓库、有效住宅和口粮，再用印章确认建筑和居民记录。热源影响宜居与生产，不等同于建筑归属登记。

**编辑注意。** 印章从哪里获得、是否可重复领取属于任务设计。当前旧章已有印章奖励，可选择保留或调整。若保留可重复领取，不宜把它直接作为长期教学依赖；本地版本 FTB Quests 领取完奖励后会重置可重复任务。

## 2. 有效建筑、住宅容量与供热

**源码事实。** 房间扫描从核心上方开始，住宅需要扫描到门。当前工作区住宅只计床头，实际容量同时受床数和空间限制；结构扫描通过与当前可住是两个判断，温度也参与可用状态。[房间扫描][S3]；[住宅扫描][S4]；[住宅日结模型][S5]。

**建议交代。** 让玩家知道如何摆核心、封闭房间、留门和通路、摆床，以及在哪里查看结构状态、容量、温度和舒适度。改建、拆床或供热变化后再检查一次。

**容易误写。** “一张完整床提供一个床位”必须同时说明空间限制；不能写成“放几张床就能住几个人”。只说“搭了房子”也不能保证该房间在当前温度下可用。上一版的一床一位标题可由作者换成更明确的表达。

**供编辑核对的默认参数。** `HouseDailyModel.calculateCapacity` 使用 `min(床数, floor(空间评分 × 有效地板面积 / 每居民面积))`。床数、人数为计数，地板面积与每居民面积以方块地板面积计，空间评分为无量纲倍率；默认每居民面积为 4，住宅最低地板面积为 4、体积为 8，默认可用温度为 0–40℃。这些来自 `TownModelParameters.Defaults.HOUSING_*`，并非不可变玩法承诺。[参数默认值][S6]。

## 3. 公共仓库与第一次补给

**源码事实。** 有效仓库贡献城镇共享库存容量，生产入库和居民供餐都使用该库存。仓库不要求分配工人。仓库货架用于增加容量，库存空间会限制产物实际入库。[仓库实现目录][S7]。

**建议交代。** 至少明确一次“把食物存到哪里”：通过仓库界面存入，并在公共库存里核对。提示玩家查看剩余容量，避免把产能、产物已入库和可供居民消费混为一谈。

**容易误写。** 普通箱子里放了食物，不等于食物已经进入城镇公共库存；后续可用仓库接口连接外部自动化。

## 4. 难民出现、招募与安置

**源码事实。** 运行中的能量塔、队伍在线等条件下，晨间存在难民出现机会；天气影响概率。招募还需要有效且有空位的住宅。出现、观察、招募、进入居民记录是不同步骤。[`TeamTownData.tickMorning` 与住房分配][S2]。

**建议交代。** 把“先准备住房和口粮，再等难民”讲清楚。招募后让玩家查看居民人数和住所；无法招募时，先查建筑可用状态、空房和床位，而不是继续等下一名难民。

**容易误写。** 不保证每天一定来人；观察任务也不等于已招募。若作者继续用观察检测，应另写明实际招募与安置的操作要求。

## 5. 口粮数量、食物品质与四营养

**源码事实。** 默认每名居民每天需要 20 **食物资源单位**。显式资源数量配置优先，没有覆盖时按食物属性折算；这不是 20 个物品，也不等于玩家饥饿值。蛋白质、脂肪、碳水化合物、蔬菜四通道另行计算，库存总食物单位足够不保证营养均衡。[资源数量解析][S8]；[默认食物折算][S9]；[住宅供餐][S10]。

**建议交代。** 让玩家分清“够吃”和“营养均衡”，并知道去哪里看口粮、实际餐食和营养变化。物品种类建议结合整合包现行食谱另选实例，不沿用已经失效的旧食物分类标签。

**选餐细节。** 每栋住宅共用菜单组成，住宅按队列顺序占用库存。模型优先采用当前可用的最高食物等级，再在该等级内选择能改善住户四营养缺口的食物；不能写成“系统一定从所有食物中选出最均衡的一餐”。[公共菜单模型][S11]。

**供编辑核对。** 默认折算可写为 `F = H + 2 × H × S`，其中 `F` 为每个物品的食物资源单位，`H` 为该食物的原版饥饿恢复值，`S` 为其饱和度倍率；有显式资源数量覆盖时不使用该默认式。完整公式通常无需出现在任务正文。

## 6. 住宅保障与照护法律

**源码事实。** 住宅队列影响住房安排和选餐先后。口粮保障先按住宅与照护顺序分配，库存不足仍会留下缺口；余粮由其他已安置居民均分，最多补到正常口粮。保障不创造食物，也不等于逐人指定菜谱。[口粮分配][S12]；[住宅供餐][S10]。

照护法律变更在下一次城镇晨间结算开始时生效，冷却从申请时的城镇结算日开始计，共 7 次结算。不是现实中的 7 天，也不应教玩家仅凭任意调世界时间绕过等待。具体候选法律与剩余冷却以界面为准。[`TownPolicyState.requestCareLaw/activatePending`][S13]；[`tickMorning/recordDailySnapshot`][S2]。

**建议交代。** 操作提示优先回答“哪个列表决定谁先获得保障”“为什么改了法律还没生效”“为何设了保障仍有人没吃饱”。政策取舍和叙事态度由作者设计，不必在任务里给出唯一正确选择。

## 7. 居民状态与岗位调度

**源码事实。** 居民能否工作受年龄组、健康、精神和住房等规则限制。岗位分配先按队列满足保障目标，再分配剩余劳力；保障人数不是上限，设为 0 仍可能得到剩余劳力。建筑当前不能工作时，其可分配容量会归零。[`TownAssignmentModel.plan`][S14]；[`TeamTownData.assignWork`][S2]；[居民日常模型][S15]。

**建议交代。** 教玩家核对“能工作的人数”“建筑能容纳的人数”“实际分到的人数”三个数，再查看停工原因。调整保障人数后还要观察实际分配，不能仅凭已设置目标判断生产已经启动。

**容易误写。** 不用旧工作优先级描述替代当前队列与保障模型；不把保障 0 写作禁用按钮。不要沿用无限属性成长、智力不影响工作等旧结论；当前属性及具体工作的效率关系以 `ResidentAttributeModel` 和各生产模型为准。[属性模型][S16]。

## 8. 狩猎与采矿的真实起步条件

**狩猎事实。** 猎人之家在晨间直接执行狩猎，不需要另外建设旧“猎人营地”。产出受工人生产力与野外猎物储量等限制，并尝试进入共享仓库。库存装不下时，不能假定狩猎没有发生：对应猎物储量仍会扣除。[`HuntingBaseBuilding.work`][S17]。

**采矿事实。** 矿井当前的 `MineBlockEntity.scanStructure` 直接返回 true，没有铁轨、通道或真实矿石结构要求；矿工之家仍有自身建筑条件。晨间在范围内关联矿井，按三维距离判断，不能承诺自动选最近的基地。同一区块多个矿井不重复提供该区块资源。[矿井扫描][S18]；[`TeamTownData.linkMinesToBases/recalcOreChunkResources`][S2]。

**建议交代。** 每条生产线至少覆盖建筑、工人、资源/猎物来源、产物去向，以及一种停工排查。不要以旧任务里的营地和铁轨要求作为现行门槛，也不要把“当前矿井结构简化”扩写成未来一直不需要结构的承诺。

## 9. 货运站与运力

**源码事实。** 城镇晨间重置服务资源，再由货运站工人产生当日运力。运力表示可承载运输线路的服务能力，不会无限按天累积；实际产能受工人属性和熟练度影响。新增或加速线路需要通过运力预约，后续不足时已接受的线路也会缩放实际运行速度。[货运模型][S19]；[预约模型][S20]；[`TeamTownData.buildingsWork`][S2]。

**建议交代。** 在首次使用仓库接口或 P2P 时说明运力从哪里来：建好货运站、安排可工作居民、等待晨间结算、查看运力，再设置线路。提示距离和速度都会影响需求。

**获取事实。** TWR 当前源码已有 `4 木板 + 4 石砖 + 1 矿车 → 1 货运站` 的合成定义。[整合包配方][S21]。这条配方不是上次 Prism 任务书迁移所同步的内容，出版前仍应核对实际包版本。

**容易误写。** 不写“每个居民固定产生 64 运力”；也不把运力表述为每运一个物品永久消耗一枚货币。

## 10. 仓库接口与库存信号

**接口事实。** 在城镇所在维度右键完成归属后，可接入本队公共仓库，不要求紧贴仓库核心。9 格各设目标物品和数量，先尝试退回无目标、错物或多余库存，再补足目标。退回与补货共享运输预算；红石控制补货，不阻止退回尝试。运力不足时初次归属可能得到 0 速率，补足运力后需要检查并手动设置速率。[接口实体][S22]；[`WarehouseInterfaceTransfer.balance`][S23]。

**发信器事实。** 监测指定物品在公共库存中的数量，按“达到阈值”或“低于阈值”输出信号；不占运输运力。没有可用仓库时输出 0。[库存发信器][S24]。

**建议交代。** 用一个具体但可替换的例子解释“目标库存”而非“往里塞什么就保存什么”，例如为机器维持某种原料数量。指出多余物品会尝试退回。发信器适合在玩家需要按库存启停设备时再介绍。

## 11. 点对点与双向货运

**P2P 事实。** 路线卡依次选择两端建立连接，再次选择同一对已连接端点会断开。发货/收货终端处理正面相反一侧的相邻容器；发送和接收过滤都必须允许物品通过。任一端红石可暂停线路；目标区块需要已经加载，设备不会自动为远端强制加载区块。[路线卡][S25]；[终端实体][S26]。

**双向终端事实。** 内部为 4 格发送区和 4 格接收区，不主动从旁边箱子抽货。外部自动化向发送区插入、从接收区抽取；两方向独立占用运力。[终端缓存与外部接口][S27]。

**建议交代。** 先教最小可用线路：归属、摆放方向、接容器、配对、设速率，最后看一次实际搬运。失败时按运力、方向、过滤、红石、库存/空位、区块加载逐项查。双向终端应另写清进出区，避免玩家照搬发货终端的接箱方式。

## 12. 用晨间反馈串起经营循环

**源码事实。** `TeamTownData.tickMorning` 串联政策激活、营养更新、住房与岗位安排、难民、矿井关联、生产、供餐、居民状态和统计记录。许多结果在结算后才可观察，设置变化不等于结果已经发生。[结算入口][S2]。

**建议写入的一次实践。** 让玩家在自然晨间前后比较人口与住所、实际岗位、食物库存与餐食、停工原因、可用运力。目标可以是“会看出哪里缺了”，而不是仅凭一次不缺粮就宣称全镇永久自给。是否用手动勾选或专门条件检测，由作者决定。

**暂不写成既成能力。** 不把调试 AI 城镇当作已有自然生成 NPC 据点；不承诺已存在成熟的外部城镇贸易、居民研究或自动演化系统。旧终章“下一次更新才能建立玩家城镇”的表述也应重新判断，因为玩家城镇基础运行链已经存在。[上线范围与证据边界](../plans/2026-10-18_20-50-00_town-release-readiness.md)。

## 格式与检测方面的编辑备忘

本节是实现任务时的辅助资料，不是建议恢复旧任务书格式。

- **正文与检测一致。** 物品检测只证明持有；观察只证明看到了目标；手动勾选是玩家自行确认。若需要自动证明有效住宅、成功招募或供给稳定，应先核对是否已有相应检测能力，不能靠一句文案声称检测已经实现。
- **使用本地化段落。** 将中英文文案放在各自语言文件，以 `{key}` 引用。保留简短段落；本机 FTB Library 2001.2.4 的 `{@pagebreak}` 应作为 SNBT 描述数组中的独立元素，不能藏在语言字符串里。不要照抄旧章的嵌入式 JSON 点击事件。
- **区别解锁与显示。** FTB Quests 2001.4.9 中 `all_started` 不等于“前置已可见”；可重复任务与 `invisible` 都会影响玩家能否按预期完成流程。版式检查应在正常玩家视角进行，编辑模式可见不等于普通玩家可见。
- **兼顾已有存档。** 沿用内容时可保留对应任务、子任务与奖励 ID；大幅更换时需要考虑已有进度如何对应。无需为了保留旧 ID 强行沿用旧叙事和排版。
- **实机编辑后核对。** 检查任务图在常用 GUI 缩放下的连线、分页、长标题、两种语言、奖励领取，以及普通玩家视角的前置可见性。解析通过只能说明数据可读。

## 作者可直接交给辅助编辑的工作

主作者确定章节意图、人物口吻、任务顺序与完成目标后，辅助编辑可以逐段核对机制、压缩重复说明、补齐必要限定、检查术语一致性、补英文，并核查检测条件是否真的对应目标。需要改动结构或新增任务时，先提出具体建议，按主作者选定的方案落实。

本文各段的源码链接指向当前实现，类名和方法名作为可搜索锚点。未来代码变化后，应重新核对相应事实。

[S1]: ../src/main/java/com/teammoeg/frostedheart/content/town/block/AbstractTownBuildingBlock.java
[S2]: ../src/main/java/com/teammoeg/frostedheart/content/town/TeamTownData.java
[S3]: ../src/main/java/com/teammoeg/frostedheart/content/town/block/blockscanner/BlockScanner.java
[S4]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseBlockEntity.java
[S5]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseDailyModel.java
[S6]: ../src/main/java/com/teammoeg/frostedheart/content/town/model/TownModelParameters.java
[S7]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/
[S8]: ../src/main/java/com/teammoeg/frostedheart/content/town/resource/TeamTownResourceHolder.java
[S9]: ../src/main/java/com/teammoeg/frostedheart/content/town/resource/TownFoodResourceAmount.java
[S10]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/TownHousingMealService.java
[S11]: ../src/main/java/com/teammoeg/frostedheart/content/town/resident/ResidentPublicMenuModel.java
[S12]: ../src/main/java/com/teammoeg/frostedheart/content/town/model/TownFoodAllocationModel.java
[S13]: ../src/main/java/com/teammoeg/frostedheart/content/town/TownPolicyState.java
[S14]: ../src/main/java/com/teammoeg/frostedheart/content/town/model/TownAssignmentModel.java
[S15]: ../src/main/java/com/teammoeg/frostedheart/content/town/resident/ResidentDailyModel.java
[S16]: ../src/main/java/com/teammoeg/frostedheart/content/town/resident/ResidentAttributeModel.java
[S17]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/hunting/HuntingBaseBuilding.java
[S18]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/mine/MineBlockEntity.java
[S19]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/logistics/TransportStationDailyModel.java
[S20]: ../src/main/java/com/teammoeg/frostedheart/content/town/transport/TransportReservationModel.java
[S21]: ../../TheWinterRescue/kubejs/server_scripts/src/recipes/shaped/new.js
[S22]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceBlockEntity.java
[S23]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceTransfer.java
[S24]: ../src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseLevelEmitterBlockEntity.java
[S25]: ../src/main/java/com/teammoeg/frostedheart/content/town/transport/device/FreightRouteCardItem.java
[S26]: ../src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PTerminalBlockEntity.java
[S27]: ../src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PTerminalBuffer.java
