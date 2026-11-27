# 普通耐火砖与控温炉正式路线

- Status: Current
- Last verified: 2026-11-28
- Scope: 耐火砖构想、独立试烧、实物观察和研究发现。
- Anchors: [控温实验炉](controlled-furnace.md)、[正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/refractory_brick.json)、[单件烧制配方](../../../TheWinterRescue/kubejs/data/the_winter_rescue/recipes/research/refractory_brick_trial.json)。

## 开始与试烧

高岭土、铝土矿各自按材料类别接受替代：原料、碎矿、粉末、矿块和冻土块的物品观察均可。类别内任取一种即可；同时掌握同类多个形态也只提供一项联络辅助。标签分别为 `the_winter_rescue:research/kaolin_materials`、`bauxite_materials`，聚合 `forge:raw_materials/<矿物>`、`crushed_raw_materials/<矿物>`、`dusts/<矿物>`、`forge:<矿物>` 和对应 `*_permafrost`。物品获得触发也接受这两组形态；既有原料观察仍有效。

获取高岭土、铝土矿或耐火黏土球，观察普通/高耐火砖物品或方块，或观察直接配方含砖的制品，都可触发构想。主动联络则先观察砂砖单砖或砂砖方块物品，学会后把这份记录放到桌面，进入绘图台通用连连看页面。高岭土、铝土矿、黏土球观察是联络辅助，每缺一项增加一级难度；全缺3级、齐全0级。辅助只需学会，不必放到桌面，放入也能正常联络，通关立即获得想法。构想正常自动入档，队伍掌握构想后即可开题。砂砖通过 research/sandy_bricks 物品标签接受 charcoal_pit:sandy_brick_item 或 charcoal_pit:sandy_brick 任一种。现成耐火砖观察既是行为构想入口，也能提供后续参考，但获取构想和开始项目都不要求先有这项观察。

准备阶段没有必需任务。准备好材料后手动进入烧制阶段，温度提示在进入时冻结：没有参考砖观察，提示从1100°C开始试验；有普通/高耐火砖 item 或 block 观察，提示精确1050–1150°C。进入后再补参考知识不改变本阶段提示；两条路径材料、时长与最终成果相同，均可第一次成功。

工作台手混4高岭土粉+4铝土矿粉+1黏土球→1既有 fire_clay_ball。炉体用 CCC/SFS/CCC，C黏土球、S沙、F营火，共6黏土球+2沙+1营火。把耐火黏土球放入样品格，每格一件，燃料格放一块木炭。可在任意时间使用炉子。设为 1050–1150°C（含端点）并烧超过 400 个加载刻，停炉后原格产出普通耐火砖。烧制时间超过阈值时，温区外产碎裂耐火砖。若时间在阈值以内，原料保持原样。填 21 秒可获得超过阈值的定时试烧。

进入烧制阶段后，用放大镜或观察键现场观察一块完整普通耐火砖即可推进任务。任务检查实物品类，接受任何来源的完整砖，不消耗物品。项目结论是烧制条件，沿用既有的成果 ID `the_winter_rescue:controlled_refractory_production`。

## 材料证据与旧工艺

数量只按当前 TWR 生效替代定义计算：营火由 shaped/new.js 的2稻草+2原木制作；冻高岭土/冻铝土矿在 melt_kaolin/melt_bauxite 营火解冻配方中每块200tick得4原料。树桩 hammer_dig 将铝土矿破为 crushed_raw_bauxite，磨盘每份至少2铝土矿粉；高岭土可直接磨至少1粉。因此单次手混的保证基础用量是4高岭土、2铝土矿（破碎后研磨）和1黏土球，不把随机额外粉计入最低需求。工具路线已有树桩（2原木+2石）、磨盘（3石类+1原版石+1木棍）、石锤（2石类+木杆+线），不含耐火砖/铁。实际生成、采集、营火点火和材料供应仍待正式包生存验证。

石磨（`stone_age:millstone`）可直接在工作台制作，旧「固体研磨」研究已退役。石磨任务 `7887EB905BAA7BEF` 在工作台任务后开放，玩家可在制作绘图台之前加工耐火原料。[石器时代任务书](../../../TheWinterRescue/config/ftbquests/quests/chapters/t1.snbt)

现有 Create 搅拌同样9份原料得3 fire_clay_ball，保留其效率优势。陶窑 the_winter_rescue:pottery_recipes/fire_brick、IE高炉索引2、工业窑首项及4单砖+砂浆→2砖块的旧工艺原样保留。依赖 JAR Charcoal Pit 7748097 的原生 pottery 数据也已读取；本路线的数量以 TWR 当前替代配方为准，不从被替换的原生配方推算。新成果不解锁高耐火砖、工业窑砖或IE高炉；旧 blast_furnace 两效果、三线索、mixer父项及 generator_T2/simple_machines下游无改动。

## 数据和集成边界

正式知识资源在 TWR kubejs/data/the_winter_rescue/frostedresearch/knowledge/。三项原料观察用于 `acquisition.optional` 和联络的 `optional_knowledge`。联络的 `required_knowledge` 为空，`inputs` 要求砂砖物品观察。项目的 `access.start_all` 只引用 `idea`。参考砖只在 firing.variants 的 knowledge_all，按 item、block、默认顺序选首个成立路径。直接含砖制品事件是 frostedresearch:refractory_component_observed；索引只算一跳，特殊机器可通过 research/refractory_products_extra 的 item/block 标签补充，目前两标签为空。refractory_bricks 标签含普通/高耐火砖和可选存在的 II 炉砖，只用于触发/提示，不能改变控温炉产物。

三个温区分别配置为 `frostedresearch:controlled_firing` 配方，分别对应 `refractory_brick_trial`、`refractory_brick_trial_low` 和 `refractory_brick_trial_high`。炉体和耐火黏土球手混配方公开。历史验证记录保存在[旧版客户端记录](../../diary/2026-11-12_20-12-00_research-m5-m6-client-validation.md)。
