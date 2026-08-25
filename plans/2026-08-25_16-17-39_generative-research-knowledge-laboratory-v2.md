# 生成式研究系统 V2：从现有研究出发

- Time: 2026-08-25 16:17:39 +0800
- Updated: 2026-08-25
- Authors: Codex（OpenAI，系统、玩法与工程架构）；项目所有者提供目标、批评与取舍
- Status: in-progress；Phase 1 五类成果基础层已于 2026-08-25 实现，Phase 2–6 待继续
- Scope: FrostedResearch、观察与想法、证据板、居民讨论/计算/实验、实验台、五种研究成果、通用原型升级、数据包创作、旧研究兼容
- Related: [研究讨论](../discussion/research_conversation.md)、[V0](2026-08-25_10-30-52_player-interactive-generative-research-system-v0.md)、[V1](2026-08-25_10-30-52_player-interactive-generative-research-system-v1.md)、[现有研究文档](../docs/research/README.md)

## 结论

V2 保留此前已经成立的研究过程：

> 世界、人物和城镇提供记录；证据板帮助玩家汇聚灵感；居民可以讨论、计算和做实验；玩家在实验台周围搭建真实空间并决定是否接受结果。

但研究成果回到当前游戏已经使用的语言。MVP 只有五种：

| 成果 | 玩家完成研究后得到什么 | 与旧系统的关系 |
|---|---|---|
| Finding（发现） | 一条能在档案、观察、提示或对话中使用的世界认识 | 唯一新增的知识成果 |
| Design（设计） | 明确列出的新配方 | 取代新内容中的 recipe Effect |
| Construction（建造） | 明确列出的多方块成型权 | 取代新内容中的 multiblock Effect |
| Procedure（方法） | 明确列出的方块右键使用权 | 取代新内容中的 use Effect |
| Prototype（原型） | 一件有来源和 serial 的实体试作品；安装后只影响该设备 | 唯一新增的工程成果 |

不在 MVP 中建立 DesignStandard，不要求 Design 再搭配“制造 Procedure”，也不把 Construction 继续拆成结构规格、施工方法和应用规则。一个研究可以同时产生多个成果。简易高炉本来就同时解锁耐火砖配方和高炉成型，V2 仍然一次给出 Design + Construction。

研究过程中的“观察方法、讨论方式、计算方式和实验步骤”统一叫 Protocol。Construction 只表示学会形成明确多方块；Procedure 只表示学会右键使用明确方块。

首批内容不再用白幕、水相变或一套完整 T1 工业线证明整个系统。它从五项现有研究开始：

1. 岩石的性质：以 Finding 为主；
2. 简易高炉：以 Construction 为主，同时保留耐火砖 Design；
3. 进阶真菌养殖：以 Design 为主；
4. 能量核心再利用：展示一项研究同时产生 Design、Construction 和普通物品奖励；
5. 能量塔效率提升 1：以 Prototype 为主。

这五项覆盖自然观察、现场调查、材料试验、生物培养、居民计算、工程台架和真实设备安装，同时不要求先发明新的世界主题。

## 设计原则

1. **研究过程比进度条重要。** 玩家做的是记录、连接、询问、委托、搭建、运行和判断，不是往研究点池里填数字。
2. **成果必须立即可解释。** 玩家能用一句话回答“我知道了什么”“我会搭什么”“我能造什么”“我手里多了什么”。
3. **不强制所有研究从玩家自己的 Idea 开始。** 难民、居民、遗迹、任务和器物可以直接带来 Idea、Finding、Design、Construction、Procedure 或 Prototype。
4. **居民不是灵感生成器。** 讨论、计算、实验是三种平级劳动，都占用居民、材料和时间。
5. **实验仍然是真实 Minecraft 活动。** 玩家要放实验台、准备样品、满足环境和装置要求；数学与采样细节由 Protocol、居民或机械计算器处理。
6. **不是所有研究都需要实验室。** 岩石调查可以靠现场记录与计算；高炉和真菌研究需要实验台；塔升级还需要安装到真实设备试用。
7. **现有机制决定可声称的结论。** 当前探矿工具实际扫描附近矿石，因此“岩石的性质”首版只讨论可用的勘探迹象，不虚构尚不存在的地层化学或矿脉生成学。
8. **一项研究可以给多个结果。** 这是当前 Effect 数据的真实形态，不再用复杂的知识包类型包装它。
9. **Prototype 是实体，普通奖励不是 Prototype。** 只有具备 serial、兼容 host 和安装后局部效果的物品才是 Prototype。
10. **普通作者只写稳定内容数据。** 公式、世界采样和机器内部字段由注册过的 provider/profile 负责；JSON 不直接读取 NBT 或 Java 字段。

## 玩家只需要学会的词

| 玩家用语 | 含义 | 常见动作 |
|---|---|---|
| 记录 | 亲眼所见、别人所说、报告所写或样品所代表的一件事 | 归档、追问、钉到证据板 |
| 想法 | 值得继续追查的规律、解释或改法 | 讨论、计算、调查、实验、搁置 |
| 研究 | 围绕一个想法或外来知识打开的工作页 | 选择下一件事 |
| 实验 | 在实验台及其周围真实完成的一次试验 | 准备、运行、封存 |
| 发现 | 团队现在能够使用的一条世界认识 | 查看、识别、引用 |
| 设计 | 一张已经能够照着制造的配方 | 在 JEI 查看、合成 |
| 建造 | 已经学会的明确多方块成型办法 | 成型 |
| 方法 | 已经学会的明确方块使用办法 | 右键使用 |
| 原型 | 一件尚未量产、但可以真实安装试用的物品 | 安装、拆除、继续观察 |

“证据”“候选解释”和“实验建议”可以出现在界面文本里，但不要求玩家理解 Claim、Obligation、revision 或 ApplicationRule。

## 玩家循环

~~~mermaid
flowchart LR
    S["世界、难民、居民、任务、器物"] --> R["记录或直接知识"]
    R --> B["证据板：汇聚灵感"]
    B --> I["想法 / 研究工作页"]
    I --> D["讨论"]
    I --> C["计算"]
    I --> E["调查或实验"]
    D --> I
    C --> I
    E --> I
    I --> F["Finding"]
    I --> G["Design"]
    I --> C2["Construction"]
    I --> P["Procedure"]
    I --> O["Prototype"]
    O --> H["真实设备安装与试用"]
    H --> I
~~~

这不是固定顺序。一个完整难民手册可以直接给 Design、Construction 或 Procedure；一件遗迹原型可以直接拿去安装；岩石调查可以没有正式实验；一项工程研究也可以同时给 Design 和 Construction。

工作页在任何时刻只展示：

