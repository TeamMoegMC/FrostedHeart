# 温度传感器反转按钮文案

- Time: `2026-11-16 15:31:07 UTC`
- Author: `OpenAI Codex`
- Status: 界面文案与构建验证完成。

将比较式按钮改成中英文反转开/关文字，悬停解释当前模式的温度边界、15级信号/无信号效果，以及点击切换动作。只在模式变化时更新 Tooltip，避免每 tick 重建提示对象；不改功能或同步。

改动 TemperatureSensorScreen.java、中英语言文件、docs/climate/world-climate-and-temperature.md 和本日记。

验证命令：`./gradlew --offline assemble`；JSON 解析已通过。

`./gradlew --offline assemble`：通过（BUILD SUCCESSFUL in 47s）；`git diff --check`：通过。本轮未重新运行客户端。
