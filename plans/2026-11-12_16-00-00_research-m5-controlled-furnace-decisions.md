# M5-A：控温实验炉与普通耐火砖最小实施决定

- Time: `2026-11-12 16:00:00 +08:00`
- Authors: `Codex / OpenAI，只读调查与实施设计`
- Status: `completed`（设计交付，待实施；未运行游戏验证）
- Scope: 新增普通耐火砖研究、自有控温炉、下一阶段提示分支；沿用已定稿 M4 stages/OBSERVATION，不替换旧高炉研究。
- Related: [M4 接口及协作修订](2026-11-12_15-45-00_research-m4-stage-tuyere-interface-decisions.md)、[总计划 M5](2026-11-11_23-38-07_research-system-playable-development-plan.md)。

## 1. 采用的最小方案与真实旧链

**保留所有既有耐火砖生产工艺，新成果只解锁自有控温炉的高效批量生产。** 不为新增研究给陶窑、IE 高炉、工业窑补三套权限 mixin，不先撤回公开能力再建立补偿迁移。新研究的价值是无需 Create 搅拌系统或工业设备即可稳定批量烧普通耐火砖；现有设备路线仍可选，生产成果没有设备成型权限。

| 当前事实（脚本简写均位于 TWR `kubejs/server_scripts/src/`，其余写出仓根相对路径） | 实施边界 |
|---|---|
| `recipes_types/create/mixing.js` 第一项：4 kaolin_dust +4 bauxite_dust +1 clay_ball →3 fire_clay_ball，ID `the_winter_rescue:create/mixing/none/0`；industrial chemical.js 同材料/产量但无显式固定ID | 保留。mixer 研究锁机械搅拌器/basin且有 transmission_machinery 前置，不能声称新玩家天然已有搅拌机。 |
| `kubejs/data/the_winter_rescue/recipes/pottery_recipes/fire_brick.json`：fire_clay_ball→refractory_brick，类型 charcoal_pit:pottery；ID **`the_winter_rescue:pottery_recipes/fire_brick`** | 保留公开，既有基础烧制替代路线；不加入新 Design。 |
| `recipes_types/immersiveengineering/blastfurnace.js` 索引2：fire_clay_ball→refractory_brick，100tick；ID **`the_winter_rescue:immersiveengineering/blast_furnace/2`** | 保留原工艺/旧设备要求，不加新成果锁。 |
| `recipes_types/immersiveindustry/kilns.js` 首项：4 kaolin_dust +4 bauxite_dust +1 clay_ball →3 refractory_brick，600tick、tickEnergy=32，类型 **immersiveindustry:car_kiln** | 由 event.custom 自动生成ID，源码没有稳定ID；不猜哈希、不为本项目改名或设锁。运行时目录核验产物即可。 |
| `recipes/shapeless.js`：4 refractory_brick +1 mortar→2 refractory_bricks；普通砖与high_refractory_brick是不同物品 | 组砖配方原样公开。新成果不包括高耐火砖/工业窑砖。 |
| `config/fhresearches/blast_furnace.json` | 原始ID `blast_furnace`、父项 mixer、4 fire_clay_ball 启动材料全部保留。必须保留成型效果 **8a9d01be7d574eaa**→`immersiveengineering:multiblocks/blast_furnace`、配方效果 **7a59b074753747e6**→产物`immersiveengineering:blastbrick`。 |

旧高炉的必需游戏线索 `7b0380fa39924e97`、4 invar_ingot线索 `8990547adf2b4f07`、1 refractory_bricks线索 `67c2d1728d8a4af1` 均不改；正式 `generator_T2`/`simple_machines` 仍以 blast_furnace 为父。新项目没有同名旧研究可替换，**不填 legacy_replacement，不授旧完成、不撤旧效果**。现有 blastbrick 配方还需要 high_temp板材、砂浆和耐火砖块；新普通砖供应不等于已解锁IE高炉。

源码权限核对：`ResearchHooks.canUseRecipe(team,recipe)` 可供自有炉调用；`RecipeManagerMixin_Research.getRecipeFor` 只在CRAFTING且存在craftingPlayer时过滤，其他类型直接匹配。现有机械合成/IE assembler适配不等于全部机器受管。只读依赖核对显示 `TilePotteryKiln` 调 `PotteryKilnRecipe.getResult`，IE `BlastFurnaceLogic.State.getRecipeForInput` 取cachedRecipe，II `CarKilnLogic.tickServer` 独立执行。因此不能只把上述旧ID放进Design就声称机器已锁。本方案避开这些未覆盖接点，旧世界没有新增权限损失，**无需任何一次性补偿**；不因为老玩家已造砖而伪造新实验历史。

