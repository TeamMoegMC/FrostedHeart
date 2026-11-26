# 树苗放置预测同步

- 作者：Codex
- 日期：2026-11-26
- 原因：原有转换仅在服务端放置事件执行，客户端先预测原版树苗，产生闪现。
- 修改：BlockItem.getPlacementState 返回时在两端转换树苗状态，保留空结果和含水属性；放置事件继续兼容其他途径。同步 docs/climate/cultivated-oak.md。
- 验证：离线 compileJava 与 git diff --check 通过；未进行客户端视觉实测。
