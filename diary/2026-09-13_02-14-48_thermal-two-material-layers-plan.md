# 限定材料导热深度为两层

- Time: `2026-09-13 02:14:48 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `材料热模型plan范围收敛；未修改生产或测试代码`

## Completed

- 用户明确选择“从受热表面最多进入两层连续材料，第三层不再被带着加热”。据此更新[材料plan第5.4节](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)，同步不变量、换热连接、驻留、实施表和验收。
- 撤回原材料温差驱动的递归驻留前沿；两层限制同时作用于请求范围与M-M建边，第三层即使已有节点也不能绕过。
- 第2层只连接稳定归属的一个表层，不连接其他表层或第2层，防止两侧范围拼接穿透三层/四层墙。有效表层间接触保留，所有保留边仍按温差双向交换。
- 补充“气隙”定义：只是部分方块的可通风空间及既有V参数，不另建空气实体、温度、热容或能量状态；实心墙不可借空气通路跳过。

## Decisions

- 两层是用户指定的游戏模拟范围截断，不宣称真实材料在第三层绝热；不追踪每一份热量的来源或传播次数。
- 几何范围采用局部mask和第2层邻接归属，不新增材料寻路求解器。材料升温不是新的范围起点。
- 过滤边不发生Q；材料失去范围资格时保留H或交接保存，不清自然温度。其他独立物理输入与已有历史仍可影响对应材料。
- Documentation impact: 仅更新plan及本日记；living docs保持当前实现描述，旧日记保留历史结论。

## Validation

- 对照WorkerPageStore.collectResidencyChanges当前sourceSeedMask/hotBrickMask扩展入口，明确材料保留与空气扩展不能混用。
- 文档链接/空白检查及git diff --check通过；未编译或运行游戏测试，因为本轮无代码改动。

## Remaining

- 实施并验证六向三层夹具、第三层预先驻留、跨Brick/Page、两侧范围重叠、地表/地下扩展及封闭重开存档连续性。
