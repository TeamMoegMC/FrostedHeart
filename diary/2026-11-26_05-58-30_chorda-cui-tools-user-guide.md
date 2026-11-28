# CUI 选择器工具详细使用手册

- 作者：Codex
- 创建时间：2026-11-26 05:58:30 UTC
- 状态：已完成。
- 需求：提供可直接使用、可随演示视频转发的详细明确文档。
- 修改：新增 docs/chorda-cui-tools.md，独立承载启动条件、命令引用、selector/class/路径规则、debug 返回字段、六条命令完整参数及示例、反射类型与字段语义、自动及精确构造器签名、动态增删生命周期、完整 Python 操作、错误排查和视频章节。docs/README.md、docs/chorda-cui.md 增加入口。主工作区 ai_files/chorda-ui-tools-guide.md 为转发副本，调整链接后与权威正文一致；不改程序代码。
- 内容核对：按 UIElement、UILayer、ClientUtils、CUIDebugHelper、AgentClientBridge、agent_client.py、dev_client_bridge.init.gradle 及真实演示记录核对。明确仅 cui-elements 单层列举，selector 可任意深度；screenRect 与 Screen GUI 偏移区别；parameterTypes 含父参数而 args 不含；Gson 对复杂返回值非可逆；反射不调用 setter/布局；超时不撤销排队命令。记录零尺寸 TextBox 在 DrawDeskTheme 已实测触发除零、尚未修复，示例使用默认有效尺寸的 ProgressBar。
- 验证：17 个 Bash 代码块通过 bash -n；1 个 JSON 代码块可解析；2 个 Python 示例通过 ast.parse；9 条带 JSON 的 shell 命令参数及显式构造器参数数量检查通过；手册/转发副本和入口涉及的 5 条相对链接有效；转发副本正文归一化链接后与权威文档一致；git diff --check 通过。
- 边界：本轮仅文档编写和静态示例检查，未重启客户端、未重新编译，引用此前真实演示证据。diary/README.md 不存在，沿用现有开发记录格式。