1. 当前想弄明白或改变什么；
2. 一个推荐行动；
3. 至多两个替代行动；
4. 每个行动需要的地点、物品、居民或设施；
5. 完成后会得到“记录、讨论纪要、计算报告、实验记录”中的哪一种。

## 知识怎样进入研究

### KnowledgeOffer

世界来源先生成玩家可感知的 KnowledgeOffer。玩家亲历、交谈、阅读、拾取、领取报告或接受教学后，它才进入团队研究档案。

Offer 的载荷只需支持：

~~~text
Observation
Idea
Finding
Design
Construction
Procedure
Prototype
~~~

每条 Offer 保存来源、人物或地点、时间、适用对象，以及它是亲历、证词、文献、教学还是实体物品。

### 首批来源

| 来源 | 玩家看到什么 | 可能给什么 |
|---|---|---|
| 玩家亲历 | 发现矿石、使用菌床、看到塔燃料下降、拾到核心 | Observation、Idea seed |
| 主动记录或取样 | 对方块、设备、样品或地点使用笔记/工具 | Observation、specimen ref |
| 未招募难民 | 交谈、展示相关物品、交换知识 | Idea、Finding、Design、Construction、Procedure，少量情况下给 Prototype |
| 已招募居民 | 岗位报告、访谈、研究所劳动 | Observation、讨论纪要、计算报告、实验记录 |
| 城镇岗位 | 矿场、食物、物流、锅炉或塔运行的压缩报告 | Observation、问题报告 |
| 遗迹与任务 | 旧图纸、操作手册、损坏器件、船上档案 | 任意成熟度知识或普通物品 |

难民不在入队时把全部知识自动并入团队。玩家必须交谈、让其进入相关岗位、邀请其参加讨论，或接受其教学。若难民已经掌握完整 Construction、Procedure 或 Design，玩家可以直接获得它，不必把已有知识重新演一遍发现流程。

当前招募会创建新的 Resident UUID，因此实现时必须把难民 NBT 中的稳定 person knowledge 在招募成功事务内显式转移；失败时不删除、不重抽。

## 证据板与“汇聚灵感”

绘图台继续使用现有 ResearchGame 的纸牌局：9×9 棋盘、可动牌、配对消除、顺序收束、纸墨消耗和局中保存都保留。

玩家钉上 2–5 份记录、证词、样品或失败报告，再选择：

- 我想弄明白它；
- 我想改进它。

如果内容只有一个明显方向，界面直接开始，不多问一次。

牌面显示对象、条件、变化、结果。跨来源配对时显示人话联系，例如：

- 同一地点，发现了不同材料；
- 相同材料，在不同温度下表现不同；
- 塔保持工作，但燃料下降得很快；
- 菌床能培养真菌，但携带和补热都不方便；
- 一块耐火材料在普通炉火中没有损坏。

纸牌局不会生成隐藏真理。topic 只在当前证据支持的 IdeaCandidate 中揭示 1–3 张想法卡。玩家点击“记下这个想法”后才创建研究；若相同 Idea 已存在，新记录只补充来源。

难民、居民、文献或器物直接给出的 Idea 可以跳过纸牌局。直接给出的 Finding、Design、Construction、Procedure 或 Prototype 则可以跳过整个想法阶段。

## 研究工作页与最小缺口编译

后台仍保留证据来源和结果关系，但首版只识别五种缺口：

~~~text
NEED_OBSERVATION
NEED_DISCUSSION
NEED_CALCULATION
NEED_EXPERIMENT
READY_TO_DECIDE
~~~

每个注册的研究 resolver 根据当前记录返回一到三个行动建议。它只说明还缺什么，不替玩家开始任务，也不替玩家接受结论。

例：

~~~text
当前想法：改善送风也许能减少能量塔燃料消耗

推荐：把一段正常运行记录交给居民计算
也可以：请有锅炉经验的人讨论
也可以：先记录一次断供前后的塔状态
~~~

人员、设施、材料和当前世界条件由 ActionResolver 实时检查。居民换班或材料不足只改变“现在能不能做”，不重编知识关系。

普通内容作者不写义务 DAG，也不写布尔脚本。resolver/profile 是有界的注册类型，例如“两地点比较”“材料耐热试验”“培养前后对比”“设备运行前后比较”。

## 研究所：讨论、计算、实验平级

### 研究所作为城镇工作建筑

新增 frostedheart:research_institute，进入现有 TownStaffingPlan。玩家提高研究所用工，就会少一个矿工、猎人或物流员。

研究所只有三列工作：

| 工作 | 玩家提交什么 | 成本 | 产物 |
|---|---|---|---|
| 讨论 | 当前想法、2–5 份资料、需要的岗位或知识背景 | 通常 2 名居民、1 班次、纸墨 | 候选解释、证词、设计建议、实验建议 |
| 计算 | 一组记录和一个人话目标 | 1 名居民若干班次，或机械计算器动力；纸墨 | 比较、排序、规格、物料核对、前后差异 |
| 实验 | 一个实验建议、实验台和样品 | 实验员班次、样品、耗材、实际环境与装置 | 实验记录、异常和人话结论 |

居民讨论不会读取隐藏答案。岗位经验和私人知识决定能提出什么；智力与教育决定能处理多少来源和多复杂的计算。它们不变成“正确率”或研究点。

计算也不让玩家填写公式。玩家只选择“比较这两处”“核对材料”“比较塔改装前后”等目标。CalculationProfile 处理单位、采样窗口和异常，先输出人话摘要，详细数值默认折叠。

三类任务统一经历：

~~~text
QUEUED
→ WAITING_STAFF / WAITING_SUPPLY / WAITING_FACILITY
→ WORKING
→ AWAITING_REVIEW
→ APPLIED / ARCHIVED / CANCELLED
~~~

一名居民一天只能为一张研究工作单贡献一个班次。完成的讨论纪要、计算报告和实验记录都停在“等待玩家审阅”，不会自动发布成果，也不会自动排下一项研究。

机械计算器执行同一种 CalculationProfile，以 Create 动力代替居民班次；Create 缺席时居民路径完整可用。

## 专属实验台与实验空间

### 玩家看到的实验

新增 frostedresearch:experiment_table。实验台不是通用科研表单，而是一张固定准备屏：

| 检查栏 | 示例 |
|---|---|
| 空间与环境 | 封闭工作间、最低体积、当前温度、火源安全距离 |
| 装置 | 样品架、加热位、记录器、动力、待测部件位 |
| 人员与供给 | 玩家值守或居民班次、样品、纸墨、燃料、试剂 |

玩家选择一条作者写好的人话路线，然后：

