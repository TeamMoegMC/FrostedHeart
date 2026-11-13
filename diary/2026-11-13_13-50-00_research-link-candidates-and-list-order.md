# 联络隐藏已得想法与研究列表状态排序

- Time: `2026-11-13 13:50:00 +08:00`
- Author: `Codex / OpenAI；实现与部署`
- Status: `completed`（实现、回归、构建及 Playtest 同步完成；实际客户端画面待验）
- Scope: 联络候选查询/回复、KnowledgeLayer 列表与同步、M2 既有验证。

## Completed

- `previewLinks` 与 `candidateLinks` 过滤档案和来件已有的输出想法，不要求先学习才隐藏。直接指定规则结算和已保存推导历史仍保持兼容，不重复发放已有想法。
- 候选回复增加 output ID，客户端在候选回复及知识快照到达时清理已取得的想法并修正候选下标，覆盖队友或其它入口获得想法后仍停留于候选页的情况。
- 列表按进行中、实际可开启、缺条件/不可用、已完成排序，同组使用稳定排序保留原有顺序。ALREADY_OWNED/LEGACY_COMPLETED 优先判为完成；备用记录不使完成项目置顶。排序只在列表重建时执行，不放进逐帧绘制路径。
- 更新操作、API、示例回环说明；既有 M2 用例改为验证档案/来件输出不再出现，同时保留底层结算不重复获得的回归。

## Validation

- `git diff --check` 通过。Java17 下 M2 GameTest 的 7 项必需测试全部通过；生产 jar/reobfJar 完成（总计6m40s），日志 `build/research-list-link-20261113-validation.log`。
- 正式 JAR 为14,912,370字节。确认 Playtest 未运行后同步，安装包与产物哈希一致，配置/按键及存档目录名称和修改时间核对不变。旧包和部署清单位于 `build/prism-backups/2026-11-13-research-candidates-order/`。未启动客户端，未更改 TWR 或 Development/runClient。

## Remaining

- 本次列表排序和实时候选变化的实际客户端视觉仍待验收。
