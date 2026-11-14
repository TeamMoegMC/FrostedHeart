# 矿物形态观察互换，风道回到绘图台桌面

- Time: `2026-11-14 13:57:10 +08:00`
- Author: `Codex / OpenAI；实现与验证`
- Status: `completed`（数据、实现、服务端回归、客户端定向流程、打包与Playtest同步）
- Scope: TWR耐火砖知识条件/材料标签/双语说明；FH桌面风道、客户端回复分发、测试fixture与living docs。

## Completed

- 高岭土与铝土矿各自使用research/kaolin_materials、bauxite_materials物品标签，接受原料、碎矿、粉末、矿块、冻土块的物品观察。聚合各矿物的forge:raw_materials、crushed_raw_materials、dusts、forge:<矿物>标签和自有permafrost物品。
- 想法事件载荷、行为概率辅助、联络减时辅助及项目开启条件使用同组；同类多形态只计一项条件，不叠加减时。已有原料观察继续有效；砖坯实际物料配方不变。没有改变观察快照格式或增加知识身份。
- 按用户确认，把冶铁炉风道从札记详情移到旧连连看所在的桌面区域。AirflowDeskPanel使用原MainGamePanel区域165,25,218×164；总桌面387×203。旧连连看与旧项目进度控件在风道期间隐藏，纸/墨/物件槽停用，玩家背包保留。
- 详情任务按钮进入桌面，Esc或返回按钮恢复同项目、所选记录和阶段任务标签；隐藏札记继续随服务端知识快照刷新。风道回复交当前桌面面板，仍通过原START/ROTATE/RESET_AIRFLOW包结算，不改服务端规则、盘面保存、原型解锁与旧连连看记录。
- 更新正式fixture、材料与风道双语教程，以及refractory-brick、iron-bloomery、player-guide、workbench、stages-and-airflow和GameTest入口文档。M4/M7客户端助手改读桌面面板；新增M4DeskOnly定向模式，本轮未重跑完整M7双客户端流程。

## Validation

- M2、M4真实风箱、M5共28项必需服务端GameTest全部通过（8m5s），日志build/research-mineral-desk-20261114-validation.log。新增25种矿物形态配对，验证项目门槛、减时及重复同类不叠加；10种形态各自可通过物品获得事件产生想法。
- 正式JSON解析、fixture对齐及两仓diff --check通过；普通jar/reobfJar与隔离m3cValidationJar均成功。最终日志build/research-mineral-desk-20261114-final-build.log（3m19s），生产JAR14,919,015字节，含AirflowDeskPanel，不含验证助手或GameTest类。
- 首轮客户端逻辑与导航通过，但人工查看实际截图发现管道/按钮未绘制：AirflowDeskPanel.render遗漏super.render。补齐后重新构建并重跑客户端，未以首轮status=passed宣称画面通过。首轮图片保留在visualizations任务目录的research-minerals-desk-20261114/build/m4-client。
- 修正后的正式Playtest离线客户端在新建M4验证世界通过：实际任务按钮进入旧桌面、九格旋转经真实C2S通关（5次操作）、服务端记录成功并临时开放原型配方、Esc与返回按钮回同一详情、阶段任务标签保留、重开盘面相同。截图人工确认未连通和成功变色画面均有管道与按钮；这是客户端按钮绑定回调测试，不是物理鼠标游玩或生存材料通关。
- 最终结果/图片：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-minerals-desk-20261114-fixed/build/m4-client/`，result.json为passed，03/04/05分别为桌面、成功和返回任务页。
- 客户端正常保存退出；临时助手已移除，JvmArgs恢复原值，保留启动器自身游玩时间记录。11个正式文件（JAR、3个知识定义、3个物品标签、4个双语JSON）与当前构建/源码逐文件核对一致。备份与清单在build/prism-backups/2026-11-14-mineral-observations-desk-airflow/。未操作用户已有世界，未更新Development/runClient。

## Remaining

- 本轮只实机验证冶铁炉风道阶段；原型试吹与完整生存材料链继续沿用既有验证范围。矿物替代已由真实注册表服务端检查，未在客户端逐一长按观察所有形态。
- 保留FH用户的.gitignore改动、TWR原暂存config/.gitignore及其它无关未跟踪内容；仅提交本轮文件。