1. 在实验台周围搭空间并放装置；
2. 放入真实样品和耗材；
3. 点击重新检查；
4. 选择亲自值守或排入研究所；
5. 运行中处理缺料、温度漂移、断电或错误样品；
6. 封存结果并回到研究工作页。

实验台首版固定为 4 个样品槽、4 个试剂槽和 1 个待测部件槽。protocol 可以给槽改名和过滤，但不能动态创建任意数量菜单槽。

### 空间不是固定多方块

实验以有限能力组合要求空间：

- 封闭空间、面积和体积；
- 当前真实温度和光照；
- 静态工作面、样品架与储物设施；
- 由 adapter 提供的实际加热、动力、记录或旋转能力。

玩家可以用不同房型满足要求。空间扫描只在重新检查、结构变脏或低频刷新时发生；运行中只读取缓存的空间和少量真实采样点，不逐 tick flood-fill。

不是每项研究都要实验台：

- 岩石的性质以现场取样和对照调查为主；
- 简易高炉使用材料加热试验；
- 真菌养殖使用培养试验；
- 能量核心使用带动力的部件台架；
- 塔效率原型先在实验台制造，再安装到真实塔上试用。

### Protocol 与 Procedure 的边界

Protocol 是研究过程定义，例如：

- 对两个地点做盲测并比较后来发现的矿物；
- 加热一份耐火材料样品；
- 对两份真菌培养基采用不同补热方式；
- 给能量核心做低功率通电检查；
- 比较塔改装前后的有效运行时间与燃料装入记录。

Construction 与 Procedure 都是最终成果：Construction 只表示玩家已经学会形成某个多方块，Procedure 只表示玩家已经学会右键使用某个方块。二者不共用数据类型，也都不与 Protocol 共用数据类型。

## 五种成果与三条直接消费路径

### Finding

Finding 是带来源和适用范围的团队认识。它完成后直接进入 KnowledgeProjection。

Finding 可以：

- 在档案中形成可查条目；
- 让笔记、观察工具或居民报告显示更明确的解释；
- 给某种现象、材料或状态命名；
- 开放相关问法、提示或后续研究入口。

Finding 不解锁配方，不解锁多方块成型，也不增加设备数值。某项研究若还要给配方，就在 results 中再列一个 Design。

### Design

Design 就是一组明确 recipe ID。团队取得 Design 后，这些配方立即进入 TechnologyAccessProjection，JEI 和真实执行端都按同一集合判断。

Design 不再要求另一份“制造 Procedure”。如果一个研究产出培养箱 Design，玩家得到的就是培养箱配方。

新内容必须使用稳定 recipe ID，不能再按输出物模糊地锁住未来所有同输出配方。旧 EffectCrafting 的动态行为只在 legacy 兼容层保留。

### Construction

Construction 就是一组非空、明确的可形成多方块 ID。团队取得 Construction 后，对应权限立即进入 TechnologyAccessProjection。

Construction 不表示结构规格层、居民施工熟练度或设备数值。简易高炉 Construction 就是“可以形成 IE 简易高炉”。

### Procedure

Procedure 就是一组非空、明确的可右键使用方块 ID。团队取得 Procedure 后，对应权限立即进入 TechnologyAccessProjection。

Procedure 不表示实验步骤、维修手册或居民熟练度，也不提供效率加成。机械计算器 Procedure 就可以是“可以右键使用机械计算器”。

### Prototype

Prototype 是一件动态实体物品，至少保存：

- prototype profile ID；
- serial；
- owner team；
- 外观 profile 与材料；
- 当前是背包中、实验台中还是安装在哪个 host。

Prototype 不进入全队技术权限。只有它真实安装到兼容设备后，设备的 InstalledContributionSnapshot 才包含它的局部效果。

普通物品奖励不是 Prototype。进阶真菌养殖赠送的一份菌床、能量核心再利用领取的 energy_core，都仍是普通奖励；它们没有 serial 和局部安装效果。

### 一项研究可以给多个结果

成果不互相转换。研究直接列出它给什么：

~~~text
岩石的性质
→ Finding：规范勘探迹象可以提示附近矿物
→ Design：铜探矿镐配方

简易高炉
→ Design：高炉砖配方
→ Construction：形成 IE 简易高炉

进阶真菌养殖
→ Design：培养箱配方
→ 普通奖励：一份棕色蘑菇菌床

能量核心再利用
→ Design：T1 核心配方
→ Construction：形成 T1 能量塔
→ 普通奖励：能量核心

能量塔效率提升 1
→ Prototype：受控进气模块
~~~

没有 DesignStandard，也没有“先把 Finding 编译成 Design”的隐藏步骤。研究 resolver 只判断玩家是否已经得到这项研究定义的结果。

### 三类投影到底投影什么

投影只是由权威数据重建的查询结果，不是第六种成果，也不是成果之间的转换。

| 内部读模型 | 输入 | 输出 | 消费者 |
|---|---|---|---|
| KnowledgeProjection | 团队已经取得的 Finding、Finding scope、当前被观察对象 | 当前可显示的档案、识别、提示和对话条目 | 笔记、档案、HUD、居民报告、剧情 |
| TechnologyAccessProjection | 团队已经取得的 Design、Construction、Procedure，以及 legacy 权限 | unlockedRecipeIds、formableMultiblockIds、usableBlockIds | JEI、配方执行、多方块形成、RightClickBlock |
| InstalledContributionSnapshot | 某个 host 里真实安装的 Prototype 和该 host 当前状态 | 这台设备当前生效的类型化局部贡献 | T1、以后实现升级接口的设备 |

最窄查询接口是：

~~~text
isRecipeUnlocked(team, recipeId)
canFormMultiblock(team, multiblockId)
canUseBlock(team, blockId)
resolveInstalledContributions(host)
~~~

每个答案附带来源 ID，方便 UI 显示“来自：进阶真菌养殖”或“来自：已安装的受控进气原型”。除此之外不需要 CapabilityManifest、ApplicationRule、RuntimeApplicationSnapshot 或 AppliedProcedureBinding。

## 五种成果的最小研究与一个复合案例

以下不是概念示例，而是 V2 的首批可玩内容。每项都从当前 research JSON、现有物品和现有 Effect 出发。

### A. Finding：岩石的性质 → 岩石与矿迹

#### 当前基础

- 旧 ID：geology_understanding；
- 当前线索：铜锭；
- 当前成本：16 个 forge:stone；
- 当前效果：解锁 frostedheart:copper_pro_pick 的输出匹配配方；
- 当前真实机制：ProspectorPick 与 GeologistsHammer 扫描玩家点击位置附近的 FHTags.Blocks.ORES。

因此首版 Finding 不能宣称已经存在真实地层化学。它只声称：

