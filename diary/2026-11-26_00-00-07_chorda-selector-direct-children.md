# CUI 只查直接子元素一层

- 作者：Codex
- 日期：2026-11-26
- 用户确认：查询只检查直接子元素一层。
- 实现：UIElement.querySelector 不再隐式递归；单片段只匹配直接子元素，不匹配自身。显式空格或 > 路径每段只推进一层，不跳过中间层；多片段允许根 selector 前缀，保持 debug 完整路径回查。控制桥 cui-query、cui-get、cui-set 均沿用此规则。cui-elements 仅枚举当前根的一层直接子元素，包含隐藏/禁用节点。
- 文档：同步 docs/chorda-cui.md、Scripts/agent_client.py。反射读取命令 cui-get 保留。
- 验证：compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle 成功；临时 Java 程序提取实际选择器代码，验证直接子元素匹配、孙级不命中、不可跳层、显式多层路径、叶子查询和 debug 完整路径回查，全通过，临时文件已清理；git diff --check 通过。
- 限制：本轮没有启动客户端实测；先前 122 节点递归枚举记录是旧语义，不代表本轮单层行为。
