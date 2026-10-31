# 专用热玩法退出公共方块入口

- Time: `2026-10-31 19:55:41 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and scoped validation`
- Status: `completed`
- Scope: `篝火源生命周期、气候随机资格、辐射几何依赖、删除旧公共专用链与重点验收`

## Completed

- 篝火onPlace/onRemove和自身BE.onLoad通知onCampfireStateChanged，捕获中的旧源重读用原每源标志字节一位和dirtyOrder，不新增队列或源权威。tick末/flush前核对最终世界；显式卸载不复活，迟到旧BE不操作新实体。
- 删除状态表篝火标志、SOURCE_MUTATION、sourceRelevant/pendingSourceMutation、Page对物理源索引的依赖、逐位置源同步、整Section源扫描，以及失去用途的nonGeometry缓冲。结构替换只失效已登记源依赖；后台原始容器替换通过原服务器队列回主线程并检查旧owner有效性。
- 删除BlockStateBaseMixin_RandomTick和LiquidBlockMixin_ThermalRadiation及登记。气候循环用原版资格和当前调色板判定；配方缓存更新不再遍历世界重算原版Section计数。辐射缓存消费通用变化，11个实际依赖位置归并最多6个不同Brick，含对角Section、Chunk生命周期和同profile流体高度。
- 相同受影响生产范围净减218行（8203→7985），没有新生产类型/世界事件队列；JAR有9个集中热Mixin、0个退役专用类、0个GameTest类。

## Decisions

- 保留真正共有的材料/几何/保存/加载与通用数据失效；完成目标是专用对象业务退出普通方块入口，不宣称整个热系统没有共享链路。
- 维持源功率、cut、数值内核、粗档、多核、v6及现有辐射公式/刷新额度。随机资格外部查询恢复原版，调色板残留可能多抽无效样本和改变随机序列；没有新CPU收益或全局温差资格。
- 支持合法生命周期/BE及显式结构重同步；未知的裸Section写入且不维护BE不承诺无条件发现源。

## Validation

- 写完整批后统一构建，首版assemble/compileGameTestJava及15项全部通过。最终调用约定复查补齐后台Raw容器入口的主线程交接，并扩展现有源用例；最终再次整体构建，同轮源7、接入6、燃料2共15项全部通过，热错误0、关闭rootBytes=0。构建与测试版本分别记录，没有把首版结果冒充最终版。
- 覆盖捕获放置确认/回滚、旧源捕获移除确认/回滚、迟到BE、异步容器替换、整Section身份替换、实际Packed Ice相变采样/零速度/配方缓存重载、辐射跨Section邻居上方/墙开闭/同profile高度、燃料与实际保存重载。
- 依赖归并公式对12288个正负/边界位置与11点直接依赖逐一核对，结果一致，最多6个Brick；这是静态几何校验，不当作额外GameTest项。
- [完整验收JSON](../docs/climate/validation/thermal-public-specialization-removal-2026-10-31.json)保留两批构建和测试。未运行JUnit、性能大矩阵、历史工程复制构建或路径敏感hash。
- Documentation impact: 更新[运行合同](../docs/climate/thermal-runtime-control.md)的当前9个接入、源/随机/辐射职责与取舍，修正[冰锥说明](../docs/climate/icicle.md)中旧资格Mixin接线；[本计划](../plans/2026-10-31_18-28-24_thermal-public-specialization-removal.md)完成，前批残留计划转superseded，后续工作入口指向新资格。旧日记不改写。

## Remaining

- 本批未重新运行独立生产JAR重启或百人联网场景，已有记录保留旧编译资格；没有新性能百分比。
- 改动未提交/推送。城镇、世界生成算法、design和受保护旧粒子未修改。
