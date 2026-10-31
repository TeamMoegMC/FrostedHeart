# 温度提交前测试与开发记录清理

- Time: `2026-10-29 23:48:00 +08:00`
- Author: `Codex; OpenAI GPT-6.1 Sol; repository cleanup and verification`
- Status: `completed`
- Scope: `热系统GameTest退役工具、专用运行配置、plans/diary归并及引用维护；生产Java、城镇和其他模块测试保持`

## Completed

- 热系统测试/观察Java从115文件、17103行降至85文件、14475行，净减2628行。删除30个文件，包括一次性客户端延迟/旧世界录制、隐藏窗口截图/光栅校验、NUMERICAL_COST/GROWTH探针、重复入口和旧16/32/128源容量变体；同步清理3个保留辅助类中的专用调用。
- 删除2份专用Mixin配置及其2份旧编译资源输出，移除onlineLatencyClient、savedWorldClient和对应Gradle目录准备任务。共享GameTest/research运行入口继续存在。
- 92份计划、447份日记原文归档移出Git工作树，归并为[当前待办](../plans/2026-10-29_23-38-00_thermal-next-steps.md)和[开发历史](2026-10-29_23-38-00_thermal-development-history.md)。保留2份跨模块引用计划、3份跨模块引用日记及最新2份实现/资格记录；当前热系统相关计划3份、日记7份。
- 更新23个文件中的108处链接。讨论内容仅修正目标路径，保留原贡献者文字；其他系统的记录和源码没有因归并而改写。
- 原文归档位于本机仓库外：`D:/Frosted-Heart-cleanup-archive/thermal-before-cleanup-2026-10-29_23-36-28.zip`，含原相对路径和清单。完整清单、引用检查和构建日志保存在ignored build，不新增大份仓库清单。

## Decisions

- 保留当前输入所有权/取消重放、查询读者、源生命周期、保存/重启、质量切换、相变/红外/热库玩法，以及双基地、分散、多维度和千源压力夹具；后续百人服测仍可使用。
- 不按test/debug名称删除正式游戏资源：已注册的debug_heater、现有热网和其他系统数据继续保留。共享worldgen/research/Town测试资源不属于本轮退役范围。
- 历史in-progress不被改写为全部完成，旧CPU计量失真、失败和资格边界保留在摘要与既有validation JSON中。
- 本轮不提交或推送Git；工作区原有的其他系统改动由其各自任务处理。

## Validation

- Java17统一assemble/compileGameTestJava通过，34秒；没有重跑服务器矩阵或JUnit。
- 退役类在剩余Java/Gradle中引用0；剩余Mixin目标缺失0；指向本轮删除文件的Markdown链接0；旧编译类/资源残留0。
- 清理前后生产Java文件长度/修改时间核对无变化。正式包thermal类426、GameTest类0、受保护旧粒子4类，与清理前一致。
- Documentation impact: plans/diary README及GameTest入口说明指向归并结果，热系统文档的旧计划/日记链接已维护；当前运行行为与上一批验收结果保持各自资格。

## Remaining

用户审查当前Git变更后提交。百人完整负载与当前整合包世界重启仍是[后续待办](../plans/2026-10-29_23-38-00_thermal-next-steps.md)，本次清理不新增运行资格。

## 2026-10-30提交范围复查更正

复查发现`2026-08-12_10-39-35_resident-food-temperature-stress.md`因文件名包含temperature被误归入清理范围；其实际内容属于城镇居民饮食/温度压力模型。已从仓库外归档按原字节恢复，恢复后该文件对HEAD没有差异。热系统实际归并日记数更正为446份，原始相关日记计数剔除这一城镇条目后为451份，整理后热系统记录仍为7份。原清理过程及上文当时统计按历史保留。

本次再次检查退役类/Mixin/删除链接/生成产物均无残留，热模拟范围diff检查通过；未重新编译或运行服务器测试。暂存区仍为空，工作区含研究界面等其他模块改动，尤其build.gradle存在共享文件片段；提交时需按本次实际范围选取。
