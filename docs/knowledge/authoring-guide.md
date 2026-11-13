# 编写研究内容

- Status: `Verified`：本页核对 M1–M5 已实现格式与服务端测试；各正式项目的客户端及游玩验证范围见其操作文档。
- Last verified: `2026-11-14`
- Scope: 数据包内容作者的入口与可复制范例，不替代完整字段参考。
- Code anchors: [ProjectDefinition](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectDefinition.java)、[StageDefinition](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/StageDefinition.java)、[IdeaAcquisitionService.emit](../../src/main/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionService.java)。

## 从现有项目复制

正式整合包内容放在 TWR 的 `kubejs/data/<namespace>/frostedresearch/knowledge/`；普通数据包使用 `data/<namespace>/frostedresearch/knowledge/`。想法、项目、成果和联络规则分别位于 `ideas/`、`projects/`、`results/`、`links/`。JSON 文件路径决定定义 ID。模组不会自动安装文档目录里的示例包。

| 需求 | 可复制入口 | 需要一起修改 |
|---|---|---|
| 单阶段、提交试样 | [土制冶炼炉](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/earthen_smelter.json) | 对应想法、两成果、材料观察和 sample 的 experiment/item |
| 两阶段、小游戏与原型 | [冶铁炉](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/iron_bloomery.json) | 阶段任务、临时配方、观察任务和最终 Design |
| 多个想法共同开启 | [joint_study](example-datapack/data/example/frostedresearch/knowledge/projects/joint_study.json) | 开始条件、逐项披露组、对应想法定义 |
| 成果承接下一研究 | [followup](example-datapack/data/example/frostedresearch/knowledge/projects/followup.json) | 成果入口及开始条件；不必强加想法 |
| 联络与事件使用不同前置 | [正式联络规则](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/) | inputs 是必要桌面记录；required_knowledge / optional_knowledge 独立配置，详见[定义参考](definitions.md#联络规则) |
| 行为获取想法 | [field_idea](example-datapack/data/example/frostedresearch/knowledge/ideas/field_idea.json) | acquisition.required/optional/triggers；学习条件仍是 understanding |
| 知识影响下一阶段提示 | [耐火砖](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/refractory_brick.json) | 有序 variants、参考知识与默认路径；进入阶段时固定 |
| 可选任务减少后续任务 | [阶段格式](stages-and-airflow.md)、[可执行的阶段测试](../../src/gametest/java/com/teammoeg/frostedresearch/knowledge/StageResearchGameTests.java) | when.tasks_all 与 omit_tasks；不改变最终成果 |

复制时保留结构，换掉命名空间、定义 ID、标题键和引用目标。标题、目标、操作方法与结论分别写清，正文也应避免提前透露尚未披露的要求；中英文内容放在资源包语言文件中。完整字段及可用观察谓词见[定义参考](definitions.md)。

## 三个容易混淆的边界

- **发现和开始分开。** `access.discover_any` 与已学前置想法负责入口，`start_all` 负责开始条件。未知要求仍逐项占位；只有来件不等于掌握。铜/青铜任选可用一个合并物品标签表达，不需要复制两项都必须满足的条件。
- **开始前置和路径参考分开。** 获取想法时的非必要知识通常仍放入项目开始条件；仅用于改变后续路径的参考知识放进 `variants.when`。进入过的路径不会随补知识重选。任务 ID 在整个项目中唯一，修改显示顺序不应更换 ID。
- **任务动作和学知识分开。** `sample` 消耗当前队伍、当前 run 的实物；`observation` 读取新鲜观察，绑定实验时还检查本次试件，既有档案或笔记不能顶替。普通永久知识前置仍按有效档案判定。临时配方写在阶段 `completion_recipes`，最终权限写在成果中。

## 接入一个自定义行为

没有 Java 代码发出事件时，单写事件名称不会自动监听游戏。可以在已确定行为成功的服务端接点调用：

```java
IdeaAcquisitionService.emit(player,
    new ResourceLocation("example", "repaired_machine"), Optional.empty());
```

想法定义的 `acquisition.triggers` 对应填写：

```json
[{"event":"example:repaired_machine"}]
```

若需按物品或方块区分，发送对应 `KnowledgeElement`，并在 trigger 的 `subject` 中填写已有知识条件格式。多个 trigger 是任选入口；必要知识、非必要知识和概率仍由该想法自己的 acquisition 决定。不要把这些条件加到所有外部 `produce` 或 `learn` 授予途径。

## 最小验证步骤

先检查 JSON、定义引用、语言键和实际配方 ID，再在装有对应模组及脚本的环境重载。检查“没有知识 → 发现 → 缺项 → 开始 → 任务 → 完成 → 实际权限”整条流程。Design 写的是配方 ID，不是产物 ID；机器是否调用技术权限接口要单独确认，不能仅凭成果声明认定机器已受管。

阶段与观察测试入口见 [GameTest README](../../src/gametest/README.md)。运行测试时应看到当轮 `GAME TESTS COMPLETE` 和 `All N required tests passed`；游戏启动失败时 Gradle 仍可能退出成功。真实客户端、独立进程重启与生存获取是不同证据，分别记录。
