# CUI 空白固定尺寸测试界面

- 作者：Codex
- 创建时间：2026-11-26 06:50:55 UTC
- 状态：已完成。
- 实现：AgentClientBridge 添加 cui-open <width> <height>，要求两个正整数 GUI 尺寸，在切换前校验；使用 CUIScreenWrapper 打开开发桥内部的 EmptyCuiLayer。根没有默认子元素、布局或渐变，不自动随内容缩小，返回根 element debug 信息及 screen 描述，不改变窗口大小。
- 渲染：初次截图发现无世界且不画背景时保留了主菜单旧帧，补充 EmptyCuiLayer.drawBackground 在 mc.level==null 时用纯黑清除整个 GUI 帧；有世界时保持透明。修正后截图确认完全空白，新加 ProgressBar 仍正常显示。
- 文档：同步 Scripts/agent_client.py、docs/chorda-cui.md、docs/chorda-cui-tools.md、docs/README.md 及主工作区 ai_files/chorda-ui-tools-guide.md。手册增加第七条命令参数、单位、返回格式和独立 add 测试示例，标明旧视频未包含此命令。
- 验证：两次 compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle 均通过。真实客户端检查 320×180/200×100 固定尺寸、零默认子元素/控件、渐变关闭、添加后尺寸不变、非法输入不切换、重新打开空根、Esc 返回前一屏幕。渲染修正后重新启动实际客户端，复验空白截图、新增子元素可见、移除和重新打开，全部通过；测试客户端正常退出。
- 证据：build/cui-open-validation.json、build/cui-open-validation-summary.json、build/cui-open-render-validation.json、build/cui-open-render-validation-summary.json 及 run/agent/screenshots 的两张修正后截图，均为本地生成物，不提交。
- 文档检查：20 个 Bash 代码块、2 个 Python 示例、1 个 JSON 示例语法和转发副本一致性通过；git diff --check 通过。本次没有提交或推送。