> 在已测试的距离和工具条件下，规范取样与敲击得到的勘探迹象，能够帮助判断附近是否存在矿物。

#### 最初想法从哪里来

至少提供三条入口，任意一条都能开始：

1. 玩家开采铜矿时，用笔记记录矿石以及它周围的普通岩石；
2. 矿场居民提交“某处普通岩石附近反复出矿”的岗位报告；
3. 有勘探背景的难民给出一份标记过的岩样记录，或直接传授外来 Finding。

玩家把“铜矿位置”“附近岩石”“另一处没有发现矿物的岩石”钉到证据板，纸牌局可以生成 Idea：

> 普通岩石中的勘探迹象也许能提示附近矿物，而不必先看见矿石本身。

#### 研究玩法

这项研究首版不要求实验室：

1. 玩家在两个地点建立取样记录；
2. 先由系统封存一次勘探结果，再由玩家实际挖掘验证；
3. 玩家可手工在绘图台整理，或委托居民/机械计算器比较两组“迹象—后来发现”；
4. 报告只给“吻合、没有吻合、样本不足”，不让玩家算概率；
5. 玩家审阅后形成 Finding，或继续增加第三处样本。

实现时从 ProspectorPick / GeologistsHammer 抽出只读 OreProspectingModel，让现场工具和研究记录使用同一附近矿物事实；不能另写一个只为研究服务的假扫描。

#### 直接结果

- Finding：frostedheart:prospecting_signs_indicate_nearby_ore；
- KnowledgeProjection：地质档案、样点记录解释、矿场居民更明确的勘探报告；
- 附加 Design：铜探矿镐的精确配方 ID，保留当前旧研究的实际回报。

若难民直接给 Finding，玩家可以立即看懂相应记录；若只给一张岩样记录，仍需证据板或现场比较。Finding 和铜探矿镐 Design 是同一研究的两个并列结果，不是 Finding 自动变成配方。

### B. Construction：简易高炉

#### 当前基础

- 旧 ID：blast_furnace；
- 当前线索：纸牌局、4 个因瓦锭、1 个耐火砖；
- 当前成本：4 个 fire_clay_ball；
- 当前效果：高炉砖 recipe + IE blast_furnace multiblock formation。

#### 最初想法从哪里来

1. 玩家得到耐火砖并记录它在普通炉火中的表现；
2. 玩家需要处理因瓦或更高温材料，但现有炉具能力不足；
3. 铁匠/冶炼工居民提交高温冶炼需求；
4. 有冶炼背景的难民可以直接给“怎样垒简易高炉”的 Construction，或给一份缺少高炉砖配方的残页。

证据板把“高温需求”“耐火砖”“火黏土”连起来，生成 Idea：

> 用耐火材料围出更集中的炉膛，也许能建立一座简易高炉。

#### 研究玩法

1. 绘图台先形成一张材料试验建议；
2. 玩家在实验台旁放置加热位和样品架，放入耐火砖样品、4 份火黏土和燃料；
3. 玩家短时值守，或让一名冶炼工居民工作一个班次；
4. 加热 Protocol 记录真实供热是否持续、样品是否放对、耗材是否完整；内部规格和结构推导交给居民计算；
5. 结果卡写“这组材料可用于高温炉体”或“供热中断，暂时无法判断”；
6. 玩家接受结果后一次取得高炉砖 Design 与简易高炉 Construction。

首版并不声称 FrostedHeart 已有通用材料耐久模拟。这是一个作者定义的工程材料试验，使用真实样品、热源、时间和耗材；若以后实现材料退化接口，再由同一 Protocol provider 读取更丰富的世界事实。

#### 直接结果

- Design：高炉砖精确 recipe ID；
- Construction：immersiveengineering:multiblocks/blast_furnace 的 formation；
- 不新增“操作高炉”的限制，因为当前旧研究也没有 use/operation effect。

如果难民只给 Construction，玩家会知道怎样成型，但仍可能缺少高炉砖 Design。这种缺口来自知识来源确实残缺，不是系统强制把一项完整研究拆碎。

### C. Design：进阶真菌养殖

#### 当前基础

- 旧 ID：incubator；
- 当前线索：纸牌局、生石灰；
- 当前投入：一份蘑菇和一份 straw briquette；
- 当前效果：培养箱 recipe + 一份棕色蘑菇菌床奖励；
- 当前机器：IncubatorTileEntity 和 IncubateRecipe 已存在。

#### 最初想法从哪里来

1. 玩家多次携带或更换菌床，记录“能养殖，但占空间且补热麻烦”；
2. 得到生石灰后，记录它能让培养过程脱离随身菌床，成为固定设备的燃料；
3. 城镇食物岗位提交真菌供应不稳定的报告；
4. 有真菌培养经验的难民可以直接给培养箱 Design，或给“生石灰补热”的实验建议。

证据板把“菌床”“固定空间”“生石灰”连起来，生成 Idea：

> 把菌床、养料和补热固定在一个设备中，可能更适合持续培养。

#### 研究玩法

1. 玩家准备两份同类菌床记录：一份继续随身培养，一份放进实验台的固定培养位；
2. 固定培养位加入蘑菇、草料、水和生石灰，复用培养箱现有的输入、催化剂、水与燃料语义；
3. 实验空间检查样品位、供水、生石灰燃料和记录设施，不把环境温度伪装成当前培养箱已经使用的变量；
4. 玩家可以亲自完成短试验；研究所路线消耗一名实验员一个或两个班次、纸、水和生石灰；
5. CalculationProfile 把随身占用、培养时间、燃料/水消耗和有效产出整理成“适合固定设备 / 仍不稳定 / 样本中断”；
6. 玩家选择“画成培养箱设计”后得到配方。

首版 Protocol 可以复用 IncubateRecipe 的输入输出以及 IncubatorTileEntity 的生石灰、水、效率和时间语义。当前培养箱并不读取世界温度，MushroomBed 的随身培养则依赖玩家身体加热条件；两条路径必须如实分别采样，不能在菜单里伪造一套不存在的温区或成功率。实验结果是工程设计依据，不额外发布一条虚构真菌自然规律 Finding。

#### 直接结果

- Design：培养箱的精确 recipe ID；
- 普通奖励：沿用一份棕色蘑菇菌床；
- 不创建 Procedure，因为当前培养箱使用并未被旧 EffectUse 限制。

### D. Prototype：能量塔效率提升 1 → 受控进气原型

#### 当前基础

- 旧 ID：generator_efficiency_1；
- 当前线索：level 1 纸牌局、mechanical_bellows、铸铁或钢锅炉；
- 当前投入：2 个 cast_iron_burner、4 个 encased_fan、8 个铸铁板；
- 当前效果：team-wide generator_effi +10%；
- 当前文字已经明确描述“加装新的燃烧室和鼓风模块”。

