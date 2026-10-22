# 世界生成

本目录解释 FH 与配套 TWR 使用的已实现世界生成系统。Java 源码、TWR 的 `kubejs/data/` 与实际生效配置是最终依据；文档状态和核对日期说明可靠性。

冰霜洞穴从[第二版当前实现](ice-caves.md)开始，查阅覆冰、地下湖、大型冰体、陷阱、晶体和冰锥的源码入口、当前参数与验证边界。[首版实现详解与实测对照](ice-caves-implementation-review.md)保留为首版快照，用于解释旧截图和旧结果，不作为第二版行为说明。其他矿物统计口径见[矿物生成量对比](../worldgen-mineral-generation-estimate.md)。

修改 feature 类型进入 FrostedHeart，修改布置、频率或群系挂载进入 TheWinterRescue。先读相关仓库的 `AGENTS.md`，对照当前文档及数据，执行所需验证；跨仓库引用变更分别检查两库。未实施目标放在 `plans/`，开放讨论放在 `discussion/`，完成和验证记录追加到 `diary/`。
