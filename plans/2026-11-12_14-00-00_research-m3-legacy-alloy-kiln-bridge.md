# M3：alloy_kiln → earthen_smelter 最小衔接决定

- Time: `2026-11-12 14:00:00 +08:00`
- Authors: `Codex / OpenAI，接口只读调查`
- Status: `completed`（调查交付；以下接口待 M3 实现）
- Scope: 一项旧研究与新项目的完成承接、权限、入口替换；不实施81项通用迁移。
- Related: [M3 内容卡](2026-11-12_13-40-36_research-m3-trial-kiln-content-card.md)、[总计划 M3/M6](2026-11-11_23-38-07_research-system-playable-development-plan.md)、[M0 交接](../diary/2026-11-12_12-40-00_research-m0-baseline-handoff.md)。

## 已核实的事实

源码下文相对 `src/main/java/com/teammoeg/frostedresearch/`。

- 真实旧 ID 是字符串 **`alloy_kiln`**，不加命名空间；`ResearchCatalog.load` 从 JSON 文件名去掉 `.json` 得到 ID。TWR `config/fhresearches/alloy_kiln.json` 无父项；本次检索正式目录没有其他项目以它作父项，故后继兼容必须用定向 fixture 验证，不能宣称当前存在某个正式下游。
- `data/TeamResearchData.CODEC.researches["alloy_kiln"]` 保存 `ResearchData`：flags 中的 finished 是完成事实，active 表示已交启动材料；committed 是研究点；clueData/effectData 按 nonce 保存。两效果：`350c485a4b92425c` = 炉砖配方；`1bb68ce233ef4f21` = `immersiveengineering:multiblocks/alloy_smelter` 成型。`TechnologyAccessResolver.addLegacySources` 目前同时要求 finished 与对应 effectData=true。
- `getExistingData(Research)` 是不创建记录的查询；建议加 String 重载以读取定义暂缺的旧记录。不要用会创建空状态的 getData(String) 做迁移筛选。旧 `legacyIds` 仅处理重命名，不负责转知识。
- 炉砖配方由 TWR `kubejs/server_scripts/src/recipes/shaped/change.js` 数组索引生成，当前是 **`the_winter_rescue:minecraft/crafting_shaped/change/9`**（源码序号核对，未运行配方管理器）。旧 EffectCrafting 按产物筛配方；新 Design 需实际 recipe ID。建议 M3 把这一条显式固定为 `the_winter_rescue:research/alloybrick`，不改材料/产量，其余数组项 ID 不动；再用运行时配方列表验证新 Design 覆盖全部预期炉砖配方。
- 已实施 M1 的 `KnowledgeService.projectAccess` 和 `KnowledgeView` 仍用真实 ResearchHistory.completed 判新项目完成；只授予两个成果不能让旧玩家项目自动显示已完成。不能为解决 UI 而伪造新试烧/run。

## 推荐决定：数据映射 + 小型桥接，旧事实不改写

在新 ProjectDefinition 增加可选 `legacy_replacement`，仅支持本次需要的静态映射。放在 TWR 的 `projects/earthen_smelter.json`，不硬编码整合包 ID 到 FH：

```json
"legacy_replacement": {
  "research": "alloy_kiln",
  "effects": {
    "350c485a4b92425c": "the_winter_rescue:earthen_smelter_design",
    "1bb68ce233ef4f21": "the_winter_rescue:earthen_smelter_construction"
  },
  "partial_idea": "the_winter_rescue:earthen_smelting"
}
```

映射只在项目、两个启用成果及其类型/目标均已加载可用时生效；完整新包缺席时保持旧系统原样，缺一项定义时给明确加载诊断并保留旧入口可用，不把玩家夹在两套残缺流程之间。映射需要在最终配方/标签就绪后刷新。不要求旧 JSON 存在才识别旧存档，但首版发布保留它作兼容定义。

1. **旧已完成 → 新成果。** finished=true 即承认整项完成，授予两个成果，无须再交观察或实验；不要求旧效果都已经领取。通过 `KnowledgeService.learn` 入档、使用 `AcquisitionSource("legacy_research:alloy_kiln", …)`；这两个成果 understanding=[]，无需冒用 COMMAND 授予，也不调用 `TeamResearchService.grantResult` 的旧物理原型分支。finished=false 但存在单项已授效果时只承接对应成果，不凭一个效果宣告整项完成；保留原始标记供诊断。
2. **只存小型迁移回执。** 复用持久化 `TeamResearchData.variants`，新增命名空间子 Compound `frostedresearch:legacy_replacements`，按新项目 ID 保存 `imported_results`（成功入档的 ID 集合）、`completed_credit`、`partial_idea_granted`。completed_credit 只来自旧 finished；它是完成承接凭据，非新实验记录。每个结果 SUCCESS/已在档案才写对应回执，取消/定义缺失下次登录或重载补做，不在每 tick 重试。回执与知识状态均走既有队伍保存/同步。已入档后被主动遗忘不在每次登录强行补回；旧已获得权限仍按原旧来源保留，UI 保持“旧研究承接完成”，不要求重做实验。合并时对这一个子键取集合/布尔并集，与 KnowledgeService.merge 同步调用，不另造迁移数据库。
3. **新完成 → 旧下游。** 不写旧 finished、不伪造旧线索/效果领取。增加桥接查询 `isPrerequisiteCompleted(team, legacyId)`：旧 raw finished **或** 活跃映射下新项目有真实 completed run 且两个成果都有效入档。替换 `TeamResearchData.commitItem` 和 `Research.isUnlocked` 的父项直接 getData().isCompleted 判定；客户端读取服务端的等价完成投影。这样新成果只在来件时尚不开放旧后继，补学后开放；遗忘新成果时失去这条新来源，仍有真实旧完成者不受影响。外部仅获两个成果可使用设备，但不伪装为完成研究来解旧父项，维持“知识权限”与“完成进度”的区别。
4. **权限继续合并来源。** 新成果仍由 TechnologyAccessResolver 处理；旧 completed+effect 标记仍是独立允许来源。现有 resolver 是来源并集，不是要求新旧两套都完成，不需另叠一层锁。不要调用 `setResearchFinished/grantEffects` 来桥接新完成：它会重放旧效果/完成通知，还会把新权限变成遗忘后依然存在的旧永久来源。原始旧状态与原始新 run 各保留，不相互篡写。
5. **部分旧进度。** active=true、committed>0 或任一已完成 clue 的旧队伍，保留全部原始点数/线索/已交材料状态；一次性授予新想法（使用普通学习/来件流程），便于直接发现新项目，随后只需现行观察前置与一次成功样。旧点数不折算为烧炉/提交，不发完成成果；不退曾消耗材料/洞察，避免猜测投入来源。回执只在想法成功入档或确实留在来件后置位。UI 说明“已有思路已承接，需验证新试样”。这是首版的进度折中，未发现与已确认玩法冲突。