## 2. 可达材料链与新炉默认参数

已核对：冻高岭土/冻铝土矿可经 `frostedheart:defrost/melt_kaolin`、`frostedheart:defrost/melt_bauxite` 营火解冻（200tick→4原料）；普通 kaolin_block/bauxite_block 在 shapeless.js 可拆4原料。树桩用 hammer_dig 把 bauxite 变 crushed_raw_bauxite；手磨把 crushed_raw_bauxite→至少2 bauxite_dust、kaolin→至少1 kaolin_dust。`new.js` 的树桩只需原木/石、磨盘只需石/木棍、stone_hammer用石/木杆/线，不用研究后的耐火砖。真实生成、采集工具手感、营火点燃和线材获取仍须正式包游玩，不把配方静态存在写成实测可达。

TWR新增公开手工坯料配方 **`the_winter_rescue:research/fire_clay_hand_mix`**：4高岭土粉+4铝土矿粉+1黏土球（9格无序）→ **1 `frostedheart:fire_clay_ball`**。它是现有物品，不另造“研究专用砖坯”；产率低于机械搅拌的3份，保留升级价值。现有木工作台入口可用，不要求3×3材料压入随身2×2。该原料会同时可用于既有陶窑：这是主动保留的替代玩法，不以堵旧路线证明研究价值。

新炉注册ID **`frostedresearch:controlled_furnace`**；公开制作recipe **`the_winter_rescue:research/controlled_furnace`**，`CCC / SFS / CCC`，C=clay_ball、S=sand、F=minecraft:campfire，产1。共6黏土球+2沙+1既有营火；不使用耐火砖/炉砖、铜铁、温度计或被锁设备。外观为小型试验炉，温度是炉内工艺玩法参数，不冒充世界热网或真实未烧黏土炉的耐温模型。

| 参数 | 首版可调整默认及操作规则 |
|---|---|
| 模式 | 试验：1 fire_clay_ball +1 minecraft:charcoal，400tick；生产：16 fire_clay_ball +2 charcoal，600tick。只接受这类坯料，不是万能窑。 |
| 温度 | 设定600–1400°C，50°C步进，默认1000°C；**1050–1150°C含端点成功**。首版无升温曲线，启动时立即采用锁存设定值，UI明确“本炉工艺温度”，不显示虚假动态实温。 |
| 产物 | 成功为真实 `frostedheart:refractory_brick`，试验1、生产16；窗外同数量 `frostedresearch:cracked_refractory_brick`，提示欠烧/过烧。碎砖无高耐火材料配方。 |
| 调温/重试 | 空闲时调整；启动锁存temperature、配方参数、run绑定。运行中调节仅改变“下一炉设定”，不重置本炉计时/改写已锁存结果。可取消，退回尚在内部的坯料一次，已扣燃料不退。 |
| 输出/保存 | 输入、燃料、输出三槽；开始前输出须为空，扣一次材料并存内部批次。满400/600tick只生成一次pendingOutput；输出受阻则等待，不再扣料/烧第二次，清槽后交付。卸载/离线暂停，保存剩余tick与pending，不补离线时间。 |
| 拆除 | 原炉掉落+未消耗库存；运行中仅退内部坯料、燃料不退；已完成掉真实pending成品、不同时退坯料。两种结算互斥，堵输出后拆炉不复制产物。 |

试验模式只能针对当前有效run的本任务使用，任务完成/研究结束后不能拿旧run持续免费生产；丢失样品可在任务未完成时重烧。首版手动启动每炉，不开放自有炉自动投料，避免额外自动化归属机制；同绑定队伍成员均可操作。学习生产成果后可手动批量生产，不依赖进行中run。试验与生产有独立模式，后者不能给试验样品伪绑定。开始后遗忘生产成果只阻止下一炉，已扣料的批次允许交付，避免中途吞料。

## 3. 想法入口、开始知识与提示分支

建议ID：想法 `the_winter_rescue:refractory_firing`、项目 `the_winter_rescue:refractory_brick`、Design成果 `the_winter_rescue:controlled_refractory_production`，understanding=[]。

