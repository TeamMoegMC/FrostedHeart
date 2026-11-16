# 砂砖风道、阴燃提示与冷风漏气优先

- Time: `2026-11-16 22:01:22 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证，6 Astra规则复核，6 Luna文案与文档`
- Status: `completed`
- Scope: FH风道判定、粒子、入口/地图与保存、M4验证；TWR双语说明；Research Playtest。

## Completed

- 取消炉腔两侧不可挖的墙，使用可开凿砂砖。入口周围保留不可挖保护，但与普通砂砖同色，无额外框或图例；入口后方扩大预挖空腔，提示继续开凿。
- 炭床未送风已有轻微阴燃闪烁，成功后增亮。AirflowParticles最多72粒，在开放空腔随机转向并碰壁反弹，接触炭床后暖色/上浮，出界或超寿命从入口补充；粒子仅视觉，不入NBT，重置清空。
- AirflowGame先在排除炭床的冷风图查入口可达的非入口边界，包括绕床接炉顶。存在即失败，优先于正确路线；否则经过炭床连到正常出口才成功。未连入口的边缘空洞不失败，炭床后才可达的热风开口不额外失败。
- 每批32笔段全部落地再统一结算；客户端从权威盘面与全部未确认笔段重建预览，避免本地先成功截断同批后续漏口。失败冻结并标实际漏口/排气，可免费重置；炉顶旁路与普通外壁使用不同提示。
- airflow_game.format=3保存dug/completed/failed，读取format2保留已挖格并重算，保留旧已完成事实；rotations重建初始地图。研究ID、阶段/临时配方、实际风箱试吹及KnowledgeState版本不变。
- 两仓中英文、五份知识文档及GameTest入口同步维护；未改design/、build.gradle、配方或项目阶段JSON。

## Validation

- 初轮测试因用例把单个Stroke误传List参数而未编译运行；修正测试参数和保护区用例笔刷端点后重跑。初轮日志 `build/research-air-leak-20261116-tests.log`。
- Java17 runGameTestServer，-Pm4CharcoalPitTests，命名空间frostedresearch_m4/frostedresearch_m4_cp，明确 `All 15 required tests passed`（4m4s）。日志 `build/research-air-leak-20261116-tests-final.log`。含两种冷漏、整批失败优先、热风开口、保护入口/空腔、失败/部分/完成存读档、format2承接、粒子双向碰撞/升温及真实风箱/阶段回归。
- jar/reobfJar/m3cValidationJar通过（47s），日志 `build/research-air-leak-20261116-build.log`；生产JAR排除助手和GameTests。
- 正式Playtest新夹具世界result.json=passed/phase9：炭床前冷粒子活动；实际Screen事件和C2S先绕床接炉顶失败、再穿右外壁失败，两次不授予原型且能重置；之后两笔正常通关，43格新笔画同tick立即Esc退出再开仍保留，临时原型配方与返回原任务页正确。
- 人工查看03初始、03b炉顶旁路、03c外壁泄漏、04成功截图，确认砂砖、预留入口空腔、保护未单独标记、炭床和空气/漏口显示。03a为间隔10tick的阴燃/粒子画面。证据：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-airflow-leak-20261116/build/m4-client/`。
- Playtest同步5文件（FH JAR、TWR四份语言JSON）逐字节核对，备份 `build/prism-backups/2026-11-16-airflow-leak/`；测试正常保存退出，临时助手/启用文件移除，JvmArgs原值保留。未动网络、启动器、用户已有世界或Development/runClient。
- 仅提交本轮文件；保留FH .gitignore、TWR原暂存config/.gitignore和其它无关文件，不push。

## Remaining

- 当前为单客户端事件/夹具验收和简化气体示意；物理鼠标手感、完整生存与新版双客户端流程未重跑。
