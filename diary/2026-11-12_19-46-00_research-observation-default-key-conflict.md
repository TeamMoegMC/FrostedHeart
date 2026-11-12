# 观察默认键与TreeChop冲突

- Time: `2026-11-12 19:46:00 +08:00`
- Author: `Codex / OpenAI GPT-6 Astra；主协调；6.1 Sol协助依赖只读核对`
- Status: `completed`（默认配置与文档；解冲突后的完整客户端路线仍在执行）
- Scope: TWR新安装的默认选项、隔离Playtest按键与FH操作文档；无Java变更。

## Completed

- 最终M6客户端先验证了空队伍联络候选、200tick配置/实际201tick等待、想法出现后三项材料仍未满足。之后按N打开TreeChop设置页，未完成观察；失败会话保留于隔离实例 `build/m5-m6-client/session-1794483271091/`。
- TreeChop 0.19.0-fixed的`KeyBindings.registerKeyMappings`将设置页默认设为N，与`ObservationClient.OBSERVE`相同。其客户端TOML没有改键字段。
- TWR新增 `kubejs/config/defaultoptions.txt`，只含 `key_treechop.key.open_settings_overlay:key.keyboard.unknown`。提交为 `3faf8096c`；原`config/.gitignore`暂存原样保留。观察仍默认N，树木砍伐本身不受影响。
- 隔离Playtest已有options，单独解除这一冲突供本轮验证和后续试玩；原Development及用户其它实例未改。已有玩家在控制设置自行解决重键，详见[开始研究](../docs/knowledge/player-guide.md)。

## Validation

- 主协调直接读取当前KubeJS 2001.6.5-build.16的`KubeJSClient.copyDefaultOptionsFile(File)`字节码：只在目标options文件不存在、默认文件存在时复制；不会覆盖既有按键设置。不引入脚本、额外模组或每次启动强制重置。
- 配置仅一行，与实例解冲突使用的原版options键名/值一致；Git差异检查通过。新用户首次复制行为目前是依赖代码证据，不冒称另建全新配置客户端实测。
- 同轮只读补核正式Charcoal Pit 3.12.0的`BlockBarrel.use`流体能力分支，支持杂酚油桶经`FluidUtil.interactWithFluidHandler`灌入桶槽；据此修正冶铁炉文档中过时的接口未知说明，仍不宣称生存实玩。

## Remaining

- 客户端用同一最终生产JAR和helper重新验证实际N观察、学习和实验，随后归档独立结果。