- acquisition.required=[]。optional设三个有效物品观察条件：kaolin、bauxite、clay_ball（均可用object eq；需要扩展形态时由内容tag合并）。项目 start_all=想法+这三项，即获取想法的非必要知识仍是项目开始必需。开始不要求成砖、风道小游戏、旧blast_furnace或Create设备。
- triggers 按M2多个入口OR：①获取原料（kaolin/bauxite/fire_clay_ball合并item tag）；②观察任意耐火砖物品tag；③观察耐火砖方块tag；④观察直接配方含砖的产品。前三项复用 item_obtained/observation_completed 和 FieldCondition.TAG。概率/optional加成沿M2默认，合法联络作为确定性补充入口，不另设实验概率。
- 正式砖tag分别放在 `kubejs/data/the_winter_rescue/tags/items/research/refractory_bricks.json` 和blocks同路径：至少含普通/高耐火砖对应物品/方块，II refractory_kiln_brick可列为可选存在成员；是否纳入其它模组耐材只编辑tag。**只作触发/参考，不因纳入tag而允许控温炉烧出这些高阶材料。**
- 直接关系：FH新增一个小型 `RefractoryObservationIndex`，在配方/标签最终reload一次遍历可读取ingredients及确定结果的CraftingRecipe；某ingredient接受砖tag任一物品时收集其直接结果Item，若为BlockItem同时收集对应Block。只算一跳，不递归产品的产品。特殊机器/无法读取的配方由TWR `research/refractory_products_extra` item/block tag补充。现场观察按两集合O(1)查询，命中发自定义事件 **`frostedresearch:refractory_component_observed`**，IdeaAcquisition触发器可不带subject。不修改注册表tag、不新增OR表达式或FieldCondition运算符。
- 该索引的物品/方块匹配也注册ObservationUse，保证关联对象能入档；reload用现有registerObservationUse刷新KnowledgeDefinitions revision，失去用途时按既有休眠语义处理。新鲜采样仍可触发想法，即使来件已有重复记录。

复用M4两阶段：`prepare`说明原料与温度试验、无必需任务，ready后手动“进入烧制”；`firing`一项OBSERVATION。进入firing时读取已有砖观察并冻结路径，普通提示“先在1000–1200°C中部试验，建议1100°C”；参考路径提示“1050–1150°C可得到完好砖，建议1100°C”。只改method，不减料、不缩时、不改成品。参考砖**仅在stage variant.knowledge_all**，不放acquisition.optional或start_all；否则会误变成必须已有砖。采用两个按顺序的参考变体（item砖tag、block砖tag）及默认路径表达任选，沿M4顺序首命中，无通用OR树。中途补砖知识只影响尚未进入的阶段，已进入firing不变；普通路径本就能一次成功，不要求先烧坏。

关键任务例子（拟M4 schema）：

```json
{
  "requirement": {
    "id": "observe_fired_brick", "type": "observation", "title": "观察本队本次烧出的完好耐火砖",
    "observation": {
      "experiment": "frostedresearch:refractory_trial", "team_current_run": true,
      "condition": {"kind": "observation", "observation_type": "item", "conditions": [
        {"field": "object", "operator": "eq", "value": {"state": "known", "type": "string", "text": "frostedheart:refractory_brick"}}
      ]}
    }
  }
}
```

试验成功样写M3 `ResearchSampleBinding(team,run,requirement,experiment)` 到真实砖ItemStack，无owner。炉启动选当前firing任务并锁定绑定；不凭物品同名认为是试验样。ObservationSessions成功完成手持/容器item采样时，把**已核验的现场ItemStack**交给M4 observeResearchTarget，直接读取binding，匹配观察者当前队伍/run/任务。无需把binding暴露成知识条件字段、无需整份NBT入档。队友拿到同队样品可观察；普通砖/旧run砖/碎砖/笔记不算。观察后不消耗、不提交，真实砖保留可用；重复观察幂等。样品被用于合成消失则可重烧；不追踪合成后的后代产物。

## 4. 新成果的唯一权限目标及最少代码

**唯一受新研究管理的生产recipe：`the_winter_rescue:research/refractory_brick_batch`**，自有recipe type建议 `frostedresearch:controlled_firing`。Design.recipes只填此ID，不填物品ID、不填三条旧机器路线、不填blastbrick。炉与手工坯料配方公开，试验模式按run资格执行，批量模式每次开始必须 `ResearchHooks.canUseRecipe(teamId, recipe)`。

```json
{
  "type": "frostedresearch:controlled_firing",
  "input": {"item": "frostedheart:fire_clay_ball", "count": 16},
  "fuel": {"item": "minecraft:charcoal", "count": 2},
  "temperature_min": 1050, "temperature_max": 1150, "duration": 600,
  "result": {"item": "frostedheart:refractory_brick", "count": 16},
  "failure": {"item": "frostedresearch:cracked_refractory_brick", "count": 16}
}
```

