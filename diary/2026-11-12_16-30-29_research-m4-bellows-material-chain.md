# M4 风箱材料链只读补核

- Time: 2026-11-12 16:30:29 +08:00
- Author: Codex / OpenAI GPT-6.1 Sol medium；M4 内容补充调查
- Status: partial：完成 TWR/FH 源码路线核对；未运行世界。
- Scope: M4 普通风箱的杂酚油、防腐木、桶与树桩台阶来源；iron trigger 扩展建议。

## 完成

- 更新 [冶铁炉玩家路线](../docs/knowledge/iron-bloomery.md) 末尾短节：记录无铁桶配方路线、log pile/collector/creosote bucket/barrel/防腐木/树桩台阶的数量和工具、Charcoal Pit 原生 pottery 烧砖路线、仍待运行核验的 fluid 转运。
- 铜锡窑坑配方3铜类+1锡类→4青铜锭；TWR bronze ingot→1 bronze sheet 的树桩配方使用 hammer_dig、chopTimes=3。FH stone_hammer 是 FHToolMaterials.FLINT 的 HammerItem，并实现 ToolCompat.hammer_dig，因此这段板材路线不需要铁工具。
- TWR bucket 配方用3 #twr:plates/structural；动态结构金属集合含 bronze。collector 的配方接受 frostedheart:ceramic_bucket 或 minecraft:bucket。Ponder 的 creosote_funnel 显示 log pile 在 sandy_collector 下燃烧生成杂酚油，之后用 IE creosote bucket 取液。
- 7块防腐木覆盖普通 bellows 的6木料及1块树桩切板原料，可产2个水平防腐木台阶；barrel 每块耗125mB，合计875mB，另需3皮革。防腐木 recipe 的加工时长1200tick。
- 主协调补充依赖 JAR 源证：data/charcoal_pit/recipes/pottery_recipes/sandy_bricks.json 用 charcoal_pit:pottery 将 unfired_sandy_brick 烧为 sandy_brick_item、exp=0.3；同 JAR rawrecipe sandy_brick.json 为1沙+4黏土球→4未烧砖。TWR remove.js 移除 charcoal_pit:sandy_bricks 普通组块配方，但未移除 pottery_recipes/sandy_bricks；TWR以砂浆补充4 firing items→2砖块。原生路线两批（2沙+8黏土球→8未烧/成品）足够收集器2砖并用4砖合成两个块，其中1块给原型；不用IE Blast Furnace或Car Kiln。因此当前没有由砂砖烧制导致的前置循环。IE Blast Furnace 与 Car Kiln 是替代路线。
- 原生 Charcoal Pit collector 配方需要铁桶，TWR 的 change.js 按同产物替换为2 sandy_brick_item+陶桶或普通桶；TWR普通桶由3结构板制，青铜属于结构金属，找到了不依赖铁的桶路线。collector 出液可由 IE creosote bucket 取用，但 barrel fluid_in 的实际装载操作仍待运行核验。
- Coke Oven 无 parents 仍有材料门槛：TWR cokebrick recipe 需要已烧 sandy_brick_item、砂浆、结构金属板，组炉需工程锤。其无父项不能据此推断材料零门槛。
- 铁 tag 只提出将已有 recipe 引用的 #forge:plates/iron、#forge:rods/iron、#forge:dusts/iron 作为候选；没有修改 M4 正式标签或任何 TWR 文件。

## 未验证

Charcoal Pit 原生 pottery 砂砖 recipe 已由主协调读取依赖 JAR 确认；本补核仍未验证正式 RecipeManager 实际收录、Creosote Bucket 到 Charcoal Pit barrel 的注液交互、单次 log pile 产液量及皮革/窑坑/树桩材料的完整生存可达性。本次未修改代码/配方/tag、未运行 Gradle/客户端/Git。
