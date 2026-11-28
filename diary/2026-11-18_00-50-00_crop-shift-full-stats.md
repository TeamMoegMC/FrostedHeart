# Shift 完整作物信息

- Author: OpenAI Codex
- Time: 2026-11-18 UTC
- Status: 完成，真实客户端截图通过。

PlantTempStats.getStats 在世界内按住 Shift 时始终展示存活、生长、最快生长温度、光照范围和冰雪抗性，不再按条件是否满足隐藏；保留当前状态及未满足条件红色标题。物品基础提示和非 Shift 简要逻辑保持原行为。同步 docs/climate/crop-fertilization.md。

验证：./gradlew runClient --offline -I Scripts/dev_client_bridge.init.gradle，BUILD SUCCESSFUL。在隔离存档 fertilizer-display-20261118 真实按住 Shift：终极保温肥料显示 Can Grow、存活 -25～35℃、生长 -15～30℃、最快 -5～24℃、光照 80～100%、肥料 100%；普通耕地显示 Near Death、存活 -5～35℃、生长 5～30℃、最快 15～24℃及相同光照范围。人工查看两张 1280×720 截图，面板完整可见。路径：run/agent/screenshots/01794962988808766600-shift-full-preserved.png、01794962994125581200-shift-full-ordinary.png。首条 setblock 提示 Could not set the block 是因为土壤已为目标状态，后续更换普通耕地成功。已正常退出。git diff --check 通过。未覆盖其他作物与界面缩放。