输入/燃料/温窗/时长/成品按上述小codec，不建通用热工模拟器。试验用同类型第二条 **`the_winter_rescue:research/refractory_brick_trial`**，input/fuel/result/failure的count均改1、duration400，并加可选字段 `"experiment":"frostedresearch:refractory_trial"`。有experiment字段的是试验配方，只匹配当前OBSERVATION任务的同experiment；没有该字段的是需要Design的生产配方。这样TWR决定实验→配方映射，FH不硬编码TWR资源ID，也不改M4条件语法。炉UI明确试验/生产候选；试验无开放任务则不可启动，批量无有效成果则说明需要研究。

Design定义缺席但批量recipe仍在时，不能因TechnologyAccessProjection的unmanaged默认可用而全公开：自有炉在生产候选判定中先要求该recipe被当前有效Design声明为managed，再查canUseRecipe；缺内容只保留已扣料批次并说明生产定义未加载。无需修改全局默认策略。旧的各机器工艺依旧可用，不能把“禁用新炉批量”误当“全世界禁止烧砖”。

高耐火边界：自有炉recipe首版仅普通 refractory_brick/碎砖；不接炉温越高产物越高级的转换。高耐火砖仍需既有 magnesia/quicklime与Create heated mixing或工业窑，工业窑砖仍需alumina/hop_graphite；新Design不解这些机器/配方。允许普通砖为旧blast_furnace提供线索材料，旧游戏/研究/配方与成型效果继续独立判定。

**FH范围：** 控温炉block/BE/menu/CUI/三槽及存档、controlled_firing type/serializer、碎砖注册/资源；M4观察任务现场stack适配（复用M3 binding）；直接关系reload索引/专用事件/ObservationUse；复用ResearchHooks与现有技术投影。不要修改Charcoal Pit/IE/II生产逻辑，不新增LegacyResearchBridge类型或补偿回执。M4尚在实施，先核对它最终接口再接入，保留已有阶段冻结/多人归属语义。

**TWR范围：** 新想法/project/results、raw/brick/extra产品tags；三条公开制作/坯料与试验/批量recipe（上述固定ID）；中英文说明与实际获取引导。既有pottery、blastfurnace、kilns、mixer、blast_furnace研究及后继原样保留。更新living docs与新增diary由实施者完成；本调查仅新增本文。

## 5. 必要验收与证据边界

1. **材料与进度**：在新正式测试世界从矿物/冻矿、手锤/树桩/磨盘、营火、工作台走到fire_clay_ball及控温炉，无成砖/搅拌机前置；三个optional观察入档才开始，原料获取/砖item/砖block/直接制品四入口分别命中；普通路径可第一次成功。
2. **温控状态**：600/1400与1050/1150边界、1000/1200失败、运行中调下一炉/取消/输出堵塞/拆炉均不复制或吞失应保留产物；加载tick计时；独立进程重启恢复pending/binding/进度，未执行的重启不能用codec往返代替。
3. **观察与分支**：同队A烧B观察成功，不提交/不消耗；旧砖/碎砖/外队/旧run失败；观察重复或不入档不影响真实任务；进入前补参考有精确提示，进入后补参考不改当前路径。
4. **有用解锁**：研究前试验可用但批量不可用，成果入档后16砖批量可生产；来件不授生产权，遗忘禁止下一炉且不删除已投料批次；真实canUseRecipe、炉UI/JEI一致。新炉从不产high_refractory_brick或refractory_kiln_brick。
5. **既有世界与旧高炉**：三个旧制砖工艺仍执行；已有耐火砖/机器不丢权限，无补偿发奖；旧blast_furnace两效果/三线索/父项mixer、generator_T2/simple_machines下游完全保留。明确没有“新耐火砖研究完成即解IE高炉”。
6. **reload与性能**：直接配方索引正确识别Ingredient标签、BlockItem、重复/空结果；A含砖而B只含A时B不自动纳入；特殊机器只按补充tag；标签/配方重载、零在线启动后索引与观察用途刷新；观察时不遍历配方。仅检查新schema、定向服务端与正式包游玩，不重复整套无关迁移调查。

本次只读正式配方/研究、FH权限/观察源码与本地依赖（Charcoal Pit、IE、II），没有构建、客户端、Git或运行时recipe导出。自动生成ID的工业工艺明确未冒认稳定ID；新配方ID是实施决定。最终方案按主协调最新指示选择“新增有价值生产能力、保留旧工艺”，不需要假定旧玩家曾烧过砖来做无证据迁移。
