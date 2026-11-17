# 温度传感器目标标签与悬停换行

- Time: `2026-11-16 15:42:16 UTC`
- Author: `OpenAI Codex`
- Status: 实现、构建及中文客户端截图验收完成。

仅改 TemperatureSensorScreen.java 的目标标签/输入框排版，以及中英文语言文件：新增 target_label，反转开/关悬停提示的点击动作前增加显式换行。功能及同步逻辑不变。同步更新 docs/climate/world-climate-and-temperature.md。

验证：`./gradlew --offline assemble` 通过（46s），JSON 解析及 git diff --check 通过。`./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle` 启动中文客户端，在原创造测试世界右键传感器，悬停反转按钮并截取 1280×720 原始游戏帧；已查看，目标温度四字正常、点击开启红石反转另起一行。通过 quit 正常退出，runClient 成功（2m35s）。

截图：`run/agent/screenshots/01794843699030940000-temperature-sensor-target-label-tooltip-newline-zh.png`。
