# 连续空气：新热源发现与进入恢复

- Status: `Current for selected implementation and recorded real-client entry acceptance; general multiplayer/default acceptance partial`
- Last verified: `2026-10-13`
- Scope: `continuousAir=true的新源空间准备、源发现队列与实际客户端加载恢复；不覆盖百人网络容量`
- Code anchors: `MinecraftThermalInput.tickOpen`, `MinecraftThermalInput.enqueueSourceDiscovery`, `PhysicalSourceSpatialIndex.discoverChunk`, `PhysicalSourceSpatialIndex.prepareNewTargets`, `PhysicalSourceSpatialIndex.flush`, `MinecraftPageManager.processAdmissions`, `OnlineEntryTrace`

## 实现与时间边界

源发现只更新源表与dirty标记。原路径在20tick边界的flush中才生成target页面种子，而页面捕获已先执行；新页面可能额外等待下一个20tick提交。早期真实客户端基线3周期24探针首次current平均46.75tick、最大55tick，20点超过40tick；首次恢复后的年龄/连续几何违反均0。

现在continuous Air发现处理后调用prepareNewTargets，仅提前准备PRESENT且尚未REGISTERED、TARGETS_CHANGED的新源空间目标。它复用现有dirty表、页面兴趣与有界捕获，不发注册/启停/功率事件，也不改变已有登记源的移动/重绑路径；真实源事件仍由原flush发出。20tick步长、每tick8发现区块、1新页、64Brick捕获和现有128MiB预算保持原限额。

提前页面准入会改变域恢复与热交付的时点。它没有新增浮点近似、修改热参数或checkpoint format5，但不保证温度逐位相同；有效温度与累计供能需以实际记录验证。短对照1472个双方fresh/current读数按同游戏tick对齐，最大差0.657°C；最终少日志长测在共同有效区间的1450个样本最大差1.370098°C，用户已接受这一实测差异。范围为8个B探针×3周期，不是重新认证全空间与函数表示的误差，也不是把任意新误差门槛设为1.37°C。

## 发现队列

暖存档长测仍出现393–454项队列，实际源区块rank372–422，发现等待47–53tick；E1初始8点失败、最长67tick。下一项依据该记录排除无发现工作的区块：continuous Air的enqueueSourceDiscovery仅排入getBlockEntitiesPos非空的chunk。

Minecraft该集合包括pending block-entity NBT和已实例化位置，尚未构造的已存BE仍会被扫描。discoverChunk本来只遍历该集合；空集合不会产生源事件。此过滤复用原队列，不增加真实扫描限额；chunk load的section owner/halo处理仍走attachLoadedChunk的原流程。后续实际方块改变由section mutation/source resync处理，点火仍使用原bootstrap入口。实际后续放置、900tick燃料/烹饪及磁盘重载已通过2 required；早期卸载/初始化/返回取消通过3 required。

## 验收口径与现有结果

开发诊断明确记录每点可见加载、发现、种子、捕获、提交、首次current及后续失效；初始40tick超时保留失败，并继续记录最终恢复。首次恢复后的连续几何失效独立计时，样本年龄仍≤40tick。诊断只在gameTest源码中；事件在内存缓冲并在完成时一次导出，避免同步日志FileWrite干扰主线程性能。

E2暖存档12000tick、3周期通过，首次current最大17tick，初始/年龄/连续几何违反0，最终256真实源/current成立，59550次查询，Root峰值约78.0MiB、停服0。601个worker供能余额记录闭合，WORK_LIMITED0。该运行仍带旧同步诊断日志，其尾延迟不能用作最终无污染性能证明。

最终少日志12000tick从初始原存档另建测试副本，实际599737ms，59530次查询，三周期24点first-current平均15.75tick、最大21tick，初始/年龄/连续几何deadline违反均0。Root峰值83552644B（79.68MiB），128MiB准入成立，停服Root0。12个native请求全部prepared、pending/fallback0；卸载热等待0，保存热等待总37µs、最大26µs。601个worker供能余额记录闭合、WORK_LIMITED0；记录的640次REGISTER分属不同源或生命周期，同源同generation重复注册0，功率均8000W。

只保留空队列过滤的简化对照从相同原存档另建副本，4800tick三周期：first-current平均33.5tick、最大41tick，2点初始deadline失败，年龄/连续几何失败0；有效温差最大0.977°C。它未达到原40tick恢复目标。因此最终保留提前准备与空队列过滤的组合，没有放宽恢复期限来选择更少代码的方案。

少日志长测post-teleport稳态8079tick：完整tick mean1.956/P99=3.789/max51.720ms；普通步行1758tick P99=6.853/max31.016ms。完整后台cut mean58.111/P99=219.219/max941.161ms。最大cut发生在首次targetTick16100，topology823.150ms、solve22.482ms、publish95.521ms；它是冷建场峰值，不能据恢复改善宣称后台固定提速。