这是一项最适合从全队数值改成实体 Prototype 的现有研究。

#### 最初想法从哪里来

1. 玩家在运行中的 T1 上记录“塔持续工作，但燃料消耗很快”；
2. 物流居民提交燃料库存下降报告；
3. 塔操作员提交装料与有效运行时间报告；
4. 玩家接触机械风箱、鼓风机或锅炉后，把它们与燃烧问题钉在一起；
5. 有锅炉/轮机背景的难民可以直接提出“稳定进气”Idea，甚至带来一件旧 Prototype。

证据板生成：

> 更稳定的进气和新的燃烧室，也许能让每份燃料维持更久。

#### 研究玩法

1. 讨论可提出“其实是断供”“超载工况不同”“进气不足”三类人话解释；
2. 计算任务把现有塔报告整理成基线，玩家不计算 process tick；
3. 玩家把现有旧研究的真实材料放入实验台待测部件路线；
4. 工程实验需要工作空间、动力、风扇/燃烧室装置和一名实验员，完成后产出一件有 serial 的受控进气 Prototype；
5. 玩家用通用研究升级工具打开 T1 的统一升级 GUI，把该原型安装到当前 active T1；
6. 塔正常运行时记录燃料装入和有效运行时间；断供或停机显示“暂时无法判断”，不清空已有记录；
7. 玩家可以保留安装、拆下返工或带着现场报告重新进入证据板。

Prototype 的 +0.1 是作者定义的工程效果，不是假装由玩家从数学中发现的自然常数。实验和试用验证的是材料齐备、装置可工作、实际运行没有被断供污染，以及效果是否在真实 host 上生效。

#### 直接结果

- Prototype：frostedheart:t1_controlled_draft_prototype；
- 安装后 InstalledContributionSnapshot 为该 T1 提供 generator_fuel_duration +0.1；
- 不创建 Design，不开放量产配方，不引入 DesignStandard；
- 以后如果确实需要可重复制造，再新增一个普通 Design 研究，直接解锁标准组件配方即可。

当前 GeneratorData 是每 team 一个 active tower，且直接读取 team-wide variant。MVP 明确只支持当前 active T1，先给它增加 incarnation、升级存储和 host-aware modifier resolver；不假装首版已经支持多塔。

### E. 复合边界案例：能量核心再利用

#### 当前基础

- 旧 ID：generator_T1；
- 当前线索：纸牌局、generator_brick、generator_amplifier_r1；
- 当前投入：4 个铜锭；
- 当前效果：generator_core_t1 recipe、energy_core 普通物品奖励、T1 multiblock formation。

#### 最初想法从哪里来

1. 飞船任务或残骸档案给出“能量核心已经失去原供能，但主体可能完整”的记录；
2. 玩家拿到或观察 generator_brick 与 amplifier；
3. 工程居民对核心做检查后提出替代供能 Idea；
4. 有飞船设备经验的难民可以直接给部分或全部知识。

#### 最小研究玩法

1. 证据板连接“旧核心”“替代供能”“周围供热用途”；
2. 居民或机械计算器完成一次接线/材料核对；
3. 实验台使用 component 槽做低功率检查，玩家放入铜材、砖样和放大器，周围必须有动力与记录装置；
4. 玩家审阅“核心可被当前材料重新接入 / 输入不稳定 / 部件不匹配”；
5. 接受后一次取得配方和成型方法。

为了让器物来源真正成立，内容重制时优先让飞船任务交付一件可检查的受损核心或稳定器物记录，而不是继续只在研究完成后凭空领取；但这属于该任务内容迁移，不是新成果类型。

#### 直接结果

- Design：generator_core_t1 的精确 recipe ID；
- Construction：frostedheart:multiblocks/generator 的 formation；
- 普通奖励：energy_core；
- energy_core 本身不叫 Prototype，因为它没有 serial、兼容升级位和安装后局部贡献。

这个案例证明：Design 和 Construction 不需要再被拆成规格、制造法、施工法和应用规则。一项完整研究可以直接同时给出两个权限。

## 通用研究升级 GUI

Prototype 不要求修改每台机器原 GUI。

### 玩家交互

1. 玩家对兼容设备使用 frostedresearch:research_upgrade_tool；
2. 服务端把普通 BlockEntity 或 IE 多方块从属块解析到稳定 host；
3. 打开统一 ResearchUpgradeMenu；
4. 菜单显示该 host 的研究升级位、当前安装 Prototype 和兼容背包物品；
5. 玩家确认安装或拆除；
6. 真实物品保存在 host 的升级存储中，team registry 只维护 serial 到 placement 的索引。

普通设备可以实现 ResearchUpgradeableDevice；IE/第三方设备通过 ResearchUpgradeHostAdapter 接入。host 身份至少包含 host type、主节点 GlobalPos 和 incarnation UUID，不能只用坐标。

MVP 只需要两个通用物品：

| 物品 | 用途 |
|---|---|
| research_upgrade_tool | 打开兼容 host 的统一 GUI |
| upgrade_prototype | 保存 profile、serial、team、材料和视觉信息的动态原型 |

Prototype 外观使用同一基础材质、材料 tint 和白名单 overlay/profile，不为每个设计注册一个新 Item。

安装、拆除和效果查询都在服务端完成。客户端只提交菜单动作、背包槽和 expected revision，不提交 ItemStack、效果值或 host 事实。

## 数据包作者契约

### 普通作者只需要一份 topic

首版推荐路径：

~~~text
data/<namespace>/frostedresearch/topics/<path>.json
data/<namespace>/frostedresearch/prototypes/<path>.json
~~~

大多数研究只写一份 topic。只有 Prototype 的 host、外观和安装贡献需要复用时，才拆出 prototype profile。

topic 负责：

- 哪些世界记录、物品、居民报告或知识包能产生最初 Idea；
- 证据板能揭示哪些 IdeaCandidate；
- 可选择哪些讨论、计算、现场调查或实验 Protocol；
- 哪个注册 resolver 判断结果可领取；
- 直接给哪些 Finding、Design、Construction、Procedure 或 Prototype；
- 仍需保留哪些普通物品奖励；
- 与哪个 raw legacy ID 对应。

运行时不会把 topic 当成百分比进度条。它只是把同一项内容所需的入口、方法和结果放在一起。

### 最小 schema

以下是结构示意，不是已经安装的数据文件：

