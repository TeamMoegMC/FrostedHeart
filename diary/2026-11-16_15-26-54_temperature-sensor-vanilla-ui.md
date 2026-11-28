# 温度传感器原版风格 UI 与 CUI theme 复用

- Time: `2026-11-16 15:26:54 UTC`
- Author: `OpenAI Codex`
- Status: 界面调整、构建和真实客户端验收完成。
- Scope: 仅外观、排版及文档；不改方块、实体、菜单、功能或同步协议。

## 改动

界面从 240×160 深色文本面板改为 196×124 原版浅灰主题面板。调用现有 VanillaTheme.drawUIBackground/UITextColor/isUITextShadow；不创建新的主题或背景纹理。保留原版 EditBox/Button，以既有 temperatureProbe 物品、红石火把物品及 T*、比较式、✓ 表达信息。描述性语言仅在悬停提示出现。输入解析、配置发送、反转按钮回调和数据更新流程保持原有行为。

Theme 的绘制方法只接受 GuiGraphics 和坐标，可直接从原版 AbstractContainerScreen 调用；无须更换 CUI screen adapter。CUI RealBox 仍不能作为原版 widget 直接插入，这与 Theme 绘制方法可独立调用是两件不同的事。本轮未接入 CUI 元素树或自动主题继承，按钮/输入框由原版渲染。

## 验证

- `./gradlew --offline assemble`：BUILD SUCCESSFUL in 58s。
- `git diff --check`：通过。
- `./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle`：真实客户端运行，通过既有桥打开创造存档 temperature-sensor-client-20261116，空手右键 (3,65,4) 传感器。
- 实际查看新界面，确认主题九宫格边缘、温度计、红石火把、✓、负小数输入和比较符号无缺图或截断；通过输入框将目标从 -12.5 改成 0、点击 ✓、切换回正常模式。现场温度 -10.2℃，输出 0，符合已有规则。
- 游戏原始截图：`run/agent/screenshots/01794842807532683100-temperature-sensor-vanilla-final.png`，1280×720，guiScale 3；已实际查看，无生成/重绘/拼图。
- bridge quit 正常退出，runClient BUILD SUCCESSFUL in 2m 54s。

## 文件和文档影响

- `src/main/java/com/teammoeg/frostedheart/content/climate/block/sensor/TemperatureSensorScreen.java`
- `docs/climate/world-climate-and-temperature.md`：追加当前界面表示、主题调用条件与验收证据。
- 本日记；截图和测试世界留在被忽略的 run/。
