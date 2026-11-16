# 研究内容模板与三配比试烧观察

- Time: `2026-11-16 13:46:10 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证，6 Luna 协助模板与只读复核`
- Status: `completed`
- Scope: FH研究作者文档、烧后样品配比/观察字段、旧进度承接与测试；TWR土制炉项目及三个项目的中英文操作说明/教程。

## Completed

- 新增docs/knowledge/content-template.md，作者指南连接五环节知识角色、目标/方法/判据/结论模板、失败补做及获取路线，并列明可纯数据配置、需Java接点和需新增机制的能力。开发栏目不强制暴露给玩家；普通前置仍按学习入档，实验观察可直接完成任务，不额外自动入档。只有确实消耗实物才使用sample。
- 正式土制炉改为三种配比烧后物品观察，两坏一好都要检查。TrialKilnSamples保存TrialKilnVariant并注册item.trial_kiln_variant字段；TrialKilnSampleItem显示配比。小炉按clay_rich/sand_rich/balanced选择对应experiment，绑定当前队伍run/task。重复同一种坏样不代替另一种，无需提交、消耗或入档。未烧、未绑定和旧run样品不替代。
- 原sample任务及独立示例继续可用。均衡观察保留内部任务ID submit_intact，旧单任务main阶段补齐比较任务并重新计算ready；已提交完好样保留信用，schema4缺stage_state的回执也承接，completed记录不重置。不推测缺失配比的旧坏样，允许重新试烧。
- 三项目联络提示区分联络必需、联络辅助和开始条件；实验说明改为自然语言。知识页标签改为“开始条件”，通用观察提示不再把物品任务说成必须现场方块观察。更新正式夹具、双语任务书、玩家指南、字段及设备说明，旧小游戏称谓改为连连看。

## Validation

- Java17 compileJava/compileGameTestJava通过（2m15s）；日志build/research-conventions-20261116-compile.log。
- 使用-Pm4CharcoalPitTests -Pm5Tests运行M3/M3B/M4/M4CP/M5/M6/M6devices：当轮All 45 required tests passed（4m19s）。日志build/research-conventions-20261116-tests.log。含三配比独立观察、错run、重复采样不叠加、不消耗/入档、BE与进度存读档、原sample提交等。
- 补充schema4回执承接后，frostedresearch_m3的6项必需测试再次通过（3m25s），日志build/research-conventions-20261116-final-kiln.log。包含旧已完成记录保持不变的显式断言。
- jar/reobfJar/m3cValidationJar构建通过（46s），日志build/research-conventions-20261116-build.log。后续验证助手单独构建通过；正式JAR14,924,561字节，无GameTest或ClientValidation类。
- 正式Playtest客户端在新建世界通过：材料/起始知识使用夹具；三座小炉实际试烧，收取后通过计时手持观察C2S完成三项；全部样品保留；任务不需保留/入档；完成按钮后验证真实配方、成型权限及IE事件。旧入口承接导航也通过。不是物理键鼠或生存获取测试。
- 结果和截图：C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-conventions-20261116/build/m3c-formal/；result.json为passed，stage20，three_samples_retained、observed_without_archiving、recipe_after、forming_after、ie_form_event_after均true。人工查看02和09，确认自然正文、开始条件标签和三项任务完成显示。
- 两次初始Java启动停在JVM参数错误（引号和Prism缓存），没有进入游戏；修正并重启启动器后正常启动。首轮游戏检查因验证助手误以为指定快捷栏为空而停止：整合包初始物品占用该格，改为实际寻找空格后重跑通过。首轮失败结果保留在同目录的m3c-formal-helper-attempt。未把这些失败记作通过，正式玩法实现未因助手问题改动。
- 客户端正常退出。清理临时助手并恢复原JvmArgs；为防Prism缓存回写测试参数，在游戏退出后关闭本轮启动器进程。6个正式文件（JAR、土制炉项目、4个语言文件）已同步并逐字节核对；备份及manifest位于build/prism-backups/2026-11-16-research-conventions/。
- 两仓JSON解析、正式夹具一致、双语引用和git diff --check通过；未改build.gradle。保留FH未暂存.gitignore、TWR原已暂存config/.gitignore和无关未跟踪内容。仅提交本轮文件，不push。

## Remaining

- 完整生存材料链和物理长按键鼠未在本轮重走；此前已完成研究及正式成果不要求重做。Playtest已可用于人工体验三配比观察。