同一JFR的成功活跃温场捕获371次mean0.538/P99=2.094/max17.318ms；最大在场景开始后51tick预热期。排除首400tick后357次mean0.456/P99=1.136/max1.696ms。捕获事件耗时与整体区块保存调用分别计量；不能把内部最大17.335ms当作通常每次温度查询或保存的成本。见[快照分布](../../build/online-entry-final-checkpoint-latency-20261013.json)。

日常唯一>50ms稳态tick为server6000原生自动保存，saveEverything49.410ms、完整tick51.720ms，GC和热snapshot重合0。两次自然保存level.dat最大25.517ms、队伍最大2.789ms；该场景不支持重构队伍存储。server1832/4231已加载基地传送附近的约51ms tick主要重合原版区块编码44.816/46.136ms。两个稳态tick启动间隔102.280/104.657ms分别重合GC53.037/59.873ms，不能把这两次节拍峰值当作热等待。启动期67.588ms tick只有稀疏CPU样本，不能精确归因全部耗时。最终强制flush完整tick172.836ms另计。

上述进入长测真实客户端只有1个，100FakePlayer是热环境消费者，当时保存由FH_CONTINUOUS_AIR_CHECKPOINT_TRIAL=native_ready选择。2026-10-14已接入普通continuousAirNativeCheckpoint配置，并通过正常配置回归与CT2副本客户端/独立重读，见[当前保存说明](continuous-air-persistence.md)。多维度和更多真实连接的容量资格独立验收，不因本项完成而标记默认或百人服通过。

## 最大温差的位置与解释

最终最大差发生在第三次返回B基地，上层第二房间的probe13，采样空气方块(2577,88,2050)，实际查询点(2577.5,88.3125,2050.5)，其下是真实篝火(2577,87,2050)。`gameplayPlacedReservoirEnvironment`使用x/z+0.5、y+0.3125，查询点略低于方块中心。候选elapsed3280/gameTick19378读取71.517956°C，对照同gameTick插值70.147858°C，差1.370098°C；两侧自然背景均-12.5°C。对照插值使用gameTick19377与19397两个有效读数，间隔20tick、没有外推；这不是人体或全地图每点温差。

该点旧路径loaded3213→discovered3229→seeded3243→captured3244→submitted3263→firstCurrent3265，加载后等52tick。组合路径loaded3208→discovered/seeded3208→captured3209→submitted3222→firstCurrent3224，只等16tick。按实际游戏时刻，first-current19362→19322，提前40tick；两轮原版可见加载时刻也不同。最大差读数两侧均使用已推进到19360的空气样本，不是拿缺场自然回退当误差。候选该篝火在19320以8000W注册；E0未录制完整source CSV，不能精确断言旧路径同一源的注册tick或把温差逐项归成多少焦耳。

这支持温差来自发现、几何捕获、恢复时序与此前温场历史改变。热方程、数值表示、停止阈值和功率没有在本项调整，但真实供热演化时序确有变化。三个周期有效样本的绝对差平均0.918/1.012/1.082°C，最大1.059/1.278/1.370°C；不能把总体差异称为仅一瞬间的孤立异常，也尚未测出每个阶段贡献。

证据：[执行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)、[最终报告](../../build/online-entry-final-closure-20261013.json)、[最终有效温度对照](../../build/online-entry-e0-final-temperature-20261013.csv)、[最终长日志](../../build/online-entry-final-20261013-r1.log)、[仅过滤对照](../../build/online-entry-e0-filter-report-20261013.json)、[供能记录摘要](../../build/online-entry-final-20261013-r1-energy-summary.json)、[本项生产改动](../../build/online-entry-final-source-diff-20261013.patch)。

## 2026-10-14 单核恢复时序的独立边界

整个Forge JVM固定到i7-12800HX单线程E核的两次有限运行，均为普通SHARED_GRID/native、总256篝火/256熔炉/100热消费者。暖到约2400tick再真实卸载返回：8点源发现均loaded+1tick、Brick捕获+2至+6tick完成，但输入在+15tick的target2420才提交。最后捕获至提交仍有9tick；当前准备和热推进共享20tick输入边界。

两次恢复任务wall998.725/1113.700ms、workerCPU250.000/93.750ms，topology769.490/555.612ms；都在原40tick时限内未交付current，且早期已有新鲜度违规。提交观察含回调开销，batch创建至worker开始不等同纯排队；墙钟阶段变化也不等同热CPU变化。原时钟追赶、40tick、预算和热参数未放宽。

忽略arena生命周期身份、按相同有序空间输入复用网格的候选额外命中仅1次，发生在重载前，关键恢复无额外命中；无足够目标收益，已经撤回，最终生产热代码不变。单核8项兼容性通过来自此前独立运行，不能用来证明256源容量；这里也不是全硬件/稳态最大容量结论。更早本机多核进入验收与已接受温差仍保持原范围。

本项没有新的有效温差认证：重载8点在失败前没有current，不能拿自然回退值对照。剩余独立几何准备需要处理候选失效、预算和提交，未在本次小修中实现，也未承诺一定满足单核冷期要求。证据与有限停止结果见[本轮报告](../../build/single-core-recovery-final-20261014.md)、[执行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。
