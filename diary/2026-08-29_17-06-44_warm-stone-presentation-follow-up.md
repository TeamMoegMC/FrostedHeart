# 暖石显示细节修正

- Time: `2026-08-29 17:06:44 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `partial`
- Scope: `暖石物品纹理、Curios 空槽图标，以及延后的 Tooltip 显示需求`

## Completed

- 将 `warm_stone.png` 重绘为原创的 `16x16` 灰色不规则石块，以暗轮廓、石质明暗和细裂纹表达蓄热石，不再显示橙黄色发光核心。
- 将 `empty_warm_stone_slot.png` 重绘为同轮廓的半透明纯灰阶图标，移除原有棕黄配色。
- 保持 `hot_water_bag.png` 不变；本轮不修改任何 runtime、配置、语言或 Tooltip 代码。

## Decisions

- 只参考《饥荒》热能石的低饱和、手绘石质方向，不复制其具体 sprite、轮廓或像素布局。
- 当前会话未提供 imagegen 内置生成器；没有擅自切换到需要 `OPENAI_API_KEY` 的 CLI fallback。由于目标是精确 `16x16` 像素资产，最终使用确定性逐像素重绘并在写入后检查实际 PNG。
- Tooltip 的新目标是默认只显示按节点热容加权的总体温度，并通过客户端 config 切换到内部/表面双温度调试显示。该实现会修改 `WarmStoneItem`，与 T15 的精确 `ItemEntity` tick hook 属于同一文件，因此按用户要求暂缓，不与 C2 world runtime 并行。
- Living docs 当前只声明资源存在和 Tooltip 的实际旧行为；本轮没有修改已实现 Tooltip 合同，因此无需更新 living docs。

## Validation

- 两张 PNG 均为 `16x16`、32-bit ARGB，透明背景保留。
- 空槽图标的所有可见像素均满足 `R=G=B`；暖石所有可见像素的最大 RGB 通道差不超过 `9`，不存在原有橙黄核心色。
- 使用原始尺寸和放大预览检查轮廓、裂纹、透明边缘与空槽可读性。

## Remaining

- 等用户在 C2 的 T15/T16 文件修改完成后明确恢复，再实现“加权总体温度默认显示 + config 调试双节点显示”，并补语言、配置测试、Tooltip 无 NBT 写入回归和 living docs。
