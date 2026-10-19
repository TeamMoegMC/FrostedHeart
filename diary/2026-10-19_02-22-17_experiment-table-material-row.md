# 实验台：材料槽排成一排，闲置时按秒刷新

- Time: `2026-10-19 02:22:17 -0500`
- Author: `Claude; Anthropic Opus 5.5; 修复`
- Status: `completed`
- Scope: `knowledge/experiment/ExperimentTableMenu`、`knowledge/experiment/ExperimentTableBlockEntity`、`knowledge/client/experiment/ExperimentTableScreen`、知识与研究文档

## Completed

项目所有者在游戏内测试 `pressure_control` 时提出两个问题。

- **看不出第二种材料该放哪里。**
  - 原因：界面沿用绘图台左侧面板。第一格材料槽在检视槽的位置，后两格在纸和笔的槽位，带着纸和羽毛笔的提示图标，中间还隔着放大镜按钮，看起来像绘图台的槽位，而不是材料槽。
  - 修复：三格材料槽改为「材料」标签下的一排，位于面板纸面上的 `(21, 34)`、`(41, 34)`、`(61, 34)`，由界面画出格子。面板右侧那一列（检视槽、放大镜、纸、笔）用同色的木纹底色盖住。
- **放进 4 块木板后仍显示 0/4。**
  - 原因：R2 依赖物品处理器的 `onContentsChanged` 标记刷新。Shift 点击合并到已有物品堆时，菜单直接改数量，Forge 的 `SlotItemHandler` 不会通知处理器，所以这类变动不触发刷新。测试用的客户端也可能早于这次标记修改。
  - 修复：去掉标记。选定实验的闲置实验台改为每 20 tick 直接读取槽位，重算材料与温度，报告有变化才同步，不重新扫描房间。不再需要的选择也会在闲置时清空。
- 顺带修正一个边界情况：找不到归属队伍的数据时，实验台原先会当作「实验已不再需要」，清空选择，运行中的进度和已投入的材料也随之丢失。现在只等下一次检查；只有知识数据确认这项实验已不再需要时才清空。

## Decisions

- 不另画实验台专用的界面贴图，先用填色盖掉绘图台专属的槽位。以后有专用贴图时，可以直接替换背景绘制。
- 闲置刷新只在选定实验后进行；没有选择的实验台每 20 tick 只做一次判断就返回。
- Documentation impact：更新了 [实验台](../docs/knowledge/workbench.md#实验台)、[研究玩法](../docs/research/gameplay-and-integrations.md#knowledge-experiment-table) 和 [计划 Outcome](../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md#outcome) 中闲置刷新的说明。

## Validation

- `./gradlew compileJava` 通过。
- 用贴图按新坐标拼出左侧面板，核对了材料行和被盖住的右侧列。
- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`：BUILD SUCCESSFUL，135 个测试，0 失败。
- `./gradlew runGameTestServer`：All 107 required tests passed。
- 第一轮 GameTest 运行期间，我同时执行了 `compileJava`，服务器加载到一半的类文件被重写，4 个测试因 `NoClassDefFoundError` 失败，其中包括实验房间测试。在最终代码上重跑的单元测试和 GameTest 全部通过，没有类加载错误。
- 没有在游戏内看过，需要重启开发客户端。

## Remaining

- 游戏内确认：三格材料槽的点击与 Shift 点击，以及材料行在一秒内变为 4/4 和 1/1。
