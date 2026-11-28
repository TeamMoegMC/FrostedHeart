# CUI 全量查询与指定目标单层列举

- 作者：Codex
- 创建时间：2026-11-26 06:14:39 UTC
- 状态：已完成。
- 修改：UIElement 新增 querySelectorAll(String)，按当前子树深度优先树顺序返回全部匹配。逐节点逆向匹配祖先路径，以查询根限定范围，同一节点不会因多条祖先匹配路径而重复加入。原 querySelector 首个匹配语义保持不变。
- 命令：cui-elements 支持可选 selector；不传仍列当前根的一层，传入时选择首个匹配目标并仅列其直接子元素。叶子返回 []，目标未命中返回错误。cui-query 改为 elements 数组，全部匹配均返回，未匹配返回 []。get/set/add/remove 仍只选择首个匹配。
- 文档：同步 Scripts/agent_client.py、docs/chorda-cui.md、docs/chorda-cui-tools.md 及主工作区 ai_files/chorda-ui-tools-guide.md；更新数组示例、Python 清理断言和可选枚举目标说明；标明旧视频中的 element/null 为旧格式，不修改录制产物。
- 验证：./gradlew compileAgentBridgeJava --offline -I Scripts/dev_client_bridge.init.gradle --console=plain 成功（已有 Mixin/弃用警告）。临时 Java 程序直接提取实际查询方法、collectCuiElements 和两个命令 case，覆盖树顺序、多祖先路径去重、子树范围、接口/直接子层/后代/索引/完整路径、空及非法 selector、首个匹配兼容性、默认和指定枚举目标、叶子、未命中错误及 elements 数组返回，通过后清理临时文件。首次临时程序因局部变量遮蔽及 switch 混合语法不能编译，修正验证程序后通过，生产编译无该问题。
- 文档检查：18 个 Bash 代码块、1 个 JSON 返回、2 个 Python 示例语法、9 条 JSON 命令参数、4 条相对链接与转发副本一致性通过；git diff --check 通过。
- 限制：本轮未启动真实客户端或重新录屏。没有提交或推送本次改动。
