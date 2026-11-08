# 缓倾岩面与矿化交会：代码、对照与效果评判

- Time: `2026-11-01 21:00:00 +08:00`
- Author: `OpenAI Codex / GPT-6，实现与实机评判`
- Status: `partial`（本轮版本/测试完成；视觉及FULL性能尚未全部达到目标）
- Scope: 伴随包测试Java、验证工具及FH知识文档，未替换正式安装

## Completed

- `roof_style=planes`复用列缓存形成两缓倾岩面及约8格过渡，停用该模式中的三个独立凸岩。
- 主矿化面在池上方局部膨大，现有方解石/石灰岩及深板岩表现充填与碎块。一次真实首轮后减小过重的暗碎块，保留迭代数据。
- 新增矿化开关、当前区块材质前后只读日志、保存方块局部审计；未添加新游戏方块、Mixin、3D octave噪声或tick管理器。
- [全部实际结果](../../TheWinterRescue/Scripts/worldgen/deposits/terrain_proof/results/2026-11-01-plane-roof/REPORT.md)。

## Decisions

- 原热液ID及完整54项运行时feature保留；没有用清空背景/增加矿石总量改善画面。藤蔓未过滤。
- 连续矿化在开关对照中成立，B/C最终充填保留；近景能读碎块，远景仍偏直浅带，因此只判部分视觉通过。
- 未把一次FULL请求耗时差异当成纯算法CPU；发现上升后追加预热NOISE配对，仍保留全流程未验收结论。

## Validation

- Java17编译/reobf、Python语法；真实NOISE/Aquifer；区块(0,0)98,304方块差异0。
- 四个完整客户端新世界正常退出：旧形状、首轮、关闭新增矿化、最终版。相同种子/锚点/主相机，原热液水色#77E0F7、随机刻3。
- 同711,349位置包络全部原热液quart；五主矿种新旧差异<0.35%。A有原内容形成的平滑玄武岩，不将全部形态归因于新公式。
- FULL请求总等待56.241→74.341秒，P95 403.87→649.34毫秒；外围同时变慢。5轮预热配对NOISE矿区中位数52.265→53.236毫秒；小样本不代替全流程/分配量验收。初次无效主线程CPU字段未用作CPU证据。
- 121.888秒/2,438刻观察：水1770→1765，气泡柱742不变。5处最终均为石头、上方熔岩，对应LavaFluid.spreadTo向下遇水规则，保留逐格和字节码证据。未人为补水，未称600秒完全不变。
- Documentation impact: 更新world-generation.md可选模式说明、审核稿状态和计划结果，报告明确通过/部分通过/未通过。

## Remaining

- 顶部交会远景主次、全流程性能归因、多种子/分配量、自动分布和正式安装。
- 固定样板：`D:/Frosted-Heart/build/hydrothermal-plane-roof-final-20261101`；存档`deposit-inspection-1793536407728`，手动入口`open-plane-roof.cmd`。
