# Chorda UI 元素选择器

- 作者：Codex
- 日期：2026-11-26
- 实现：UIElement 添加 name、getSelectorClasses、querySelector 和 getUniqueSelector；UILayer 提供有序子节点委派；ClientUtils.querySelector 查询当前 CUI Screen；debug tooltip 显示唯一路径。
- 规则：类与直接接口按继承层级排列，匿名类略过名称，接口不递归。支持 #name、.Class、*、复合匹配、后代与直接子节点路径、:nth-child(n)。未匹配返回 null，叶子匹配完成返回自身。
- 唯一路径：以根及兄弟索引定位，同名、匿名元素可区分；树重建或顺序变化需重新获取。
- 文档：更新 docs/chorda-cui.md 的选择器行为与助手入口。diary/README.md 不存在，参考现有日记格式。
- 验证：./gradlew compileJava --offline --console=plain 成功（已有弃用/Mixin 警告）；从实际源码提取选择器方法，在工作目录临时 Java 验证程序中覆盖继承顺序、匿名类、接口非递归、复合/后代/直接子节点查询、唯一路径回查、空/非法/未命中 selector 和叶子行为，全部通过，临时文件已清理；git diff --check 通过。
- 限制：未启动 Minecraft 客户端，debug tooltip 和当前打开 UI 助手尚未进行游戏内实测。
