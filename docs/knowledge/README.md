# 知识系统

这里说明观察、想法、成果组成的队伍知识系统，以及绘图台中的记录、学习、联络和传播。

当前行为以 [`knowledge/` 源码](../../src/main/java/com/teammoeg/frostedresearch/knowledge/)
和生效的数据包为准。人类设计意图见只读的 [`design/knowledge_research.md`](../../design/knowledge_research.md)
；旧研究项目与小游戏仍由 [`docs/research/`](../research/README.md) 说明。

阅读顺序：

1. [状态、API 与生命周期](state-and-api.md)：档案、来件、事件、休眠、历史、权限与系统边界。
2. [定义与联络规则](definitions.md)：数据包格式、有类型的条件、匹配和观察用途扩展。
3. [观察与研究笔记](observations-and-notes.md)：主动采样、实物检索、记录身份和笔记传播。
4. [绘图台操作](workbench.md)：界面入口、筛选、联络、图谱和录入录出。
5. [游戏内检查路线](playthrough.md)：示例包的研究闭环、分支/回环和成果效果。

使用时在绘图台点击「知识」，在世界中按已配置的观察键（默认 N）观察。数据包作者可从 [示例数据包](example-datapack/)
开始；该示例不随模组自动加载。修改实现时运行本轮知识测试，更新对应文档并追加 [`diary/`](../../diary/README.md) 记录。