~~~json
{
  "format": 3,
  "legacy": {
    "id": "geology_understanding",
    "mode": "coexist"
  },
  "presentation": {
    "icon": "frostedheart:copper_pro_pick"
  },
  "idea_sources": {
    "copper_site": {
      "type": "frostedheart:block_and_neighbor_observation",
      "subject": "#frostedheart:ores/copper"
    },
    "miner_report": {
      "type": "frostedheart:town_work_report",
      "report": "frostedheart:mine_ore_with_surrounding_rock"
    },
    "prospector_refugee": {
      "type": "frostedheart:person_knowledge",
      "knowledge": "frostedheart:prospecting_experience"
    }
  },
  "inspiration": {
    "type": "frostedresearch:compare_sites",
    "cards": [
      "copper_site",
      "control_site"
    ],
    "reveals": "frostedheart:rock_and_ore_signs"
  },
  "protocols": [
    "frostedheart:two_site_prospecting",
    "frostedheart:compare_prospecting_records"
  ],
  "resolution": {
    "type": "frostedresearch:reviewed_field_comparison",
    "minimum_sites": 2
  },
  "results": [
    {
      "type": "finding",
      "id": "frostedheart:prospecting_signs_indicate_nearby_ore",
      "views": [
        "frostedheart:geology_archive",
        "frostedheart:prospecting_report_detail"
      ]
    },
    {
      "type": "design",
      "id": "frostedheart:copper_prospecting_pick",
      "recipes": [
        "twr:research/copper_pro_pick"
      ]
    }
  ]
}
~~~

twr:research/copper_pro_pick 是迁移时应建立的稳定配方 ID 示例；当前 KubeJS 配方必须先获得明确 ID，不能把示例当作已存在事实。

### 另外三种结果写法

简易高炉：

~~~json
{
  "results": [
    {
      "type": "design",
      "id": "frostedheart:blast_brick",
      "recipes": [
        "twr:research/blastbrick"
      ]
    },
    {
      "type": "construction",
      "id": "frostedheart:form_simple_blast_furnace",
      "multiblocks": [
        "immersiveengineering:multiblocks/blast_furnace"
      ]
    }
  ]
}
~~~

进阶真菌养殖：

~~~json
{
  "results": [
    {
      "type": "design",
      "id": "frostedheart:incubator",
      "recipes": [
        "twr:research/incubator"
      ]
    }
  ],
  "rewards": [
    {
      "item": "frostedheart:straw_briquette_brown_mushroom",
      "count": 1
    }
  ]
}
~~~

能量塔效率提升：

~~~json
{
  "results": [
    {
      "type": "prototype",
      "profile": "frostedheart:t1_controlled_draft"
    }
  ]
}
~~~

### Prototype profile

~~~json
{
  "format": 1,
  "host": "frostedheart:generator_t1",
  "socket": "frostedheart:air_control",
  "bom": [
    {
      "item": "steampowered:cast_iron_burner",
      "count": 2
    },
    {
      "item": "create:encased_fan",
      "count": 4
    },
    {
      "tag": "forge:plates/cast_iron",
      "count": 8
    }
  ],
  "visual": {
    "profile": "frostedheart:ducted_fan",
    "material": "frostedheart:cast_iron"
  },
  "installed_contributions": [
    {
      "type": "frostedheart:generator_fuel_duration",
      "add": 0.1,
      "exclusive_family": "frostedheart:t1_efficiency_stage_1"
    }
  ]
}
~~~

数值由注册的 UpgradeEffectHandler 验证单位和 host；普通作者不能写 generator_effi 之类旧 CompoundTag 键。

### 三层作者职责

| 角色 | 负责什么 |
|---|---|
| 普通内容作者 | 组合已注册 idea source、Protocol、resolver、五种 result、普通奖励、材料和文本 |
| 资源/叙事作者 | 翻译、证据牌面、难民背景、遗迹文本、报告文本、Prototype overlay |
| Java/集成作者 | 实现世界 observation provider、CalculationProfile、ExperimentProfile、apparatus adapter、Finding view、升级 host/effect handler |

KubeJS 只生成同一 JSON 或在数据生成阶段注册同结构候选；它不能直接给团队添加 Finding、权限或数值。

### 校验与 reload

catalog reload 一次性校验：

- result ID 与引用是否存在；
- Design 是否只引用明确 recipe ID；
- Construction 的 multiblock 是否存在且有服务端拦截点；
- Procedure 的 usable block 是否存在且有服务端拦截点；
- Finding 的 view handler 是否存在；
- Prototype 的 host、socket、BOM、effect handler、metric 和单位是否存在；
- experiment 的槽位数是否超过固定上限；
- legacy raw ID 是否明确；
- 翻译和图标是否缺失。

候选有错误时保留上一份可运行 catalog，并一次显示全部诊断。reload 不修改已生成 Prototype 的 serial、材料或贡献；实体原型引用它制造时冻结的 profile revision。

schema 禁止 progress、points、team/player UUID、任意 Java/NBT path、任意公式脚本、直接 variant key、客户端翻译作为逻辑条件，以及按输出物模糊展开新配方。

## 后端工程架构

### 总图

~~~mermaid
flowchart TB
    S["世界 / 难民 / 居民 / 任务 / 器物"] --> O["KnowledgeSource 与 Observation adapters"]
    O --> G["TeamKnowledgeData：记录、想法、工作制品、四类团队成果 ID"]
    G --> N["NeedCompiler + ActionResolver"]
    N --> W["研究工作页"]
    W --> L["讨论 / 计算 / 实验 Protocol"]
    L --> G
    G --> K["KnowledgeProjection"]
    G --> T["TechnologyAccessProjection"]
    P["Prototype Item + host storage"] --> I["InstalledContributionSnapshot"]
    T --> C["配方 / JEI / 多方块 / 方块使用"]
    K --> U["档案 / HUD / 居民报告 / 对话"]
    I --> H["具体设备运行模型"]
~~~

### 核心数据

~~~text
TeamKnowledgeData
  observations
  ideas
  workOrders
  workArtifacts
  acquiredFindingIds
  acquiredDesignIds
  acquiredConstructionIds
  acquiredProcedureIds
  prototypePlacementIndex

Prototype 物理事实
  inventory ItemStack 或 host upgrade storage
  profileRevision
  serial
  ownerTeam
  placement
~~~

Prototype 不以 acquiredPrototypeIds 作为全队知识保存。实体丢失、拆除或安装都以真实 ItemStack / host 存储为准，team index 只用于定位和对账。

实验高频 trace 独立分段存储；TeamKnowledgeData 只保存 run 元数据和 segment refs。实验台保存 inventory、active run ID 和局部 checkpoint，不保存第二份完整 Run。

### 服务边界

所有写操作统一经过 TeamResearchService：

