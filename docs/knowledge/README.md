# 知识系统

这里说明观察、想法、成果组成的队伍知识系统，以及绘图台中的记录、学习、联络、传播和分阶段研究。

当前行为以 [`knowledge/` 源码](../../src/main/java/com/teammoeg/frostedresearch/knowledge/)
和生效的数据包为准。人类设计意图见只读的 [`design/knowledge_research.md`](../../design/knowledge_research.md)
；旧研究项目与小游戏仍由 [`docs/research/`](../research/README.md) 说明。

阅读顺序：

1. [状态、API 与生命周期](state-and-api.md)：档案、来件、事件、休眠、历史、权限与系统边界。
2. [定义与联络规则](definitions.md)：数据包格式、有类型的条件、匹配和观察用途扩展。
3. [观察与研究笔记](observations-and-notes.md)：主动采样、实物检索、记录身份和笔记传播。
4. [绘图台操作](workbench.md)：界面入口、筛选、联络、图谱、录入录出、研究页与实验台。
5. [小炉与实物提交](trial-kilns-and-samples.md)：三配比试烧、当前 run 样品及绘图台消耗提交。
6. [游戏内检查路线](playthrough.md)：示例包的研究闭环、分支/回环和成果效果。
7. [土制冶炼炉正式路线](earthen-smelter.md)：整合包中的发现、材料准备、试烧与正式解锁。
8. [阶段与风道](stages-and-airflow.md)：阶段选择、可选任务、临时配方与鼓风口试验。
9. [冶铁炉正式路线](iron-bloomery.md)：材料观察、风道规划、风箱试吹与最终生产配方。
10. [编写研究内容](authoring-guide.md)：可复制的项目、知识入口、自定义事件及最小验证步骤。
11. [控温实验炉](controlled-furnace.md)：温度、批次、试样与生产权限的当前实现。
12. [普通耐火砖正式路线](refractory-brick.md)：原料、参考知识、试烧与批量生产。

使用时在绘图台点击「知识」，按已配置的观察键（默认 N）检视世界目标、无目标时的手持物品或容器中的悬停物品。放大镜也可长按右键观察，物品检视时间减半；具体规则见[观察与研究笔记](observations-and-notes.md)。数据包作者可从 [示例数据包](example-datapack/)
开始；该示例不随模组自动加载。修改实现时运行本轮知识测试，更新对应文档并追加 [`diary/`](../../diary/README.md) 记录。
