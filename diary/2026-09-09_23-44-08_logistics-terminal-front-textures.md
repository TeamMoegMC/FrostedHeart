# 城镇物流终端正面图案重绘

- Time: `2026-09-09 23:44:08 +08:00`
- Author: `Codex; OpenAI GPT-6; primary implementation agent`
- Status: `completed`
- Scope: `shipping_terminal_front.png、receiving_terminal_front.png、bidirectional_logistics_terminal_front.png`

## Completed

- 重绘[发货终端正面](../src/main/resources/assets/frostedheart/textures/block/shipping_terminal_front.png)：左侧带箱盖和锁扣的箱子，右侧蓝色向外箭头，箭头上方为货物方块。
- 重绘[收货终端正面](../src/main/resources/assets/frostedheart/textures/block/receiving_terminal_front.png)：相同箱子与货物布局，橙色箭头指向箱子。
- 重绘[双向物流终端正面](../src/main/resources/assets/frostedheart/textures/block/bidirectional_logistics_terminal_front.png)：两个货物方块，下方为橙色向左、蓝色向右的双头箭头。
- 三张正面使用 [transport_station.png](../src/main/resources/assets/frostedheart/textures/block/transport_station.png) 的共用城镇底纹，清除中央信封后覆盖新图案，统一木框与灰色外缘。

## Decisions

- 逐像素绘制，保留 `16x16` RGBA、不透明像素和原资源路径。货物用低饱和米灰色明暗面区分，箭头主色直接沿用发货/收货接口材质。
- 共用底纹的中央 `x=3..12, y=4..11` 为图案覆盖范围（零起点）；该范围之外的 176 个像素与货运站原材质完全一致。
- 模型仍将角色材质映射到 `north` 面，由既有 `blockstates` 根据 `facing` 旋转。
- Documentation impact: 当前系统文档未限定旧图案，已描述的功能、面分工、模型引用及放置规则均未改变，无需更新 living docs。

## Validation

- 检查三张材质在原尺寸及 2x、4x、14x 最近邻预览下的形状、箭头方向和配色。
- 重新读取实际保存的三张 PNG，验证尺寸、RGBA 模式、完全不透明及共用边框像素一致性。
- 解析三份模型及 blockstates，确认 `north` 正面引用与全部六向旋转一致。
- 纯贴图修改，以静态预览和资源检查验证；未运行 Java 构建或游戏客户端。

## Remaining

- None.
