# 空间构建边界遍历优化

- Status: `Historical for the 2026-11-10 regional implementation; retired regional types are not the current production backend; passage overlap remained unqualified`
- Last verified: `2026-11-10`
- Scope: 空间构建前的Section影响闭包、区域依赖目录和安装/取消生命周期。
- Code anchors: `TopologyUpdatePlanner.continuationBoundary/continuationBoundaryBytes`、`AirRouteCompiler.Component.dependency/advance`、`RegionalAirRuntime.OwnerPage/OwnershipPreparation/expandBoundary/holdBoundary/boundaryReady`、`AirLoadTable.recordBacking`、`ThermalDimensionMailbox.execute/closeEngine`。

## 实现

`TopologyUpdatePlanner.continuationBoundary`继续保留输入周围27个Section、待发现通路、已捕获/候选/已公告几何及通路依赖Section的保守相邻闭包。已删除其前置的逐通路组件闭包遍历：每个`AirRouteCompiler.Component`从一个位置开始，只沿六面邻接发现新位置，且把每个已访问位置与六面邻居登记到`bySection`。因此一个组件的依赖Section是面相连的，同一Section遍历已覆盖原组件闭包，重复收集组件及扩容visited集合没有额外覆盖作用。相应临时额度不再包含该visited组件集合。

`RegionalAirRuntime.OwnerPage`在已有cell/air目录中增加`dependents`，同时登记区域自身scope和外部dependencySections。目录在原`OwnershipPreparation`中准备：拷贝未替换区域、加入候选区域、移除被替换区域的全部旧scope/依赖条目，沿原InstallToken一起可见；取消恢复previous，完成清理断开previous。只有依赖、没有拥有单元的Section仍保留目录项，不能按cells为空提前移除。

区域闭包改为从边界Section沿该目录遍历，每个被触及区域只扩展一次；区域hold及ready查询也从目录选取实际关联区域。原先反复扫描全维度区域以求固定点的计算退出这些入口。新增目录容器纳入`recordOwnerPage`的既有交接盘点，候选和上一版仍按实际持有期计账。

此批没有修改热传导公式、细网格表示或存档格式。它减少构建前的重复遍历，没有把保守影响范围缩成改块所在的单个Section，也没有改变输入时钟触发全量边界刷新的现有条件。

压力录制进一步确认同一维度的全部`ContinuousAirSolver`会在盘点时分别调用共享`AirLoadTable.recordBacking`。原`AirBackingMeter`只去重了各数组的字节数，方法仍重复遍历整张共享表，形成区域数乘源绑定数的额外开销。现改为用既有meter的身份集合在方法入口标记整张表，同一次盘点只遍历一次；meter清空后的下一次盘点仍读取实际容量。没有添加跨盘点缓存或第二份内存账本。

第一轮千源运行在关闭阶段暴露原`ThermalDimensionMailbox`的交接竞态：执行回合释放monitor、进入QUEUED后，在monitor外调用`engine.whenRoundReady`；主线程关闭可派发closer并同时修改质量队列，真实记录到`ConcurrentModificationException`。未修改的编译对照也重现同一堆栈。普通回合现于同一mailbox monitor内核对queued/waitingRound并登记/武装唤醒；closer的后继登记也持有该monitor，避免立即回调启动另一closer时并发访问。首次千源数值断言通过但整轮记录失败；最终版本的千源、核心回归及关闭均无热系统异常。

## 验证与限制

当前必要编译通过。最终同一编译的几何输入顺序、跨Section未安装连接、卸载检查点、暂停worker后的真实磁盘保存、并发保存及材料/通风模型，与新增目录生命周期对照一起运行，10/10通过；实际22次区域安装、43个Section闭包对照无差异，21个完成准备对象在热运行时仍存活时全部回收。另有曲线、普通双基地和千源各1项，共13项通过。测试产物初次缺少一个现有世界生成测试内部类；补齐测试编译后运行。新增测试初次使用私有字段导致编译失败，改用已有公开访问器后通过，失败日志保留。

