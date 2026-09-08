# 知识定义示例数据包

这是用于体验和编写知识定义的 Minecraft 1.20.1 数据包，未随模组资源自动加载。其
`data/knowledge_example/frostedresearch/knowledge` JSON
是本示例的有效内容；系统格式和行为以[知识定义文档](../definitions.md)和 Java 源码为准。

将本目录复制到测试世界的 `datapacks/knowledge-example` 后执行 `/reload`。从纸和木棍的物品观察，或橡木原木与纸的观察开始。完整包包含
9 个想法、9 个项目、17 项成果和 12 条联络规则，可展示分支、汇合与回环。

体验步骤与临时完成指令见 [游戏内检查路线](../playthrough.md)。`/knowledge research complete <项目ID> [想法ID]`
供开发者跳过尚未实现的计算、证据和实验任务，保留真实研究来源与成果学习流程。加载本示例会管理其中声明的配方、拉杆/熔炉操作和高炉成型；它用于测试世界，不是正式整合包进度。

继续编写时从[各定义与字段](../definitions.md)开始；复制并修改示例标识，不直接把示例命名空间当作正式整合包内容。
