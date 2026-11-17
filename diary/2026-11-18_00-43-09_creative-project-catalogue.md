# 创造模式查看全部数据包研究

- Time: `2026-11-18 00:43:09 +0800`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: 旧研究总览、知识快照、研究详情预览；仅FH实现，TWR数据未改。

## Completed

- 旧总览顶部增加创造模式书本页签「新研究（创造模式）」，列出全部当前数据包项目，可搜索本地化标题或ID；包含未发现、进行中与已完成项目。
- 点击查看详情复用原有研究工作区，展示说明、完整前置条件与各阶段基础任务；关闭或Esc返回原总览，保留选择与搜索。
- 预览不提供研究操作，不学习知识、不创建研究记录、不授予配方。正常札记及旧研究图谱仍按原规则展示；退出创造模式隐藏入口并关闭预览。
- `KnowledgeView.create(service, true)`额外生成`creative_projects`；`TeamResearchService`按接收者游戏模式复用每种快照，同队生存玩家收到普通视图。进入新页签时重新请求`REFRESH`，目录不持久化。
- 更新[研究UI](../docs/research/research-ui.md)、[绘图台](../docs/knowledge/workbench.md)和[同步契约](../docs/knowledge/state-and-api.md)，补充中英文标签。

## Decisions

- 使用独立目录页签及只读详情，不将新项目转换成旧`Research`对象，不改变解锁或任务判定。
- 各阶段以基础任务预览，不预测进入阶段时才冻结的分支。

## Validation

- Java17离线`runGameTestServer jar reobfJar`通过；命名空间`frostedresearch_m1,frostedresearch_m4`，19项required GameTests全部通过，含新增的未发现目录/无状态变更/未来阶段预览用例。
- 日志：`build/creative-project-catalogue-final-20261118.log`，测试工作目录`build/creative-project-catalogue-20261118/`。
- 中英文JSON、重复键、新标签与`git diff --check`通过。`build.gradle`和`gradle.properties`未改。
- 首轮配置停留在ForgeGradle元数据SSL请求；本次命令设置临时`JAVA_TOOL_OPTIONS`中的连接/读取超时10000ms，未修改持久配置。编译发现重复`tick`后合并到既有方法，最终构建成功。
- 正式JAR同步到`The-Winter-Rescue-Research-Playtest-20261112`；旧包备份`build/prism-backups/2026-11-18_00-42-10-creative-project-catalogue/`。已核对文件逐字节一致、仅有一个FH JAR、新类及语言资源齐全。未改实例存档或配置。
- `client_launched: false`；自动化与部署不作为实机视觉验收。

## Remaining

- 游戏内验收：创造模式打开旧研究总览→顶部书本图标→搜索/选择未解锁项目→查看三个详情分页→Esc返回；切到生存模式确认入口隐藏，普通札记不新增发现。