额外的`frostedheart_pending_route_boundary`仍未获得强交错资格。第一次混合批次不满足该场景的全世界257源前提；独立世界的新旧版本都实际执行三次远处拆建，但均未观察到要求的连续四个输入窗口跨越未完成构建。两版均有621个本地样本、最大年龄20tick、不可用/几何错误0，源出口与本地几何hold均实际观测成功，最终同样因覆盖断言退出失败。没有放宽断言、虚构重叠或将该项列为通过。这个限制独立保留，不能把13项通过扩展为所有长时交错条件已经验证。

四个实际房间包含封闭、开门、多源和隔墙，并执行点火、开墙、熄火。包含共享表和交接修复的最终版重新完成2600tick轨迹，包含3120个空气样本和1560个材料样本；最大绝对温差分别为0.0006420579741508448°C和0.000007462731726803895°C。sampleTick及可用性不一致均为0，整段0.1°C比较通过。该微小差异是完整异步运行的观察结果，没有宣称任意输入都位级相同。

原始证据在`build/spatial-boundary-20261110`及`build/thermal-baselines/spatial-final-20261110`。温差参考为`spatial-before-r2-20261110`；性能参考为没有编译干扰的`spatial-before-clean-20261110`。初次普通负载参考与编译重叠，已排除其性能百分比。所有运行只使用现有编译对照和当前必要编译，没有重新编译历史源码或制作发布包。这些本地生成文件不入库；旧的内存泄漏因果结论仍见[内存证据](thermal-server-memory-evidence.md)。

归档的范围、原始计数和逐项资格见[本轮验证回执](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-spatial-boundary-2026-11-10)。

## 已完成的负载测量

共同配置为Java17、4GiB堆、四个热worker、128MiB热准入目标、1000ms/s活动额度和realtime20。千源场实际放置1024个有燃料的营火，替换一个已经生效的热源，执行1600tick并实际保存三次。对照和最终版的测量窗口均约80秒；原对照在负载结束后重现相同关闭竞态，故对照的整体功能状态为失败，性能统计限定于故障之前的负载窗口。最终版整个场景通过、热错误0、关闭rootBytes=0。

| 指标 | 对照 | 最终版 |
|---|---:|---:|
| 几何处理lane累计执行片段，秒 | 21.3308916 | 7.7287579 |
| JFR协调回合录制时长累计，秒 | 34.6527007 | 17.9589453 |
| 热worker Java执行采样数 | 2640 | 1120 |
| 含airBackingBytes的热worker采样数 | 1732 | 275 |
| 独立对照房间最大样本年龄，tick | 37 | 24 |
| 不可用tick / 读取失败 | 0 / 0 | 0 / 0 |
| 实际保存次数 | 3 | 3 |
| 可选准入拒绝次数 | 4515 | 7833 |
| 整轮热计账峰值，B | 239244978 | 242679746 |

几何lane片段下降约63.8%，协调录制时长下降约48.2%。前者包含该lane中的几何准备及交接盘点；后者的JFR事件在回合前置刷新之后开始，不能描述成完整协调者CPU时间或服务器总CPU下降48.2%。采样数量只作调用归属和成本方向佐证。

这批优化没有同时降低所有资源指标。压力窗中JFR线程分配计数区间给出的速率约57.26→63.27MB/s（54.61→60.34MiB/s），采样区间在约80秒窗口内且并非全窗精确总量；拒绝/恢复尝试也增加，热登记峰值增加约3.28MiB。实际构建region/epoch数3404→3247、细粗工作调度有变化，不能把全部差异解释成单次相同候选的速度比。

普通双基地（256营火、100温度消费者、2400tick）前后均4300次数值提交、12000次查询、服务违规0；构建region/epoch为282→283。热线程native执行周期18.256→18.712十亿，累计分配1,790,159,536→1,853,440,944B，几何lane片段2.241→2.274秒。该场没有证明整体提速；平台ThreadMXBean CPU毫秒读数仍不用于速度结论。此批主要收益在大规模区域共享表和影响范围的重复扫描，普通细网格数值组装仍是后续热点。
