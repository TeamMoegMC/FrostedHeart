# 知识定义示例数据包

这是用于体验和编写知识定义的 Minecraft 1.20.1 数据包，未随模组资源自动加载。其
`data/knowledge_example/frostedresearch/knowledge` JSON
及 `data/example/frostedresearch/knowledge` 是本示例的有效内容；系统格式和行为以[知识定义文档](../definitions.md)和 Java 源码为准。

将本目录复制到测试世界的 `datapacks/knowledge-example` 后执行 `/reload`。从纸和木棍的物品观察，或橡木原木与纸的观察开始。knowledge_example 命名空间包含
9 个想法、9 个项目、17 项成果和 12 条联络规则；另有 example 命名空间的 M1 多来源与纯成果入口，可展示分支、汇合与回环。其中 `material_record`、`basic_switch`、`pressure_control` 与
`lattice` 带有证据、演算和实验需求，可在研究页与实验台完成。

体验步骤与临时完成指令见 [游戏内检查路线](../playthrough.md)。`/knowledge research complete <项目ID> [想法ID]`
供开发者跳过项目需求，保留真实研究来源与成果学习流程。加载本示例会管理其中声明的配方、拉杆/熔炉操作和高炉成型；它用于测试世界，不是正式整合包进度。

继续编写时从[各定义与字段](../definitions.md)开始；复制并修改示例标识，不直接把示例命名空间当作正式整合包内容。

M1 验证：`/knowledge grant idea example:i` 后打开 `example:joint_study`，五项必要条件中 J/C 各一个未知占位；
检视粘土球、沙、木炭并入档，再授予/入档 example:j 可补齐。B 只显示一次。
作者例外可只从 start_all 删除 a，保留组关联作为参考。
`/knowledge grant result example:smelting_finding` 后可发现 `example:followup`；
`/knowledge research complete example:followup` 无需指定想法。上述 grant 是开发验证入口，不代表正式生存获取路线。

M2：授予并入档 example:i 后，实际拾取纸、使用苹果、观察纸或调用 example:field_work 可获得 example:field_idea（示例 chance=1）。
也可在档案零选择匹配 field_zero，选择一份纸匹配 field_one，或用粘土球＋沙匹配 field_legacy，等待完成。
example:smelting_finding 入档后会触发无联络规则的 learned_hint，并使 field_idea 联络减20tick。
behavior_study 承接新想法。示例用于测试世界，开发 grant 不代表正式生存获取路线。
