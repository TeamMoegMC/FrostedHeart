# 暖石材质与空槽图标细化

- Time: `2026-09-08 23:56:06 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `warm_stone.png 与 empty_warm_stone_slot.png`

## Completed

- 重绘 [暖石物品材质](../src/main/resources/assets/frostedheart/textures/item/warm_stone.png)：以连贯的左上亮面、右下暗面和少量短裂纹替代原来的碎点与深色中心；保留低饱和灰石设定。
- 重绘 [Curios 空槽图标](../src/main/resources/assets/frostedheart/textures/slot/empty_warm_stone_slot.png)：与物品共享轮廓，使用低对比半透明灰阶浮雕。
- 两张资源保持 `16x16` RGBA 和原路径，继续由现有物品模型、槽位资源引用。

## Decisions

- 本次采用确定性逐像素编辑。内置 imagegen 不可用，未调用需要 API key 的生成服务。
- 热水袋仅作为同系统风格参考；物品模型和游戏逻辑无需修改。
- Documentation impact: living docs 描述的行为、模型引用和跨系统合同均未改变，无需更新系统文档。

## Validation

- 检查新旧资源在 1x、3x、10x 最近邻缩放下的轮廓、明暗，以及浅灰槽位背景上的可读性。
- 重新读取实际资源，确认两张 PNG 均为 `16x16`，外圈全透明，各有 137 个可见像素；物品可见像素完全不透明，槽位可见像素满足 `R=G=B`。
- 解析 `models/item/warm_stone.json`，确认 `layer0` 仍为 `frostedheart:item/warm_stone`。
- 纯贴图修改，未运行 Java 构建或游戏客户端。

## Remaining

- 游戏内的实际物品栏、手持及 Curios 渲染尚未检查。
