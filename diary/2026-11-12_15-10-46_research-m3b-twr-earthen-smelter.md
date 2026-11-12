# M3-B TWR 土制冶炼炉正式内容

- Time: `2026-11-12 15:10:46 +08:00`
- Author: `Codex / OpenAI GPT-6.1 Sol medium；TWR 正式内容实现`
- Status: `partial`：正式数据与静态验证完成，桥接和整合运行待验。
- Scope: TWR 土制冶炼炉定义、稳定配方 ID、中英文文案；FH 只新增本文和 [玩家路线](../docs/knowledge/earthen-smelter.md)。未改 FH 代码、既有 docs、README、总计划或协调记录。

## Completed

TWR 精确变更清单（下列路径均相对 TheWinterRescue）：

- 新增 `kubejs/data/the_winter_rescue/frostedresearch/knowledge/ideas/earthen_smelting.json`
- 新增 `kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/earthen_smelting.json`
- 新增 `kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/earthen_smelter.json`
- 新增 `kubejs/data/the_winter_rescue/frostedresearch/knowledge/results/earthen_smelter_design.json`
- 新增 `kubejs/data/the_winter_rescue/frostedresearch/knowledge/results/earthen_smelter_construction.json`
- 新增 `kubejs/data/the_winter_rescue/tags/items/research/alloy_kiln_minerals.json`
- 修改 `kubejs/server_scripts/src/recipes/shaped/change.js`：只将 alloybrick 产物的那条配方 ID 固定为 `the_winter_rescue:research/alloybrick`；保留原数组顺序、材料、4件产量和所有其他条目索引 ID。
- 新增 `kubejs/assets/frostedresearch/lang/zh_cn.json`、`en_us.json`：项目、想法、条件、任务与两成果的完整中英文文案。
- 修改 `kubejs/assets/immersiveengineering/lang/zh_cn.json` 并新增同目录 `en_us.json`：正式炉/炉砖显示名统一为土制冶炼炉/土制炉砖与对应英文。
- 修改 `kubejs/assets/twr_researches/lang/zh_cn.json`、`en_us.json`：仅统一旧 alloy_kiln 名称，方便搜索和承接显示。

## Decisions

- 不重复 FH 已有三条小炉配方。沿用3:1、1:3、2:2，4木棍400tick，本次成功样提交1件；中文明确“绘图台抄录台物件槽→返回研究页提交”，不使用实验台，不学习样品。
- 必要获取知识空，optional 为黏土球/沙物品观察；四个铜锡原矿/碎矿标签触发。未设自定义 chance，采用 M2 配置默认（0.15基础、每项optional加0.1）；只为本想法配置零输入联络提供稳定入口，不添加 initial 授予。
- 开始必须想法+黏土球观察+沙观察。单任务严格使用 submit_intact/sample，experiment=frostedresearch:trial_kiln，item=frostedresearch:fired_trial_kiln。
- 两成果分别是 Design 与 Construction，understanding均空；legacy_replacement 完整对应 research=alloy_kiln、350c485a4b92425c→design、1bb68ce233ef4f21→construction、partial_idea→earthen_smelting。原旧 JSON 和 nonce 未改；桥接生产实现由另一对话负责。
- 未找到匹配 alloy_kiln/alloybrick/alloy_smelter 的现有 FTB 任务条目，未改无关任务书；新项目三步正文及独立玩家文档负责说明路线。
- 已检索 TWR 全树和适用父目录，未发现 AGENTS.md；遵循 FH AGENTS 和本次授权范围。无 Git 操作，未触碰已暂存 config/.gitignore 或 .workbuddy。

## Validation

- `node --check kubejs/server_scripts/src/recipes/shaped/change.js` 通过。
- Node 静态脚本解析6份新增数据/标签和6份相关语言 JSON；核对想法/项目/两成果/零输入联络引用、中英文键完整一致、观察对象、sample字段、两成果类型/目标及与旧JSON原effect nonce映射，全部通过。
- Node VM 用最小 KubeJS shaped/Item stub 执行当前 change.js，再在内存恢复原索引 ID 表达式比较：116条配方中仅1条 ID 变化，目标为 alloybrick；116条的产物数量、图案、材料均不变，其余115条 ID 不变。此为脚本生成逻辑验证，不冒充实际 KubeJS RecipeManager。
- 四矿物标签均与 `recipes_types/immersiveengineering/alloy.js` 字面输入一致；排除石/沙/配料/金属锭。材料链核对冻黏土解冻、腐木→木棍、营火、绘图台铜板、窑坑铜锡→青铜、树桩制板、结构金属/高级线材标签及工程锤配方。未证明真实世界已获得这些材料。
- 未运行 Gradle、构建或客户端，未复制/覆盖 Prism、运行实例或玩家世界。FH 桥接代码正在并行工作，不据此报告桥接测试通过。

## Remaining

集成阶段需用实际正式数据加载器/RecipeManager检查定义、标签成员、稳定ID与完整炉砖配方覆盖；客户端检查双语文案、JEI、真实抄录台提交、两成果学习及配方/成型权限；桥接验证旧完成/部分进度与新完成；最后在正式包新测试世界验证材料、砖/窑坑/早期工具、三配比操作和下游生产。提交由主协调统一处理。

## 交接文案修正

主协调复核后修正新玩家路线文档，以及 TWR 新增中英文语言文件中的想法、项目和建造成果正文：本项目想法/成果的 understanding=[]，M2 行为/联络和研究完成经 produce 正常自动学入档，完成研究正常立即取得配方与成型两权限；观察保留记录后需手动学习，只有实际留在来件中未学的条目才另行手动学习。不再引导玩家寻找正常流程不存在的构想/成果来件。两份语言 JSON 重新解析通过，并断言每种语言只改三项正文值、全部键与 ID 保持不变；未改定义、配方、生产代码或其他工作者文档，未运行构建/客户端。
