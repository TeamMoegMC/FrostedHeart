# 新旧研究共用项目详情显示组件

- Time: `2026-11-13 13:41:39 +08:00`
- Author: `Codex / OpenAI；实现`
- Status: `completed`（实现、构建及 Playtest 同步完成；真实画面待验）
- Scope: `ResearchProjectFrame`、`ResearchProjectWorkspace`、`KnowledgeLayer` 和双语按钮。

## Completed

- 从当前实际挂载的旧研究 `ResearchProjectWorkspace` 提取底板和分页绘制，新旧详情共用 `ResearchProjectFrame`，复用 `TechIcons.DIALOG`、35像素标题栏、20像素页签及旧档案配色。
- 新项目详情替换札记顶栏，显示项目标题、状态、关闭按钮及研究说明/前置知识/阶段任务三个页签；说明与成果、开启条件、当前阶段方法和任务各有独立阅读区域。
- 保持新项目较大的窗口自适应尺寸、滚动正文和底部操作；开始成功转入任务页。分页保留实际 run 选择，关闭返回项目列表；风道与研究操作仍用原知识快照/包。
- 旧项目仍使用原积分、线索、阶段之外的旧控制逻辑，旧页保留原尺寸和三个页签含义；未修改数据包及服务端研究判定。
- 同步更新知识操作、玩家指南和旧研究 UI 文档；本次共用组件尚未游戏内视觉验收。

## Validation

- `git diff --check`、双语 JSON/按钮键核对通过；Java17 的 compileJava/jar/reobfJar 通过（4m27s），日志 `build/research-shared-detail-20261113-build.log`。
- 正式 JAR 14,911,607 字节，ZIP 中包含共享显示类和三页双语文本。
- 用户确认正常保存退出后，再次确认实例进程已退出，将正式包同步到 Research Playtest；安装包与产物哈希一致，配置、按键及存档目录名称/修改时间核对不变。旧包及清单位于 `build/prism-backups/2026-11-13-shared-research-details/`。没有启动客户端，未触碰 Development、runClient 或 TWR 源码。

## Remaining

- 游戏内确认说明、知识缺项、当前任务、关闭/Esc、风道、旧详情页签和中英文裁切；本轮未以编译替代视觉证据。
