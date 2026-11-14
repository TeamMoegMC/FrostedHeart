# 热模拟运行时：固定温场、输入与保存

- Status: `Current for the production fixed-field implementation; retirement validated by full 156/157 plus corrected source-focused 8/8, 157 distinct functional checks; hundred-player performance qualification incomplete`
- Last verified: `2026-11-14`
- Scope: 正式生产运行链路、输入所有权、物理时间、查询、热数据v7与关闭恢复。
- Code anchors: `MinecraftThermalInput.createWorker`、`ThermalDimensionEngine`、`TiledThermalField`、`SpatialSourceIngress`、`SpatialHeatStore`、`SpatialHeatBuffer`、`QueryPublication`、`RegionalCheckpoint`、`AirFieldCheckpoint`、`DormantChunkThermalState`。

## 正式入口与数值状态

`MinecraftThermalInput.createWorker`只创建`TiledThermalField`和`QueryPublication.createFixed`。`ThermalDimensionEngine`只协调这一套固定温场；旧`RegionalAirRuntime`、`RegionJob`、`RegionBuild`、工作队列及粗细切换运行链路已经退役。没有生产后端切换选项。独立图构建/数值参照所需的六个类只在GameTest源集中，正式JAR不包含它们。

每个Tile的Accepted帧和`sampleTick`共同拥有实际值及其计算时刻。`acceptedTick()`只是现有采样时间的诊断最小值。`domains()`按已捕获邻接和已安装通路形成Section成员集，不拥有额外时钟、热量或任务。相互传热的空间一起推进；断开的基地可分别推进。实际起止区间完全相同的域可以在原已付费输入容量内共用事务。

源、输入和数值只有一条生产链路：`SpatialSourceTimeline` → `SpatialSourceIngress` → `SpatialHeatStore` → 一个范围内的Claim和`SpatialHeatBuffer` → 固定温场提交。Store拥有已接受输入，缓冲只保存可取消的准备副本及已提交的deferred J；取消不扣原输入，提交只确认一次归属。其他范围的余额不能随当前范围提交而清除。

## 输入、几何与物理时间

`transportThrough`在同批几何引用完成交接后才更新。数值截止同时考虑输入前缀、真实几何、源控制和原20tick切片。源热量可先记录到控制边界，源启停状态等实际接收方达到该边界后才生效。

`activeSince`是当前活动寿命的起点，不随求解推进。首次捕获或原生重入之前的控制在该起点初始化；它不代替`sampleTick`。旧Page身份不能约束或退休同坐标的新Page。

真实几何在受影响旧温场到达事件切点后安装。纯扩容留在原Page队列等待容量，已有空间继续计算；混合输入中的实际修改、收缩和退休优先作用于已付费空间。冻结写回期间不安装可选扩容。普通几何不能通过直接修改采样时间冒充补算完成。

## 查询与读者生命周期

`QueryPublication`只发布几何头和固定温场的实际提交版本。普通点查询直接读取Accepted，`ReadCursor`通过Page几何与世界单元地址读取温度、材料及采样时间。旧数值槽位查询、区域查询索引、区域帧租约和payload家族计账已经移除。

私有Working值不能被查询或普通SAVE读到。读者在提交后检查版本；关闭使旧读者失效，仍借用的Page元数据额度在最后一个读者释放后归还。超过原40tick有效期的数据继续按原玩法路径回退，不能给旧值重打新时间戳。

## 区块热数据v7

用户明确允许舍弃旧区块热数据，因此`FrostedHeartThermal.version`改为7，不迁移v6。版本不匹配时忽略整个旧热数据根，包括旧空气温度、材料储热和待处理热输入。这个决定只作用于该热数据根，未修改原版区块、物品或实体的存档处理。

`AirFieldCheckpoint`只保存Section原点、Brick/单元掩码、每个单元的实际温度和采样时间，不再保存旧角点、形状、模态或系数。加载当前v7值时直接恢复单元能量，休眠冷却从原采样时刻计算。合并、过滤和重新编码不能重置已保存数据的年龄。

材料继续使用`MaterialSectionState`保存状态身份、材料定律、焓、分支与采样时间。`RegionalCheckpoint`这个现有名字保留，但内容已收敛为Accepted Section空气/材料值和原输入所有者的pending J，没有旧区域帧列表、Part索引或FrameLease。

普通SAVE在原值/输入事务下取得一致快照；数值私有计算不持有这个保存事务。原生UNLOAD冻结真实Accepted值，保存不要求数值追平服务器tick。取消卸载后先完成原生回执，由worker推进相关邻区到真实重入边界，再按原休眠合同接回。

## 资源与故障

本轮没有调整进入1°C、退出/保存0.5°C、误差`max(1°C, 5% × |参照温度−环境温度|)`及工作额度。配置`continuousAirWorkBudgetMsPerSecond`仍按任务执行经过时间计量，包含抢占和GC，不能当作纯CPU。128MiB仍是原资源计账目标，不是已证明的实际可达堆硬上限。

旧投影专用的`continuousAirCorrectionToleranceC`配置及其引擎参数已经移除。当前构建/材料/源所共用的拓扑和物理定律继续使用原实现。

worker故障取消未提交状态，原Store、已提交温场和源窗口保持一次归属，经原保存/替换链路恢复。关闭清空原生回调持有关系，不能让旧场经保存回调继续存活。

## 验证及边界

完整矩阵157项中156项通过，最后一项原区域断言遗漏了当前固定引擎持有的pending J；只修正该测试后，相关8项全部通过，严格验证40/90J余额、材料不重复吸热和完整守恒。两轮生产逻辑相同，157项不同检查均有通过证据；没有把后续专项写成又一轮完整157/157。六次实际故障均存盘读回并回收旧场，v7写入/读回及旧版本忽略通过。

本轮退役范围、迁移测试、字节码依赖检查与最终结果见[生产运行链路退役回执](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-production-runtime-retirement-2026-11-14)。此前[提交时间批次](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-committed-domains-2026-11-14)和[第一轮小范围清理](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-production-cleanup-2026-11-14)只证明其对应代码。

性能优化按用户要求暂停。此前百人时效、主线程耗时、分配量、完整128MiB可达峰及8h/24h耐久门槛仍未完成，本轮清理与v7保存验证不能替代这些资格。
