# 热模拟接入集中与重点验收

- Time: `2026-10-31 01:55:57 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and scoped verification`
- Status: `completed selected implementation and stated validation`
- Scope: `11个热模拟接入、实际气候/篝火业务、源与保存拒绝链、正式包验证`

## Completed

- 11个接入类迁入mixin.minecraft.thermal并连续登记，旧包类与旧配置项退出。没有新增启动配置、反射/ASM/AT等生产访问手段。
- 源登记与材料修正统一到Chunk最终写入通知；删除专用热onPlace及旧入口。显式补查重置旧快照，普通续读保留进度；现有启动等待记录保存位置并在额度版本变化后恢复，关闭时清理。
- Section通知复用owner和状态编码；资格判断原版true时直接返回。BE判空采用公开API，删除工具性父类/构造适配。
- 保存钩子移到saveChunkIfNeeded内部实际save周围，删除冷却表依赖与外层重复筛选；干净区块不创建封存，准备拒绝不写盘，prepared在finally退出。原卸载future/取消协议保持。
- Ice.playerDestroy副本改为原掉落和精准采集之后的窄温度拦截。原版随机融冰和全局禁冻规则保持。
- ClimateBlockUpdates与CampfireGameplay持有实际共享实现；原随机循环、随机调用顺序、雪直接调用保护、岩浆薄通知、LifeTime和燃料边界规则保持。所涉生产代码相对本轮开始净减少115行，测试与文档另计。

## Decisions

- 不把把代码搬出Mixin称为已经消除原循环接管；保留范围和局部公开API复制成本明确记录。
- 当前维护保存的封存步骤仍有同步接管等待；钩子收窄不等于自动保存零等待。
- 启动回归首轮错误地假设满软额度必然拒绝有界启动。修订的是测试前提，生产额度规则和实现未为通过测试调整。

## Validation

- 先整批实现和静态闭合，再assemble/compileGameTestJava，首次通过。静态比较确认移出的天气、积雪判断和植物方法操作顺序保持。测试前提修订后只重编测试源码。
- 最终重点回归13项通过：源5、实际燃料/存档2、NativeCheckpoint3、真实维护保存/破冰/雪直接调用3；各进程热错误0、关闭rootBytes=0。首轮源4/5与失败日志保留。
- 正式JAR在独立Forge1.20.1-47.3.0、Java17、4GiB堆读取已有v6测试世界，继续保存并正常退出，热错误0；磁盘样本540→840tick、系数797个均有限，燃料18643→18359。无GameTest类/旧热Mixin类，受保护旧粒子4类保留。
- 完整记录：[验收JSON](../docs/climate/validation/thermal-integration-2026-10-31.json)。未运行JUnit或完整性能矩阵，没有复制历史工程重编或路径敏感hash门槛。
- Documentation impact: 更新运行合同的统一接入、业务归属、源恢复、公开API成本与维护保存时序；更新气候入口和原计划Outcome，历史日记不重写。

## Remaining

- 完整原版随机循环复用、辐射通知归并未实施，按批准范围保留。地面投料的旧满容量边界未混作等价整理修复。
- 本批没有整体CPU提速百分比、完整整合包或100联网玩家资格。独立服务器使用原9个模组/库与有效纸张研究目录，没有真实连接玩家；生产进程未启用根记账盘点。
- 改动留在工作区，未创建新提交或推送；研究界面、世界生成、城镇等其他既有改动没有选入本批。
