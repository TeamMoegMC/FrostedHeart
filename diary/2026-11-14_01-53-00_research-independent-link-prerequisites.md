# 联络与事件采用独立前置，正式研究增加辅助知识

- Time: `2026-11-14 01:53:00 +08:00`
- Author: `Codex / OpenAI；实现与验证`
- Status: `completed`（代码、正式数据、定向回归、打包及Playtest同步；新入口未实机重走）
- Scope: LinkRule、联络判定与等待、TWR三项正式研究及双语说明、测试fixture和living docs。

## Completed

- LinkRule新增可选required_knowledge/optional_knowledge，分别配置联络的档案必要知识与减时辅助知识；inputs仍是必须摆到桌面的精确输入。省略新字段分别继承旧acquisition.required/optional，显式[]表示该路径无此类知识。事件概率与获取入口保持原规则；外部授予与学习不受获取门槛约束。
- 联络开始按所选规则计算耗时，等待中重查必要知识；辅助知识只查有效档案，来件不算、无需摆桌面。新增条件同时纳入定义引用检查与观察用途索引，无存档升级。
- 土制炉：粗铜物品观察为联络必要输入，黏土球、沙为辅助，默认10→8秒。冶铁炉：粗铁+煤/木炭/焦炭为必要输入，铜/青铜锭为一个辅助组，10→9秒，燃料不再额外给联络减时。耐火砖：砂砖单砖或砂砖方块的物品观察为必要输入，高岭土、铝土矿、黏土球为辅助，10→7秒。每项辅助默认减20tick，以20TPS换算秒。
- 用户确认两种砂砖皆可。新增物品标签the_winter_rescue:research/sandy_bricks接受charcoal_pit:sandy_brick_item或charcoal_pit:sandy_brick；使用已有object/tag谓词，无需向观察快照写入分类。事件载荷、联络与项目条件均已有标签匹配能力。
- 延续“开启研究须集齐全部前置”的设计，在三项目access.start_all中分别补入粗铜、粗铁、砂砖观察；联络辅助仍是开启项目必需知识。实验、阶段、成果与既有run进度不变。
- 同步正式fixture、双语研究正文/任务书；更新定义、作者、操作、API、三路线及控温炉测试依赖说明。M5正式项目新增Charcoal Pit砂砖前置，-Pm5Tests因此加载真实Charcoal Pit服务端依赖；普通生产构建不引入测试源。

## Validation

- Java17下M2/M3B/M4真实风箱/M5共33项必需GameTest全部通过（5m21s），日志build/research-idea-routes-20261114-retest.log。覆盖独立门槛、旧规则继承、显式空覆盖、有效档案减时、等待时前置撤销、辅助观察用途、真实砂砖两形态与项目门槛、风道/设备既有流程。
- 首轮33项中32通过；一项正式联络测试只安装土制炉规则，未安装耐火砖规则，导致耐火辅助观察缺少用途。补齐测试快照后完整重跑通过。原日志保留于build/research-idea-routes-20261114-validation.log，未将首轮失败当成通过。
- TWR改动JSON解析、引用语言键、正式fixture对齐与两仓diff --check核对。另起不带测试开关的jar/reobfJar成功（1m9s），日志build/research-idea-routes-20261114-final-build.log；正式JAR14,915,490字节，无GameTest类/fixture。
- 确认Research Playtest未运行，同步JAR、3条联络、3项目、砂砖标签及4个双语JSON，共12文件；逐文件核对与构建/源码一致。原文件备份及清单在build/prism-backups/2026-11-14-independent-idea-link-routes/。未复制配置、按键或存档，未动Development/runClient。

## Remaining

- 本轮没有启动客户端或实际点击新联络入口；用尚未获得想法的队伍在Playtest验收“只摆必要记录”“辅助未入档/已入档耗时差”“两种砂砖任选一种”。已有想法继续隐藏，已有研究进度不重置。完整生存采集材料链仍属于此前未验范围。
