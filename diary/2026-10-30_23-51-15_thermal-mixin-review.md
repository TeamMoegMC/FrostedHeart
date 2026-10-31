# 热模拟 Mixin 完整链路研究

- Time: `2026-10-30 23:51:15 +08:00`
- Author: `Codex; OpenAI GPT-6; source investigation and engineering review`
- Status: `completed investigation; recommendations not implemented`
- Scope: `热运行时、篝火及随机相变基础接入，共11个正式Mixin`

## Completed

- 逐项检查此前7个直接接入和1个篝火配套，并补审控制随机资格、原版融冰和原版结冰的3个间接接入。此前7＋1不能代表完整相变链路的依赖数量。
- 研究结果集中在[逐项报告](../plans/2026-10-30_23-51-15_thermal-mixin-integration-review.md)，记录每项用途、可替代程度、具体优化、性能/语义边界、统一目录与业务归属、删除清单及最小验证范围。
- 核对原版saveChunkIfNeeded的实际save调用、随机循环与Ice.playerDestroy字节码；对照Forge47.3.0保存与邻居事件补丁，确认其时机和覆盖不能直接替代全部现有钩子。
- 确认可研究的具体改点：维护保存准备进入更准确的调用位置、原版随机资格true快路径、雪的重复相变观察、篝火规则集中、Ice.playerDestroy窄拦截。
- 确认Biome.shouldFreeze抑制也被原版SnowAndFreezeFeature使用，不能按当前自定义tickChunk不调用就删除。

## Decisions

- 以职责集中、减少重复工作和原版流程接管为目标；不设“必须删除几个Mixin”的数字，不用一层转发代替业务归属整理。
- 保留必要的底层变化与保存交接，不新增轮询扫描、事件总线或另一套定时器。ServerLevel整段循环收窄和岩浆触发器删除尚需具体等价实现，未称作已经证明可无代价完成。

## Validation

- 只读源码、注册、生成数据及本地依赖字节码/Forge补丁；本轮没有编译、JUnit、GameTest或生产服务器测试。
- 生产源码保留原工作区状态；没有实施或撤回先前17:46的源发现改动，没有修改城镇、世界生成、design或受保护旧粒子。
- Documentation impact: 新增研究计划并在现有热系统待办中链接。当前行为文档无需更新，因为本轮没有改变实现。

## Remaining

- 报告中的实施建议和对应验证未执行；没有新的性能提速数字或生产资格。
