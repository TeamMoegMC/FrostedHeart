# 小炉试烧与研究实物提交

- Status: `Current`（源码与 Forge GameTest 验证范围见最新开发记录；客户端画面、生存获取和独立进程重启另验）
- Last verified: `2026-11-14`（提交入口扩展为玩家主背包/快捷栏与绘图台物件槽）
- Scope: M3-A 三组小炉、固定燃料试烧、收取/拆除/保存、当前 run 样品绑定与绘图台实物提交；不包含 TWR 正式解锁或旧研究迁移。
- Code anchors: [TrialKilnBlock](../../src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/TrialKilnBlock.java)、[TrialKilnBlockEntity](../../src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/TrialKilnBlockEntity.java)、[ResearchSampleBinding](../../src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/ResearchSampleBinding.java)、[ProjectRequirement](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectRequirement.java)、[KnowledgeService.submitSample](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java)。

## 制作与试烧

三条 FH 原生 shapeless 配方均直接可用，放在随身 2×2 格中即可，不需要正式炉、工具、水桶、M4 临时权限或额外阶段。

| 方块/同名物品 ID（frostedresearch:） | 黏土球:沙 | 固定结果 |
|---|---|---|
| trial_kiln_clay_rich | 3:1 | cracked_trial_kiln：炉壁收缩开裂 |
| trial_kiln_sand_rich | 1:3 | cracked_trial_kiln：炉壁松散崩解 |
| trial_kiln_balanced | 2:2 | fired_trial_kiln：炉壁保持完好 |

输入使用 minecraft:clay_ball 与 minecraft:sand；每配方输出1座未烧小炉。这里是固定玩法配比，不是材料物理模拟。
放置后主手持至少4根 minecraft:stick 右键，服务端一次消耗4根并试烧400个加载中的游戏 tick（20TPS时20秒）。
不足不扣；烧制中右键只报告剩余秒数，不追加燃料。没有离线时间补算，不连接热网、风箱或其他炉体。

世界状态 phase：0未烧、1烧制中、2完好、3损坏。三种块共用一个 BlockEntity 类型；配比由方块 ID 保存，
remaining 保存剩余 tick。原版贴图组成低矮小炉模型，烧制时发光，完成后显示完好/裂纹；右键显示结果原因。

## 关联本次研究

实验标识固定为 frostedresearch:trial_kiln。小炉通过 KnowledgeService.sampleTargets 查询当前队伍的开放 run 中，
尚未完成且 sample.experiment 匹配的任务，不引用任何 the_winter_rescue 项目 ID。

- 只有一个待交目标：投入燃料时自动关联。
- 多个目标：先空手右键循环选择，提示项目及任务标题，再持木棍起烧；未选择时不扣燃料。
- 没有目标：仍可试烧，明确提示未关联；这种结果不能用于之后才开始的研究。

未烧小炉可以预先制作；是否能提交取决于本次起烧时的绑定。燃烧期间目标不会换绑；遗忘想法不清空进行中 run，
但放弃、完成或重新开始后的旧样品不满足新的开放任务。换队后的其他队伍不能提交旧队伍样品。

## 收取与拆除

完成后空手右键收取一件试样，并移除世界小炉。背包装不下的剩余物品掉落到玩家附近；同一 settle 方法先标记结算，
之后移除方块不会再掉第二件。烧完直接拆除也只给一件对应结果。结果物品每堆最多1件。

未烧或烧制中拆除只返还同配比的未烧小炉；木棍不退、剩余进度/选择归零，重放需要重新供燃料。
正常区块卸载/保存保留原方块 ID、phase、remaining 和绑定，恢复后按已加载 tick 继续。没有基于时间戳的追赶。

## 实物任务与提交

ProjectRequirement 增加独立 type=sample，例如：

```json
{
  "id": "submit_intact",
  "type": "sample",
  "title": "提交本次试烧的完好小炉",
  "sample": {
    "experiment": "frostedresearch:trial_kiln",
    "item": "frostedresearch:fired_trial_kiln"
  }
}
```

一次提交固定消耗1件。sample 任务不接受额外 level、count 覆盖、观察槽或实验台时长，不把它当实验台计时/观察知识。
起烧绑定仅含 team、run、requirement、experiment，结果物品以 ResearchSample 标签保存这四项，不复制全量世界/玩家 NBT。
坏样即使带绑定也不符合指定的成功物品 ID；未绑定样、旧 run 样、错误队伍/任务/实验的样品不扣除。

把实际试样留在玩家主背包或快捷栏，打开绘图台研究详情，选中实物任务，点击“提交试样（消耗1件）”即可；也可继续使用绘图台工具页的检视物槽。无需点击检视或记入知识档案。
SUBMIT_SAMPLE仍只发送run与任务ID，服务端先尝试桌上实际物品，再按玩家Inventory.items的顺序查找36格主背包/快捷栏；不遍历装备、副手、鼠标携带物或嵌套容器。桌上无效样不会挡住背包中的合格样。
检查全部通过后，同次操作扣1件并写任务回执；任务已满足或 run 已关闭时不再扣物。之后点击正常完成研究按钮取得项目成果。
仅本次完好样必交，坏炉不强制计数；不要求三组全部试烧。

ResearchHistory.samples 保存任务 ID 到 {experiment,item} 的回执。存储 schema_version=5，v3以及更旧存档缺这个可选映射时
读为空，不改变既有 origins/证据/演算/实验进度；v2单 idea 到 origins 的兼容继续保留。所有 with*/完成复制保留 samples，放弃清空任务进度。
若作者后来改变该任务的实验或物品，旧回执不会自动满足改变后的要求。

## 示例与验收边界

[example:trial_kiln](example-datapack/data/example/frostedresearch/knowledge/projects/trial_kiln.json) 是完整的独立示例：
实际取得黏土球可按 M2 得到想法，黏土球与沙观察入档后开始，起烧均衡小炉，收取成功样留在背包或放检视物槽提交，再正常完成获得 Finding。
示例包不自动加载；也可用开发 grant 验证入口，但不把命令授予当作生存获取证明。正式 Design/Construction、IE 配方/成型权限和旧研究桥接留 M3-B。

TrialKilnGameTests 使用独立批次，验证加载中的400tick、三结果、燃料与拆除、BE NBT重建、一次结算、实际槽位消耗与绑定回归；
真实客户端视觉/键鼠、跨进程重启、正式 TWR 生存材料路线仍需后续验收。

M4将KnowledgeState版本升至5；本页样品绑定与samples回执契约保持原样，schema4记录继续读取。
阶段项目的小炉候选只取当前选定任务；尚未进入的阶段不会提前绑定，离开的阶段不接受旧样品回调。

## M6 多份合并记录

小炉sampleTargets与研究页采用同一创建时间+UUID顺序；原空手cycle操作不变，文案增加可区分的记录序号、阶段和任务。仅一个目标仍保持原自动绑定，多个目标保持既有明确选择要求。已完成项目的备用run不提供新任务目标，历史阶段与已提交样品进度保留；遗忘后恢复。merge不将来源队伍的世界小炉或未提交ResearchSample改绑到目标队伍，旧样品需重新制作/绑定。
