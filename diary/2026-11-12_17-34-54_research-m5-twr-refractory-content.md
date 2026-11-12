# M5 TWR 普通耐火砖内容交接

- Time: 2026-11-12 17:34:54 +08:00
- Author: Codex / OpenAI GPT-6.1 Sol medium；TWR 正式内容
- Status: partial：静态数据完成，待 FH 机制和正式包运行验收。
- Scope: TWR 新增 M5 数据及双语键；FH 只新增本文与 docs/knowledge/refractory-brick.md。

## Completed

TWR 精确文件清单：13份新增JSON、2份语言追加，共15文件。

- kubejs/data/the_winter_rescue/frostedresearch/knowledge/ideas/refractory_firing.json
- kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/refractory_firing.json
- kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/refractory_brick.json
- kubejs/data/the_winter_rescue/frostedresearch/knowledge/results/controlled_refractory_production.json
- kubejs/data/the_winter_rescue/tags/items/research/refractory_raw_materials.json
- kubejs/data/the_winter_rescue/tags/items/research/refractory_bricks.json
- kubejs/data/the_winter_rescue/tags/blocks/research/refractory_bricks.json
- kubejs/data/the_winter_rescue/tags/items/research/refractory_products_extra.json
- kubejs/data/the_winter_rescue/tags/blocks/research/refractory_products_extra.json
- kubejs/data/the_winter_rescue/recipes/research/fire_clay_hand_mix.json
- kubejs/data/the_winter_rescue/recipes/research/controlled_furnace.json
- kubejs/data/the_winter_rescue/recipes/research/refractory_brick_trial.json
- kubejs/data/the_winter_rescue/recipes/research/refractory_brick_batch.json
- kubejs/assets/frostedresearch/lang/zh_cn.json
- kubejs/assets/frostedresearch/lang/en_us.json

## Decisions

采用M5最小方案：无legacy_replacement；单Design只管理新自有炉批量配方。手混9格4/4/1→1坯料，炉6黏土+2沙+1营火公开。试验1坯1木炭400tick、批量16坯2木炭600tick，成功窗1050–1150含端点；成功沿用普通耐火砖，失败为碎砖。同队本次成功样只观察、不消耗/提交、不要求失败。

开始要求想法与kaolin/bauxite/clay三项观察；获取想法时这三项optional。参考砖不在optional或start_all，只影响进入firing时冻结的方法提示。prepare空任务后手动推进；三顺序变体只换提示、不改奖励。四种行为入口OR与本想法专用零输入联络均已声明；直接产品使用约定专用事件，extra标签先为空，不造递归索引。

未改旧pottery/blastfurnace/industrial kiln/mixer脚本、旧81研究或其权限/下游。没有第三方机器锁、额外坯料物品或补偿迁移。无Git/Prism操作；不改FH既有docs/README/代码/总计划/coordination/design。

## Validation

- Node解析13份新JSON并断言：四入口、optional/start_all一致、空prepare、参考item/block/default顺序、实验队伍/run条件、两个配方数量/温窗/时长/产物、唯一Design batch目标、无legacy_replacement、两语言所引键齐全一致；通过。旧配置目录仍81文件。
- 材料链核对当前TWR营火替代2稻草+2原木、冻矿200tick→4原料、树桩破铝土矿、磨盘保证2铝土粉/1高岭土粉、旧搅拌9材料→3坯料、旧陶窑fire_brick与新低产手混。没有用原版营火或被TWR替换的原生配方计算材料。
- 只读打开当前Gradle依赖 curse.maven:charcoal-pit-reloaded-534602:7748097 的JAR，并读取 pottery_recipes/sandy_bricks.json；确认依赖原生pottery数据存在。本项目普通砖旧陶窑数量直接取TWR fire_brick.json，不据该砂砖配方推算。
- 当前检索未找到 controlled_furnace、cracked_refractory_brick、controlled_firing 的FH生产注册；这些资源引用、M4/M5任务schema及直接产品索引交由并行实现者集成。静态断言不冒充Java CODEC/RecipeManager验证。
- 未运行Gradle、客户端或部署；未新增JS，未触碰既有配方脚本、旧项目、.workbuddy或config/.gitignore。

## Remaining

FH注册后用真实加载器验证新recipe type与stage variants；运行炉的试验/批量权限、温窗边界、绑定观察与重载/取消/堵输出结算；分别验证四个想法入口、参考路径冻结、旧三生产路线继续可用，以及正式包材料与下游生存路线。提交由主协调统一处理。
