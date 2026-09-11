# 材料温度闭环调查与现有计划修订

- Time: `2026-09-10 00:53:06 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `材料查询、导热、休眠保存、玩家长波与表面红外的工程计划；未实施生产代码`

## Completed

- 原位扩展[当前方块热模型计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#material-temperature-closure)，明确最新执行入口、旧约束的替代关系与实施状态，保留此前实施记录。
- 将[旧红外计划](../plans/2026-08-29_18-38-09_infrared-mesh-temperature-rendering.md)标为superseded并链接新方案，不再以旧Air-only目标指导后续实现。
- 核实材料温度参与求解器的空气/材料换热与hot mask，但工具、红外和休眠读取只取transport；V0/V0面被跳过，material-only Brick的查询地址无效。补齐相应数据与生命周期方案。

## Decisions

- 沿用稀疏Brick、单worker、arena、指数交换、既有source/辐射和增量协议；新增普通材料固定热容和导热边，先接测温以形成可观察的实施顺序。
- 材料余热使用同一section中的稀疏位置/材料类别记录；新材料0.25°C精度，原空气格式和精度保留，不能以空气均值伪造材料状态。
- 玩家材料长波采用有界接收者方向采样；红外按可见对象表面显示，明确专用几何pass、小背景表和实体温度同步的必要成本。
- 相变显热、全世界双向辐射和实体反写世界能量仍是明确的模型边界，不宣称本计划消除了所有物理近似。
- Documentation impact: 本轮仅改变计划，当前游戏行为未变；未将预期实现写入living docs。计划列明实施时应更新的五份climate文档。

## Validation

- 对照当前工作区的publication、topology、solver、dormant codec、温度计、玩家热模型与红外协议核对接口和假设，读取相关人类设计及最新开发记录。
- 三份Markdown共16个本地文件链接检查通过，未发现替换字符或行尾空格；两个计划的git diff --check通过。核对计划状态与数值预算；不编译或运行游戏测试，因为本轮没有代码改动。
- 计划中的性能门槛与状态开销是预算/验收要求，不是已实测性能。此前GameTest结果不作为新材料闭环的通过证据。

## Remaining

- 按计划A–G实施并执行真实Forge、客户端图像和100观察者负载验证；完成前保持计划ready/in-progress。
