# 保温肥料客户端作物信息

- Author: OpenAI Codex
- Time: 2026-11-18 UTC
- Status: 实现完成，真实客户端对照截图通过。

PlantTempStats 按 CropFertilization.soil 读取根部肥料，以服务端相同的 5 * grade.preserve 调整三个最低温度；状态与展开范围共用调整后数据。修复温度偏低判定误用 minSkylight。同步 docs/climate/crop-fertilization.md。

验证：runClient --offline -I Scripts/dev_client_bridge.init.gradle 完成生产编译并实际进入隔离世界 fertilizer-display-20261118。手持土壤温度计观察小麦，终极保温肥料显示 Can Grow 和 100% 储量；替换为普通耕地后显示 Near Death / Temperature too low / -10.0°C。截图分别为 run/agent/screenshots/01794962108830883600-preserved-clean.png 与 01794962121462285500-untreated-crop.png。已人工查看两张截图。通过桥接 quit 正常退出。git diff --check 通过。未实测其他等级、多层作物。

后续 Shift 实机验证：真实按住 Shift，普通耕地显示存活 -5～35℃、生长 5～30℃、最快 15～24℃；终极保温肥料显示可以生长、最快 -5～24℃及 100% 储量。满足条件的存活和生长范围沿用原逻辑隐藏。截图：run/agent/screenshots/01794962483565627600-shift-ordinary.png、01794962489483009800-shift-preserved.png。已查看截图并退出客户端。
