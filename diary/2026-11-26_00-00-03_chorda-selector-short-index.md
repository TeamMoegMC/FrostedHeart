# Chorda selector 序号缩写

- 作者：Codex
- 日期：2026-11-26
- 修改：UIElement 的序号解析与 getUniqueSelector 输出统一使用 [n]，从 1 开始，例如 * > *[2] > *[5]；替换 :nth-child(n) 语法以缩短 debug 显示。
- 文档：同步 docs/chorda-cui.md。原开发记录保留历史。
- 验证：离线 compileJava 成功；源码方法提取的临时 Java 检查覆盖紧凑路径回查、复合选择器、从 1 开始的索引、非法及超大索引，全部通过，临时文件已清理；git diff --check 通过。未重启客户端重新截图。