## 旧入口、服务器残留与显示

**保留旧 JSON，运行时按活跃映射停用旧研究入口。** 不修改磁盘配置、不调用 Research.delete（它会重置队伍数据/改图）、不依赖整合包升级删除文件。服务器残留原版 alloy_kiln.json 也被同一映射取代；新包缺席/卸载时映射消失，原旧入口、旧部分进度与效果恢复正常。卸载新包不把新实验倒写成旧进度，曾只走新路线的队伍可走恢复后的旧路线；不承诺内容包卸载等于反向迁移。

新增单一 `isReplaced(legacyId)` 判断：旧 UI 列表/详情显示“已由土制冶炼炉接替”并可转新项目；普通激活/选中/投点/线索完成路径不再推进旧研究。服务端覆盖 `commitItem`、`setCurrentResearch(team,…)`、`doResearch`、`setClueCompleted/checkResearchComplete`，不能只设 isHidden 或 inCompletable；两者无法阻止已经选中的研究继续完成。重载/登录遇到旧 activeResearchId 时解除线索订阅并清当前选择，保留 ResearchData.active/committed/clueData；从恢复入口重新选中即可续旧进度。管理命令保留显式行政语义，不另设一套权限。

新 UI 添加 `completion_source=legacy`（或等价小字段），completed_credit 项目即使没有想法也列入项目列表，显示“旧研究承接完成”，禁用重复启动；不生成 ResearchHistory 或虚构试样来源。快照另给一份小型 `legacy_replacements` 列表（旧 ID、新项目 ID、effective_completed）供旧 UI/父项检查，避免客户端自己推测。真实新 run 与旧完成承接并存时保留历史，旧承接优先免除重复实验；进行中新 run 可显示为“不再需要完成”，保留已有记录且停止继续提交，不自动伪造完成或清空它。

## Sol 执行范围与定向验收

- **FH**：ProjectDefinition/KnowledgeDefinitions 加映射；一个 `knowledge/LegacyResearchBridge` 承担导入/回执/查询；TeamResearchData 加只读字符串查询与旧推进接点、Research 父项查询；KnowledgeService.projectAccess/merge、KnowledgeView、KnowledgeRuntime 的登录/最终重载接点及真实完成/成果学习后刷新；旧列表/详情和知识 UI 消费小投影。桥接变更后调用 TeamResearchService.sync 使权限投影失效；不要在投影读取时写档或授予。离线队伍下次加载/登录补做；重载及时处理在线队伍。与正在实施的 M2 共用 KnowledgeService/Runtime，合入时保留其事件入口。
- **TWR**：新项目中的映射、两个成果、上述单条稳定炉砖 recipe ID；保留旧 JSON 和效果 nonce。实施跨仓修改前重新核对适用 AGENTS；本次只读检索未找到 TWR 仓内 AGENTS，不代表所有父目录都已复核。
- **验收**：①旧 finished、旧仅单效果、旧部分进度、新队伍四种档；②重复登录/reload/重启及单结果授予失败后补做，不重复来件/回执；③旧完成玩家两权限可用、新项目承接完成且没有伪造 run；④新项目完成→成果学习→fixture 旧后继开放，同时 recipe 与成型一致；⑤原样残留服务器 JSON、已选旧研究仍不能走旧路线；⑥移除新数据包后原旧进度可续，装回幂等；⑦新成果遗忘不被桥接永久权限掩盖，真实旧权限仍保留；⑧两队合并回执/成果不重复，已有新 run 不被删除。另用实际 RecipeManager 核验固定配方 ID 与 IE 成型目标，自动断言不代替生存游玩。

不增加阶段引擎、进度百分比换算、旧奖励重放、81项自动推断、通用迁移脚本语言或安装目录清理器。本次仅新增本文；未改生产/既有文档/TWR，未 Git 操作、构建或发送其他对话消息。结论来自当前源码与正式数据只读核对，以上迁移场景尚未执行。
