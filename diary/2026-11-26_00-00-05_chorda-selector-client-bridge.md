# CUI 选择器客户端控制桥

- 作者：Codex
- 日期：2026-11-26
- 实现：AgentClientBridge 新增 cui-elements（所有下级 debug 信息扁平数组，含隐藏及禁用）、cui-query（selector 首个匹配，未命中 JSON null）、cui-set（JSON selector/field/value，按最近字段声明沿继承链反射赋值，支持 private）。返回字段声明类、修改前后值及新 debug 信息；实例字段赋值不调用 setter、不刷新 UI。JSON 输出保留 null。
- debug 信息：selector、name、type、classes、parentSelector、title、局部 rect、UIElement 屏幕坐标 screenRect、enabled、visible。
- 文档：同步 Scripts/agent_client.py 使用说明和 docs/chorda-cui.md。
- 编译：./gradlew compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle --console=plain 通过；git diff --check 通过。
- 真实客户端验证：独立存档 cui-selector-debug-20261126 的研究界面枚举 122 个下级元素（3 个隐藏），全部 122 条 selector 回查 debug 信息一致；反射修改 UIElement 继承私有 name、按新 #name 回查并恢复 null；enabled 布尔修改回查并恢复；查询未命中返回显式 null；字段不存在、primitive null 赋值及非 CUI 界面三条命令错误均符合预期。
- 验证调整：Escape 返回绘图桌仍是 CUI，最初非 CUI 断言不成立；改为 disconnect 后检查，三条命令均正确返回非 CUI 错误。此为测试前置状态纠正，不是实现失败。
- 证据：build/cui-selector-bridge-validation.json、build/cui-selector-bridge-validation-summary.json、build/cui-selector-bridge-client.log（生成物不提交）。测试修改字段已恢复，客户端已正常退出。
