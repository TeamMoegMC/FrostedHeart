# 温度提交前简化、恢复活性修正与重点验收

- Time: `2026-10-29 23:04:00 +08:00`
- Author: `Codex; OpenAI GPT-6.1 Sol; implementation follow-up and validation, continuing GPT-6 Astra work`
- Status: `completed for selected optimization and targeted validation`
- Scope: `thermal源控制、质量恢复、查询页容量、空队列与待处理热量保存复制；排除城镇及旧粒子`

## Completed

- 按[计划](2026-10-29_23-38-00_thermal-development-history.md)完成A1/A2/S1/S2/S4和受限S3；生产12文件净减300行、无新增生产顶层类型。删除ThermalSourceBatch/Builder/applyEvent和旧batch/index回调；普通结构只继承/降粗，恢复沿专用入口；查询Page一次预分配，cells/regions容量按实际先删后加准备；空队列不创建迭代器，保存删除覆写复制。
- 修正真实源UNLOAD漏观察及空快照最低准备开销，诊断日志退出；原断言保留并补真实LIT=false检查。
- 删除隐式恢复后首轮压力迟迟不恢复。核对现有JFR与代码发现自动选档没有进入hasReadyWork，暂时受阻的自动候选还会从ready退出；现共享selectedStep并保留原集合项，无新队列、状态或退避阈值。
- Documentation impact: 更新[运行合同](../docs/climate/thermal-runtime-control.md)和[存储/性能](../docs/climate/thermal-storage-optimization.md)，新增[结构化验收](../docs/climate/validation/thermal-preproduction-simplification-2026-10-29.json)。历史日记与design保持。

## Decisions

- 保留共享多核、区域独立发布、粗档保护、输入J所有权和v6格式。本批未引入近似、调整额度或降低测试条件。
- 临时容量不足仍允许降粗；验收关注持续服务、真实保存、撤压恢复及释放，不把小房间在全局压力下降粗单列为失败。
- 第一轮CPU周期升高后仅复测双基地。两次结果都保留，不从少分配推导CPU大幅提速，也不把热登记软目标称为精确堆上限。

## Validation

- Java17下统一assemble/compileGameTestJava成功，最终r7。ForgeGradle6.0.6工具解析曾在编译前触发repository重放错误；仅在ignored build使用现有工具缓存及临时init脚本，原1.+依赖不变，未修改项目构建/全局Gradle配置或重编历史源码。
- 13项重点首过；源真实熄灭/重燃/拆除/替换、2项并发保存、结构隔离通过；最终4项质量/生命周期复测通过，各自编译归属见JSON。取消重放6400J、2次取消只提交1次，J和温度误差0；运行中任务保存后继续提交1次。
- 隔离：4928次远处方块变化，本地800采样空读/几何失效0、最大年龄20tick，新房间22tick安装。
- 双基地两轮各256源/100消费者/2400tick，4workers、5GiB堆、128MiB/1000ms每秒：各12000查询、全细档、违规0。累计分配2.320342032/2.324045848GB，基线2.410199496GB，少3.73%/3.57%；登记峰76.365/76.372MiB，基线77.121MiB。native周期30.006/25.281十亿，基线25.156十亿，CPU提速未确立。
- 千源64→128MiB：四窗提交21/21/21/19，空读0、最大年龄21tick；三次保存，目标大区4880tick及对照4960tick恢复，细档稳定400tick，J差/顺序错误0。登记峰155.776MiB，上轮182.186MiB；时点不同，不视为稳定压缩率。所有通过运行关闭root0。
- 失败保留：源r1失败、r2诊断复现；首轮压力在9860tick后中断且无关闭回执，未计通过。包内426thermal类，退役批次/测试/native观察0，受保护旧粒子4类保留。无Python、JUnit、子代理或Git发布。

## Remaining

- 结构范围缩小、粗档前端进一步汇总/材料组化、保存源快照局部化仍为独立后续候选。本批未新增0.1°C近似轨迹、100真实联网玩家、10分钟完整生产生命周期或整合包跨进程重启资格。
- 原始日志/JFR/临时工具留在ignored build；提交使用正式JSON和当前文档，不加入生成缓存。
