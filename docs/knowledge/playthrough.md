# 示例包的游戏内检查路线

- Status: `Current`
- Last verified: `2026-09-08`
- Scope: 开发测试用知识联络、项目完成、图谱分支/回环与成果效果
- Code anchors: [`example-datapack`](example-datapack/README.md)、[
  `KnowledgeCommands`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeCommands.java)、[
  `KnowledgeService`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java)、[
  `KnowledgeGraphModel`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeGraphModel.java)

本轮已同步更新 `run/saves/knowledge-test/datapacks/example-datapack`。重启更新后的开发客户端进入该世界；其他世界需要自行安装此示例包。它不是正式进度内容。

## 起点

在抄录台检视纸和木棍，把两份观察记入档案，然后联络，得到「纸页与木棍」想法。也可以长按观察键记录放置的橡木原木，再与纸的物品观察联络。

「脉络」现在会显示已经公开的「材料比较」项目；未开始时与想法之间是虚线。管理员执行：

```text
/knowledge research complete knowledge_example:material_record
```

得到 `material_finding`（材料札记）与 `oak_button_design`（木制按钮）。项目与实际成果连接进入图谱，项目标为完成。

## 分支与汇合

下表省略 `knowledge_example:` 前缀。每次先在档案中选择输入进行联络，得到想法，再使用对应完整项目 ID 执行完成指令。

| 联络输入                                     | 想法                  | 完成项目               | 获得成果                                             |
|------------------------------------------|---------------------|--------------------|--------------------------------------------------|
| `material_finding` + `oak_button_design` | `mechanisms` 停留的信号  | `basic_switch`     | `lever_design`、`lever_procedure`                 |
| 同一组合，选择另一个候选                             | `pressure` 脚下的回应    | `pressure_control` | `pressure_plate_design`、`pressure_finding`       |
| `material_finding` + 煤的物品观察              | `ignition` 留住火种     | `fire_control`     | `campfire_design`、`heat_finding`                 |
| `pressure_finding` + `material_finding`  | `shelter` 交错的支撑     | `lattice`          | `ladder_design`、`climbing_finding`               |
| `lever_procedure` + `pressure_finding`   | `signals` 把回应传远     | `signal_paths`     | `repeater_design`、`signal_finding`               |
| `heat_finding` + `material_finding`      | `heat_work` 火中的变化   | `kiln`             | `charcoal_design`、`metallurgy_finding`           |
| `signal_finding` + `metallurgy_finding`  | `automation` 会回应的炉火 | `furnace_control`  | `furnace_procedure`、`blast_furnace_construction` |
| `signal_finding` + `lever_design`        | `feedback` 回应的回声    | `feedback_study`   | `feedback_finding`                               |

例如，掌握「停留的信号」后：

```text
/knowledge research complete knowledge_example:basic_switch
```

指令需要权限等级 2，且队伍已经掌握可以启动该项目的想法。省略想法参数时，按 ID
顺序选择一个当前有效、声明该项目的想法；可用第二个参数显式指定来源。它不授予缺失的想法。成果仍检查正常学习条件，不能学习时保留在来件。

## 回环

继续联络以下组合：

- `metallurgy_finding` + `climbing_finding` → 已有的 `materials`。
- `feedback_finding` + `lever_procedure` → 已有的 `mechanisms`。

知识身份不会重复增加，但会记录新的实际推导路径，形成回指初始想法的环。图中使用回指连线表达这些路径，不执行力导向迭代。

滚轮以鼠标所在位置缩放；拖动画布平移；「全图」重新适配。点击知识、项目或联络连接点，打开第二张浮纸；关闭后保留相机与当前页面。

## 成果效果

| 成果                           | 可检查的实际目标                                               |
|------------------------------|--------------------------------------------------------|
| `oak_button_design`          | 配方 `minecraft:oak_button`                              |
| `lever_design`               | 配方 `minecraft:lever`                                   |
| `pressure_plate_design`      | 配方 `minecraft:oak_pressure_plate`                      |
| `campfire_design`            | 配方 `minecraft:campfire`                                |
| `ladder_design`              | 配方 `minecraft:ladder`                                  |
| `repeater_design`            | 配方 `minecraft:repeater`                                |
| `charcoal_design`            | 熔炼配方 `minecraft:charcoal`                              |
| `lever_procedure`            | 方块 `minecraft:lever` 的操作                               |
| `furnace_procedure`          | 方块 `minecraft:furnace` 的操作                             |
| `blast_furnace_construction` | IE `immersiveengineering:multiblocks/blast_furnace` 成型 |

这些目标从示例包加载时就成为受管理目标。比较完成研究前后效果时使用同一队伍。忘记某项成果可通过已有
`/knowledge forget result <成果ID>` 检查其权限撤销；已完成研究历史不会因此消失。
