# CUI 控制桥反射读取字段

- 作者：Codex
- 日期：2026-11-26
- 实现：新增 cui-get JSON 命令，参数 selector、field；使用与 cui-set 相同的继承链查找逻辑读取实例字段，支持 private，返回 field、declaringClass、fieldType、value 和 element。null 保留为 JSON null，标量保留类型，其他对象采用字符串描述。
- 文档：同步 Scripts/agent_client.py 与 docs/chorda-cui.md。
- 验证：compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle 成功；临时 Java 程序提取实际反射辅助方法，检查继承私有字段、null/字符串/数字/布尔读取、同名字段遮蔽、字段不存在，全部通过，临时文件已清理；git diff --check 通过。未启动客户端验证本条新命令。
- 待确认：用户“这个查询是不递归的”存在范围歧义，已询问是要求直接子节点查询，还是要求修复为递归查询。目前源码 cui-query 与 cui-elements 已递归遍历，elements 结果为扁平数组；本次先不改变遍历语义。
