# 专用热玩法退出公共入口的源码调查

- Time: `2026-10-31 18:34:03 +08:00`
- Author: `Codex; OpenAI GPT-6; engineering investigation`
- Status: `completed investigation; implementation pending`
- Scope: `篝火源变化、气候随机资格、岩浆辐射依赖与删除条件`

## Completed

- 对照源状态表、Section待办、物理源索引、气候循环、配方重载和辐射面读取范围，写入[具体移除方案](../plans/2026-10-31_18-28-24_thermal-public-specialization-removal.md)。
- 方案覆盖对象生命周期、快照确认/回滚、复用原源待办、删除跨层篝火标志；气候循环自身筛选与原版资格恢复；原辐射缓存按11个实际读取依赖位置合并失效。
- 旧计划和当前运行合同链接新方案，明确尚未实施。

## Decisions

- 保留真正共有的材料/几何/保存和数据失效通知，不把对象判断换个全局入口后声称已删除。
- 随机调色板方案以额外palette检查换取移除BlockStateBase钩子，披露残留调色板条目及外部查询/随机序列变化，不宣称无代价等价。
- 无生命周期的任意裸Section改写无法同时得到无公共观察、无复查和无条件发现；合法BE建立/加载与既有结构重同步必须单独验证。

## Validation

- 只读代码及当前Forge47.3.0映射字节码，核对Campfire.onRemove、BE安装后的onLoad和palette.maybeHas/count接口；没有修改生产/测试源码、构建或运行测试。
- Documentation impact: 新增有明确状态和Outcome的计划，并在当前合同链接为未实施方案；旧日记不改写。

## Remaining

- 尚需实施与重点验收；源码目前仍包含上一条日记确认的公共源维护路径。
