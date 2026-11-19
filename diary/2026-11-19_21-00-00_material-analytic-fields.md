# 材料温度解析场合成

- Author: OpenAI Codex
- Date: 2026-11-19 UTC
- 材料数值入口在原始记录上按方块中心合成解析场；无记录仍返回 NaN。
- 相变环境估算不重复合成；原始 MaterialSample 和红外批量合成保持独立，不修改焓。
- 文档：更新 world-climate-and-temperature.md。
- 验证：`./gradlew compileJava --offline` 成功（29 条既有编译警告），`git diff --check` 通过；未运行游戏内测试。
