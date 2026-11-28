# 控温实验炉的燃料火焰

- Time: `2026-11-28 12:35:46 +08:00`
- Author: Codex（OpenAI，界面实现与验收）
- Status: completed
- Scope: 控温实验炉的燃料显示。

## Completed

- 使用游戏自带的熔炉火焰贴图。彩色部分随当前燃料下降而从上到下缩短。移除燃料条和秒数提示。更新了[控温实验炉说明](../docs/knowledge/controlled-furnace.md)。

## Validation

- 构建通过。在临时整合包实例对比未点燃、刚点燃和剩余燃料减少时的截图，确认火焰缩短。截图保存在 `build/furnace-flame-client/`。后续停炉检查遇到整合包剧情弹窗。

## Remaining

- Research Playtest 正在运行并锁定模组文件。退出游戏后可同步新包。
