# 挖掘风道与炉底炭床必经区

- Time: `2026-11-16 20:29:00 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证，6 Astra玩法和只读复核，6 Luna文案与文档`
- Status: `completed`
- Scope: FH风道小游戏、绘图台网络/保存、客户端/服务端验证与知识文档；TWR冶铁炉双语说明和教程；Research Playtest同步。

## Completed

- 按用户示意图将旋转管道改为28×28挖掘地图：棕色泥土可挖，左下蓝色入口，上方预挖炉腔及蓝色出口。Astra建议把星星换成横跨炉底截面的炭床，并用连续不可挖炉壁限制入口；四邻接成功路径必经炭床，无支路目标冒充途经的问题。
- 左键按下/拖动/松开，3格宽菱形笔刷与整段插值；拖动位置按事件增量累计，不受帧间坐标滞后影响。鼠标离开地图断开笔段。客户端即时预览，单个在途请求，每批最多32笔段，服务端回传后重放未确认笔段；Esc/返回/关闭补发未发笔段。
- 泥土、炉壁、炭床、蓝色箭头、笔刷框、图例和动态气流均为GUI绘制，无新贴图或流体模拟。未接通时入口有气流提示；接通后粒子沿真实路径经过炭床并变为暖色，炭床点亮。持续送风、无试吹按钮、评分、耗材或时限。
- 研究ID、run/task绑定和立即通关结算沿用。新DIG_AIRFLOW追加到枚举末尾，保留旧ROTATE_AIRFLOW编号但不执行，RESET恢复原泥土。通关后的盘面不能重置/重复发奖，后续实际风箱和观察流程不变。
- NBT format=2保存dug与completed，路径由地形重建。旧rotations重建新盘面；已有任务完成事实在打开时恢复为完成，无旧路径时显示已完成与亮炭床，不编造挖掘轨迹。
- 维护作者指南、玩家指南、绘图台、阶段/风道、冶铁炉、测试入口文档及两仓中英文说明；未改design/、build.gradle或配方/阶段定义。M4/M7助手适配挖掘；测试助手新增实例内临时文件启用方式，避免修改运行中Prism缓存的JVM参数。

## Validation

- Java17 compileJava/compileGameTestJava/compileM3cValidationJava通过（2m43s），日志 `build/research-excavation-20261116-compile.log`。
- `runGameTestServer -Pm4CharcoalPitTests -PgameTestNamespaces=frostedresearch_m4,frostedresearch_m4_cp` 当轮 `All 13 required tests passed`（5m20s），日志 `build/research-excavation-20261116-tests.log`。含不可挖炉壁、绕行失败、成功路径必经炭床、快速笔段连贯、部分/完成盘面存读档及旧旋转盘面重建；正式绘图台结算/存档、临时配方、阶段推进、真实风箱与观察回归也通过。
- jar/reobfJar/m3cValidationJar通过（首轮1m15s；最终外观和助手修正后2m49s）。最终日志 `build/research-excavation-20261116-client-fix.log`。后一次仅调整GUI显示和测试坐标，服务端规则无变化；未重复整套服务端回归。
- 首轮正式客户端到达盘面但助手未加入窗口居中偏移，点到了画布外，phase7超时；不是通过结果。修正为实际屏幕坐标，并断言预览已挖格增长，再重跑。首次证据保留 `build/m4-client-first-helper-attempt/`。
- 最终正式Playtest新世界 `result.json=passed, phase=9`：实际Screen按下/快速拖动/松开产生真实DIG C2S，两笔完成。第一笔49格在同一tick立即Esc退出后重开仍完整保留，确认关闭补发；服务端成功路径经过炭床，通关立即开放临时原型配方，未自动进入下一研究阶段；Esc与返回按钮保留原详情/阶段任务标签。人工查看03初始与04成功截图，确认土壤纹理、炉壁、炭床亮起、气流、图例和按钮显示正常。
- 结果与图片：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-airflow-excavation-20261116/build/m4-client/`。客户端操作由Screen事件/按钮回调驱动，材料知识为夹具；不宣称物理鼠标手感、完整生存、液体物理或新双客户端通关。M7助手仅适配编译，本轮未重跑双客户端。
- 六份修改过的JSON可解析，两仓diff --check通过；生产JAR排除验证助手/GameTests。Playtest只同步FH JAR与TWR四份语言文件，共5文件逐字节比较；备份清单 `build/prism-backups/2026-11-16-airflow-excavation/`。测试正常保存退出，临时helper和启用文件已移除，JvmArgs原值保留。没有关闭已有其它游戏会话或启动器，没有打开用户已有世界，没有同步Development/runClient。
- 仅提交本轮文件，保留FH `.gitignore`、TWR原暂存 `config/.gitignore` 及无关未跟踪文件；不push。

## Remaining

- 玩家可在Playtest冶铁炉第一阶段体验挖掘手感。当前是一关固定地图和通路动画；完整生存、物理键鼠与新双客户端流程未在本轮验收。
