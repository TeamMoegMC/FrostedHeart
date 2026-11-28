# 能量塔过载警告接入现有弹窗

- 作者：OpenAI Codex
- 日期：2026-11-26
- 状态：完成源码修改与离线编译；客户端显示未实测。

将 GeneratorData.tickFuelProcess 原有危险／临界阈值聊天警告改为 TeamTownData.pendingTowerTipSignals 队列通知，与供热开关提示复用 TownSignalNotificationPacket 和 TownSignalTipPresentation。危险为 WARNING，临界为 CRITICAL；同次跨越两档只发临界，保留原阈值与损坏逻辑。新增枚举追加在末尾，补全严重度、领域映射与中英文文本。遵循现有城镇提示设置，不重复发聊天消息。未修改已有树木工作区改动。

文档：更新 docs/town/town-model.md 的过载提示行为。diary/README.md 不存在，参照已有日记格式。

验证：bash gradlew --offline compileJava 成功（29 条既有 API／Mixin 等警告）；中英文 JSON 解析通过；git diff --check 通过。编译后仅调整新增两行缩进。未运行游戏客户端，后续可在启用城镇事件提示时分别跨越危险、临界阈值，检查弹窗及聊天无重复。
