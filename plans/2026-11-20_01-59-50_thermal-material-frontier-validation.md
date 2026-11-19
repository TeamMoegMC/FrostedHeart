# 材料边界扩张与候选开销对照

- Time: `2026-11-20 01:59:50 +08:00`
- Authors: `OpenAI Codex`
- Status: `superseded`（两层候选筛查已完成；用户随后取消材料层数限制）
- Scope: `WorkerPageStore`、`TiledThermalField`、篝火驻留及 Forge 验证。
- Related: [余温退出](../docs/climate/thermal-residual-lifecycle.md)、[篝火记录](../diary/2026-11-20_03-02-02_temperature-gameplay-and-weather-history.md#campfire-six-face-heat)。

## 已核对状态

起始材料扩张只检查 `BlockBrickLayout.airContactBlocks`，上一轮以篝火六邻 source seed 绕过完全封闭的篝火缺口。用户澄清后确认不能把这个限制直接当成无界材料扩张的缺陷：必须同时保持两层实体接触上限。当前授权要求先验证正确性和性能，不能将 Brick 覆盖数量等同于 CPU 实测。

## 对照

在同一构建中比较直接逐面判定、几何候选掩码和与驻留温度扫描合并的摘要。摘要必须取最终 Accepted 状态；求解中间态还会被后续相邻面和后半段输入改变。记录候选准备、更新、扫描与实际求解总成本及分配；循环换序、预热后配对计量。纯内核对照与实际生产主线程/worker 集成明确区分。

以完整捕获场景为精度参照，覆盖完全埋藏的热/冷材料、多层墙、实体到空气、半砖接触、温差迟滞、自然相变、材料替换及余热。移除篝火六邻强制引用前，验证通用规则能够接管。保持现有 1°C / 0.5°C 阈值与材料自然相变参考温度；新增区块只从已加载世界读取。维护 owning docs 并追加开发日记。

## 用户约束补充

用户在本轮明确“两层”是实体传播深度上限。旧 `TopologyView.materialContactAllowed` 采用表层与单一第二层归属，当前固定格核并未使用这一判定。新对照必须在同一个两层接触规则下进行；无界逐层扩张候选取消。修复同时覆盖已经驻留的格子，防止只限制新增 Brick 而仍在 Brick 内穿透厚墙。篝火本体作为直接接收功率的材料表层，周围实体可为第二层。

## Outcome

新增统一 `MaterialContactPolicy`，固定求解器使用每 Brick 三个 long 的接触缓存，参考编译器与材料边界共享同一规则。篝火仅保留正功率端口目标，通过带温差门槛的表层请求纳入其他六邻 Brick。修复三轴角点和远端风道表层变化的缓存失效范围，均为一次局部更新，不递归扩大驻留。

同构建筛查完成直接扫描、边界候选缓存、最终 Accepted 合并扫描三种实现，另测数值接触直接判定/位掩码、重建、正式求解与几何提交的总观察。最终 28 项必需用例全部通过。保留换热接触位缓存；边界候选缓存和摘要没有稳定的普遍优势，不进入生产。结论与范围见[验证报告](../docs/climate/validation/thermal-validation-reports-2026-11-19-to-20.md#thermal-material-frontier-2026-11-20)及[原始计量](../docs/climate/validation/thermal-validation-data-2026-11-19-to-20.json#/records/thermal-material-frontier-2026-11-20)。

本轮没有证明全服最优，也没有证明两层玩法限制相对取消限制的净性能收益。用户随后询问取消限制是否无限扩大以及成本是否值得；已区分温差门槛、材料深度和整体资源范围。未收到取消指令，故保留两层约束。长时间、多个建筑分布及百人集成仍属后续资格工作，不将单场景筛查包装成这些结论。

## 后续取代

2026-11-20 用户随后明确要求先取消“第三层不传热”。已移除层数策略、唯一归属、专用接触缓存及其几何依赖，并让所有有足够扰动的边界材料按实际接触请求下一 Brick。1 K / 0.5 K 迟滞保留，六邻强制 seed 不恢复。42 项定向回归通过，见[取消深度限制记录](../diary/2026-11-20_03-02-02_thermal-runtime-and-material-history.md#material-depth-limit-removed)。上述两层测量保留为历史结果。
