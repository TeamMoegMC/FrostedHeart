# 普通耐火砖与控温炉正式路线

- Status: Verified：FH实现、正式数据、29项服务端测试、独立进程保存/恢复及完整客户端入口/实验通过；材料使用夹具，完整生存采集未实玩。
- Last verified: 2026-11-14（砂砖必要、三原料辅助的联络配置；新入口客户端待重走）
- Scope: refractory_firing 想法、refractory_brick 项目、controlled_refractory_production 单 Design；只增加自有炉批量工艺。
- Anchors: [M5 采用决定](../../plans/2026-11-12_16-00-00_research-m5-controlled-furnace-decisions.md)、[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/refractory_brick.json)、[批量配方](../../../TheWinterRescue/kubejs/data/the_winter_rescue/recipes/research/refractory_brick_batch.json)。

## 开始与试烧

获取高岭土、铝土矿或耐火黏土球，观察普通/高耐火砖物品或方块，或观察直接配方含砖的制品，都可触发构想。主动联络则先观察砂砖单砖或砂砖方块物品，学会后只把这份记录放到桌面，默认10秒。高岭土、铝土矿、黏土球观察是联络辅助，各减1秒；只需学会，不放到桌面。三项辅助齐全时7秒。构想正常自动入档，开始项目仍需构想、砂砖及三项原料观察全部掌握。砂砖通过 research/sandy_bricks 物品标签接受 charcoal_pit:sandy_brick_item 或 charcoal_pit:sandy_brick 任一种。现成耐火砖观察既是行为构想入口，也能提供后续参考，但获取构想和开始项目都不要求先有这项观察。

准备阶段没有必需任务。准备好材料后手动进入烧制阶段，温度提示在进入时冻结：没有参考砖观察，提示在1000–1200°C中部试验、建议1100°C；有普通/高耐火砖 item 或 block 观察，提示精确1050–1150°C。进入后再补参考知识不改变本阶段提示；两条路径材料、时长与最终成果相同，均可第一次成功。

工作台手混4高岭土粉+4铝土矿粉+1黏土球→1既有 fire_clay_ball。炉体用 CCC/SFS/CCC，C黏土球、S沙、F营火，共6黏土球+2沙+1营火。试验模式投1坯料+1木炭，选择1100°C，烧400个加载tick（20TPS时20秒）；1050–1150°C含端点产1普通耐火砖，窗外产1碎砖。温度是本炉工艺参数；运行中改设定只影响下一炉。不强迫失败或遍历温区。

由绑定队伍任意当前成员新鲜观察本次成功砖（手持或容器中的真实物品）完成任务，无需保留/入档观察，不提交、不消耗成砖。普通旧砖、外队/旧run砖、碎砖和笔记不能代替本次成功样；试样合成掉可在任务未完成时重烧。完成研究后单 Design 正常自动入档，开放16坯料+2木炭、600tick→16普通砖的批量工艺，温窗仍为1050–1150°C。批量不要求进行中run，也不伪造试验绑定。

## 材料证据与旧工艺

数量只按当前 TWR 生效替代定义计算：营火由 shaped/new.js 的2稻草+2原木制作；冻高岭土/冻铝土矿在 melt_kaolin/melt_bauxite 营火解冻配方中每块200tick得4原料。树桩 hammer_dig 将铝土矿破为 crushed_raw_bauxite，磨盘每份至少2铝土矿粉；高岭土可直接磨至少1粉。因此单次手混的保证基础用量是4高岭土、2铝土矿（破碎后研磨）和1黏土球，不把随机额外粉计入最低需求。工具路线已有树桩（2原木+2石）、磨盘（3石类+1原版石+1木棍）、石锤（2石类+木杆+线），不含耐火砖/铁。实际生成、采集、营火点火和材料供应仍待正式包生存验证。

**磨盘仍受旧研究约束，并非公开工具。** [millstone定义](../../../TheWinterRescue/config/fhresearches/millstone.json)启动需2石头、1木炭和默认1级灵感；再提交3石头与1煤，两项线索分别提供75%和25%进度，可以完成而不做纸笔小游戏。探索首次到达的新区域可获得旧灵感（`InsightHandler`、`TeamResearchData.onAreaVisited`；区域尺度由 `insightAreaRange` 配置，默认128）。这条旧前置不依赖耐火砖成品；新系统的想法与旧灵感等级是不同机制。

现有 Create 搅拌同样9份原料得3 fire_clay_ball，保留其效率优势。陶窑 the_winter_rescue:pottery_recipes/fire_brick、IE高炉索引2、工业窑首项及4单砖+砂浆→2砖块的旧工艺原样保留。依赖 JAR Charcoal Pit 7748097 的原生 pottery 数据也已读取；本路线的数量以 TWR 当前替代配方为准，不从被替换的原生配方推算。新成果不解锁高耐火砖、工业窑砖或IE高炉；旧 blast_furnace 两效果、三线索、mixer父项及 generator_T2/simple_machines下游无改动。

## 数据和集成边界

正式知识资源在 TWR kubejs/data/the_winter_rescue/frostedresearch/knowledge/。三项原料观察分别出现在 acquisition.optional、联络 optional_knowledge 与 access.start_all；联络 required_knowledge=[]，inputs 与项目 start_all 另要求砂砖物品观察。参考砖只在 firing.variants 的 knowledge_all，按 item、block、默认顺序选首个成立路径。直接含砖制品事件是 frostedresearch:refractory_component_observed；索引只算一跳，特殊机器可通过 research/refractory_products_extra 的 item/block 标签补充，目前两标签为空。refractory_bricks 标签含普通/高耐火砖和可选存在的 II 炉砖，只用于触发/提示，不能改变控温炉产物。

唯一 Design 目标为 the_winter_rescue:research/refractory_brick_batch。trial 配方带 experiment=frostedresearch:refractory_trial，batch 无 experiment；二者类型都是 frostedresearch:controlled_firing。炉子和手混配方公开，不依赖旧研究，不填写 legacy_replacement。

`frostedresearch:controlled_furnace`、`cracked_refractory_brick` 和 `controlled_firing` 已在生产注册，温度结算、取消/拆除、堵塞输出、观察绑定、参考路径冻结和重启恢复见[M5服务端记录](../../diary/2026-11-12_18-10-45_research-m5-controlled-furnace-handoff.md)。

[最终客户端](../../diary/2026-11-12_20-12-00_research-m5-m6-client-validation.md)从无想法开始，经过实际10秒联络、三项N观察/保留/学习、正常工作台合成与炉操作，完成试验并实际产出16砖批量；验证JEI权限、进入后参考冻结与重开时精确提示，以及中英文教程。材料、地面和第二轮遗忘等使用夹具，不表示从采集开始的完整生存通关。多记录设备选择也有实际CUI验收，详见同一记录。

上段11月12日证据的联络入口为当时的零输入规则。现已改成先学会砂砖物品观察并放到桌面联络，三项原料仅作为联络辅助、仍是项目开始条件；实验和权限规则未变。旧 M5 自动客户端演示的零输入开场不能直接作为新入口验收，需按新顺序重走。
