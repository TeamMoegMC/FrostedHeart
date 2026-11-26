# CUI 单层枚举、任意深度选择器

- 作者：Codex
- 日期：2026-11-26
- 澄清：仅列举当前层；selector 可选择任意深度元素。取代上一条记录中把选择器也限制单层的解释。
- 实现：UIElement.querySelector 恢复子树深度优先查询（含自身）；空格选择任意后代，> 限制直接子节点。cui-query、cui-get、cui-set 自动使用同一规则。AgentClientBridge 的 cui-elements 保持单层枚举，不递归。
- 文档：同步 docs/chorda-cui.md 和 Scripts/agent_client.py。
- 验证：compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle 成功；临时 Java 程序提取实际 selector 和 collectCuiElements 方法，验证枚举只含第一层、单片段及后代 selector 选择深层元素、> 不越层、全部完整 debug selector 回查和未命中返回 null，全部通过；临时文件已清理；git diff --check 通过。
- 限制：本轮未启动客户端实测。