~~~text
ArchiveObservation
AcceptKnowledgeOffer
PinEvidence
FinishInspirationSession
RecordIdea
CommissionDiscussion
CommissionCalculation
PrepareExperiment
StartExperiment
SealExperiment
AttachWorkArtifact
AcceptFinding
AcceptDesign
AcceptConstruction
AcceptProcedure
FabricatePrototype
InstallPrototype
RemovePrototype
~~~

客户端只发送意图、稳定 ID、team epoch 和 expected revision。服务端重新检查玩家、team、居民、实验台、物品、空间、host 和当前世界状态；客户端不能提交“实验成功”“解锁这个配方”或“原型有 +0.1”。

TeamResearchManager 保留为兼容 facade，唯一命令处理器名称使用 TeamResearchService。

### 端口

~~~text
KnowledgeSourceProvider
ObservationProvider
ResearchProtocol
DiscussionProfile
CalculationProfile
ExperimentProfile
ResearchApparatusAdapter
FindingViewHandler
ResearchUpgradeHostAdapter
UpgradeEffectHandler
TechnologyAccessResolver
~~~

不实现 AssetCapabilityCompiler、ApplicationRule、ApplicationProfile、RuntimeApplicationSnapshot 或 AppliedProcedureBinding。

### 同步和性能

- team 切换时用共同 TeamContextEpoch 清空并安装 town/research/knowledge 快照；
- catalog revision 先于 team 状态；
- 客户端常驻同步研究摘要、成果 ID 和三类 projection 摘要，不同步无界 trace；
- 只有 active experiment 高频采样，普通机器和城镇只提交稀疏或聚合记录；
- 行动卡在知识变化时重算，可执行性在人员、材料或设施变化时局部刷新；
- Prototype 安装/拆除只失效目标 host 的 InstalledContributionSnapshot。

## 与当前源码和内容的边界

1. DrawingDeskTileEntity 当前把 ResearchGame 完成提交为 MinigameClue。V2 保留棋盘，新增 finishInspirationSession 产出 IdeaCandidate；旧 research 继续走原 clue executor。
2. MechCalcTileEntity 当前生产 points。新路径只执行具体 CalculationWorkOrder；Create 缺席时居民路径仍可用。
3. ITownBuilding.CODEC 当前使用 `buildByNameWithLegacyInt()`：新存档按名称分派，新类型可插入任意位置；MVP 新增 researchInstitute 时只需验证名称 round-trip 与旧整数解码回归，不再维持虚假的新增顺序约束。
4. BuildingBlockScanner / ConfinedSpaceScanner 可作为封闭实验空间基础，但必须有 bounded scan、明确 interior anchor 和缓存；露天调查使用独立 field protocol。
5. Resident 当前没有私人知识字段；使用独立 PersonKnowledgeOverlay，并处理难民招募换 UUID。
6. ProspectorPick / GeologistsHammer 已有真实附近矿物扫描；岩石研究复用抽出的 OreProspectingModel，不虚构地层性质。
7. IncubatorTileEntity / IncubateRecipe 已存在；真菌试验尽量复用其输入输出语义和温度接口。
8. GeneratorData 当前每 team 只有一个 active T1，并直接读 team variant；Prototype 切片必须先增加 incarnation、host storage 和 host-aware resolver。
9. 当前 81 个旧 research 定义中有 128 个 recipe effect、27 个 multiblock、4 个 use 和 6 个 stats。V2 不在首版一次重制全部。
10. 旧 EffectCrafting 大多按输出物在 reload 时展开配方；新 Design 必须切换到稳定 recipe ID。

## 旧研究兼容

未重制的旧研究继续使用旧 Research / Clue / Effect executor。兼容映射只回答既有权限，不伪造新的知识成果：

| 旧数据 | 新查询中的来源 | 不自动生成 |
|---|---|---|
| EffectCrafting | legacy recipe entitlement，与 Design recipe IDs 合并 | Design、Finding |
| EffectBuilding | legacy multiblock entitlement，与 Construction 合并 | Construction 资产、Finding |
| EffectUse | legacy usable-block entitlement，与 Procedure 合并 | Procedure 资产、完整机器操作知识 |
| EffectStats | LegacyTeamModifier，旧消费者迁移前继续兼容 | Prototype |
| EffectItem / experience / command | 旧 executor 按 effectData 幂等发放 | Prototype 或知识成果 |
| 直接 research-ID gate | 原 adapter 或显式 legacy entitlement | Finding |

规则：

1. 旧 effectData 仍是是否已经 grant 和物品奖励幂等的权威，不能仅凭 finished 重发；
2. 旧 variants、level、clueData、insight 和 visitedArea 原样保留；
3. legacy 权限可以进入 TechnologyAccessProjection，但不能满足新研究 resolver；
4. 旧 generator_effi 继续作为 legacy team modifier；人工重制后的受控进气 Prototype 使用同一 exclusive family 去重，不能叠成 +0.2；
5. 新旧来源同时存在时，UI 显示两条来源，失去一条不会关闭另一条；
6. 不自动把完成的旧 geology_understanding 伪造成新 Finding。若内容作者决定给旧玩家承认该知识，必须写显式 legacy migration，并标记来源；
7. 新 topic 可以 coexist 或 supersede 旧入口，但 supersede 不删除旧存档历史和已经领取的奖励。

五项首批内容的迁移目标：

| 旧研究 | V2 明确结果 |
|---|---|
| geology_understanding | Finding + 铜探矿镐 Design |
| blast_furnace | 高炉砖 Design + 高炉成型 Construction |
| incubator | 培养箱 Design + 原普通物品奖励 |
| generator_T1 | 核心 Design + T1 成型 Construction + 原普通物品奖励 |
| generator_efficiency_1 | 新档走实体 Prototype；旧档保留并去重 legacy +0.1 |

## 实施顺序

### Phase 0：纸面验证五项研究

- 为五项研究各画一张从 Idea 来源到结果卡的可点击流程；
- 验证五个结果词是否无需解释即可理解；
- 为每项列出现有 world fact、需要新增的 observation/provider 和不允许声称的机制；
- 冻结实验台准备屏的信息结构，不冻结复杂 schema。

验收：玩家能够复述每项研究为什么开始、下一步做什么、最终得到什么。

### Phase 1：五种结果和三类直接查询（已完成基础层）

