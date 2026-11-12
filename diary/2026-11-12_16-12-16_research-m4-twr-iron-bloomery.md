# M4 TWR 冶铁炉正式内容交接

- Time: 2026-11-12 16:12:16 +08:00
- Author: Codex / OpenAI GPT-6.1 Sol medium；TWR 数据实施
- Status: partial：正式内容与静态验证完成，FH 机制联调和实际客户端未验。
- Scope: TWR M4 数据/配方/语言；FH 仅新增本日记和 docs/knowledge/iron-bloomery.md。

## Completed

TWR 精确文件清单（12个）：

- 新增 kubejs/data/the_winter_rescue/frostedresearch/knowledge/ideas/iron_smelting.json
- 新增 kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/iron_smelting.json
- 新增 kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/iron_bloomery.json
- 新增 kubejs/data/the_winter_rescue/frostedresearch/knowledge/results/iron_bloomery_design.json
- 新增 kubejs/data/the_winter_rescue/tags/items/research/iron_copper_or_bronze.json
- 新增 kubejs/data/the_winter_rescue/tags/items/research/iron_carbon_fuel.json
- 新增 kubejs/data/the_winter_rescue/tags/items/research/iron_materials.json
- 新增 kubejs/data/the_winter_rescue/recipes/research/prototype_tuyere.json
- 修改 kubejs/server_scripts/src/recipes/shaped/new.js
- 修改 kubejs/server_scripts/src/recipes/shapeless.js
- 修改 kubejs/assets/frostedresearch/lang/zh_cn.json
- 修改 kubejs/assets/frostedresearch/lang/en_us.json

## Decisions

严格采用 M4-A 两阶段 schema、stage task 包装、completion_recipes 和 observation.condition/experiment/team_current_run；想法两组观察 optional，项目三项 start_all required。同队任意当前成员现场观察本次成果，不绑定个人。单 Design 包含两个永久稳定配方；原型配方只来自风道通关临时来源，不在永久 Design 内；风箱现有公开配方未改。零输入联络仅输出本想法，不新增 initial 或全部想法公开。

旧 iron JSON/nonce 不改；映射单 Design，完成信用沿采用的 finished-only 契约由并行 FH bridge 实施。获取铁的 tag 范围、材料依据和玩家操作见[新增路线文档](../docs/knowledge/iron-bloomery.md)。TWR 仓及子目录未找到 AGENTS.md，适用父目录规则沿先前核对；执行 FH 文档/日记约定。未改现有 FH docs、代码、README、plans、coordination 或 design，也未改 .workbuddy/config/.gitignore。

## Validation

- 两个修改脚本 node --check 通过。
- Node 静态检查8份新数据/标签/配方 JSON、双语引用：两组 optional 与 start_all 对象一致、两个阶段与任务/临时配方/本次 block 观察字段符合决定、旧 effect nonce 与单 Design 对应、understanding 为空、永久结果不含原型配方。
- Node VM 比较 new.js 当前逻辑与内存还原的旧 ID 逻辑：173条 shaped 行只改主冶铁炉 ID，材料/数量/图案/顺序相同。提取 shapeless 配方数组比较79行，只改变正常风口的显式 ID，材料/数量/顺序一致。两处同产物移除过滤器存在；其他数组索引不变。初次 shaped stub 缺 enchant 方法，补足模拟方法后比较通过，不是 KubeJS 实际启动验证。
- 铁集合来自正式 bloom_iron/twr:rusted_iron 和 Forge 铁材料标签；现有原型/正式风口三材料、主冶铁炉九材料、公开风箱材料和窑坑制青铜/树桩制板路线均按源码核对。未声称初期全部实玩可达。
- 未运行 Gradle、Java CODEC/RecipeManager、客户端或 Git；未改 Prism 或玩家世界。prototype_tuyere 注册和 M4 schema/临时权限由并行 FH 实现者负责。

## Remaining

集成时确认原型已注册、阶段 JSON 经真实加载器接受、临时原型在无来源时确实不可制作、通关/离线/完成/放弃来源正确；实际 RecipeManager 核对正常风口两条 conditional 旧配方和主炉同产物别名均无绕过。验证真实延迟 blow、同队现场观察、新旧 run/旧 iron finished 与 effect-only 补偿、旧 M3 桥接回归、双语 GUI 以及正式包生存获取。提交由主协调处理。
