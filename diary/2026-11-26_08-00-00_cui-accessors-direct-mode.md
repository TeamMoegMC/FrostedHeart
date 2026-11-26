# CUI 属性访问器与 direct 模式

- 作者：Codex
- 日期：2026-11-26 UTC
- 实现：cui-get/cui-set 默认优先公开 getter/setter，无对应方法时回退字段；统一 direct:true 显式绕过目标属性访问器。移除 TextField 特判，使用通用 setText 重载匹配。
- 录制：两个 build 录制脚本改用 SimpleLayer 纯色背景及 TextField 静态标签；构建目录脚本未纳入 Git。
- 文档：同步工具指南、CUI 指南和命令帮助。
- 验证：compileGameTestJava --offline 已通过；未运行客户端交互验证。