- [x] 在独立 `frostedresearch:knowledge` TeamKnowledgeData 中加入 Finding、Design、Construction、Procedure 的 acquired ID；
- [x] 实现五种 result codec、最小 topic/profile datapack loader 和独立普通 rewards 定义；
- [x] 实现不可变单调 revision catalog、候选聚合诊断与 last-known-good reload；
- [x] 实现 KnowledgeProjection、TechnologyAccessProjection、managed universe、来源 trace 与最小四个查询；
- [x] Design 直接驱动 recipe ID；Construction 直接驱动 multiblock；Procedure 直接驱动 usable block；
- [x] 合并 EffectCrafting / EffectBuilding / EffectUse legacy entitlement，并从已完成且 effect 已 grant 的权威状态重建来源；
- [x] 实现全量 knowledge snapshot、登录/换队/grant/reload 同步以及 JEI 刷新；
- [x] 实现 `upgrade_prototype` 实体壳、冻结 profile revision/serial/owner team，以及管理员 grant/revoke/info 命令和可选在线玩家团队目标。

验收：用开发命令授予五种结果时，Finding 只改变信息，Design 只开放精确配方，Construction 只开放精确成型，Procedure 只开放精确方块使用，Prototype 每次制造一件新 serial 的实体；revoke 可移除四种团队成果及 orphan 历史，info 可检查定义与团队状态，命令可显式选择在线玩家所属团队。Phase 1 未新增正式 topic，具体内容纵切从 Phase 2 开始。

### Phase 2：证据板与岩石 Finding

- 改造 DrawingDesk 的小游戏产物为 IdeaCandidate；
- 实现记录收件箱、钉位、语义去重和最多三张行动卡；
- 从现有探矿工具抽 OreProspectingModel；
- 实现两地点记录、手工/居民计算和岩石 Finding view；
- 给铜探矿镐 KubeJS 配方建立稳定 ID。

验收：玩家可以从世界调查或难民知识进入；Finding 的信息反馈与铜探矿镐 Design 是两个看得懂的并列结果。

### Phase 3：最小研究所、实验台、高炉与真菌

- 增加 researchInstitute 的最小 staffing、讨论/计算/实验队列和一人一日一单；
- 实现 experiment_table、固定槽位、封闭空间检查和基础加热/记录 apparatus；
- 实现高炉材料试验和真菌培养试验；
- 打通 Design 配方与 Construction formation 的真实执行端；
- 保留 Incubator 普通物品奖励。

验收：两项研究都消耗实际样品、设施、居民或玩家时间；高炉一次给两个结果，培养箱一次给配方，玩家不填写公式。

### Phase 4：能量核心复合研究

- 把任务/飞船档案接成稳定 KnowledgeOffer；
- 实现核心低功率台架和居民材料/接线计算；
- 给 generator_core_t1 配方建立稳定 ID；
- 一次授予 Design + Construction + 普通 energy_core 奖励；
- 不把普通核心标成 Prototype。

验收：该研究完整保留当前配方、成型和物品回报，同时不引入标准化层。

### Phase 5：实体 Prototype 与塔效率

- 实现 research_upgrade_tool、统一菜单、动态 prototype item 和 T1 host adapter；
- 给当前 active T1 增加 incarnation、升级存储和必要燃料运行摘要；
- 实现受控进气 Prototype 的实验制造、安装、拆除和局部 +0.1；
- 把 generator consumer 切到 host-aware resolver，并与旧 variant 原子去重；
- 不修改 Generator 原 GUI，不开放量产配方。

验收：同一 team 只有安装着该 serial 的 active T1 得到效果；拆下后效果消失；旧存档不会得到双份加成。

### Phase 6：内容作者工具、迁移和文档

- 完成 topic/prototype schema、datapack reload、validate 和 dump template；
- 迁移 JEI、实际 recipe 执行、IE formation 与 RightClickBlock 到统一查询；
- 再选择下一批旧 research 做人工重制，不自动猜 Finding；
- 更新 docs/research、相关 town/climate 文档和 diary。

## 验收标准

### 玩家体验

- 玩家能从亲历、难民、居民、遗迹或任务开始研究；
- 证据板帮助产生 Idea，但直接知识不被迫再玩一次小游戏；
- 工作页最多显示三个下一步；
- 讨论、计算和实验都占用真实资源和时间；
- 实验需要真实空间、装置和样品，但玩家不填写数学表；
- Finding 有明确的信息反馈；
- Design 立即开放明确配方；
- Construction 立即开放明确成型；
- Procedure 立即开放明确方块使用；
- Prototype 是实体，只有安装后影响目标设备；
- 简易高炉和能量核心再利用可以一次给多个结果。

### 内容创作

- 普通作者用一份 topic JSON 能定义 Idea 来源、Protocol、resolver 和 results；
- 普通作者不需要理解能力清单、应用规则或标准化状态机；
- 新 Design 只引用稳定 recipe ID；
- 新 Construction 只引用实际存在且已接服务端检查的 multiblock；
- 新 Procedure 只引用实际存在且已接服务端检查的 usable block；
- Prototype profile 明确 BOM、host、socket、外观和类型化安装效果；
- 所有错误在 reload 时一次给出，上一份 catalog 继续运行。

### 工程

- 世界记录、团队成果、实验 trace 和 Prototype 物理事实各有唯一权威；
- projection 可以从结果 ID、legacy 权限和 host 实体重建；
- 旧 effectData 不重复发奖；
- JEI 与真实执行使用同一 recipe query；
- multiblock formation 与 block use 分别使用 Construction / Procedure query，但共享同一 TechnologyAccessResolver；
- 新设备数值不写 team-wide variant；
- catalog reload 不改变已经存在的 Prototype serial 和效果 revision。

## 明确不做

- 不在 MVP 中实现 DesignStandard；
- 不实现 CapabilityManifest、ApplicationRule 或多层资格编译；
- 不把制造配方拆成 Design + 制造 Procedure；
- 不把 Construction 再拆成结构规格 + 施工 Procedure；
- 不让 Procedure 提供运行数值；
- 不把普通 item reward 叫 Prototype；
- 不要求每项研究都有讨论、计算和实验三步；
- 不要求玩家填写公式、变量、误差或实验计划；
- 不为每台设备修改原 GUI；
- 不在首发继续扩写白幕、水相变、植物温区或未实现的世界观机制；
- 不一次重制全部 81 项旧研究。

## Outcome

Status: in-progress。

Phase 1 已完成五类成果基础层：Finding、Design、Construction、Procedure、Prototype；其中 Construction 独占多方块成型权，Procedure 独占方块右键使用权。已实现结果/目录、团队权威、物理 Prototype 壳、统一投影与 legacy 来源、执行端/JEI 适配和全量同步，但没有新增正式 topic、改动 companion 配方或迁移五项旧研究。下一步从 Phase 0 流程稿与 Phase 2 岩石 Finding 纵切继续；任何新增后台类型都必须先证明它能让这五项研究更清楚或更可玩，否则不进入 MVP。
