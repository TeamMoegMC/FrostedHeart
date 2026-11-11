# 修正手持观察中央遮罩开口

- Time: `2026-11-11 12:57:31 +08:00`
- Author: `Codex；OpenAI；主代理`
- Status: `partial`
- Scope: `ObservationProgressScreen` 手持物品观察的客户端遮罩。

## Completed

- 根据玩家截图定位中央亮区：手持观察分支返回固定的中央矩形开口，背景只在开口之外变暗。
- 手持观察改为绘制完整屏幕的渐进半透明暗色遮罩，不绘制开口与边角标记。世界方块、实体和掉落物仍沿用目标投影开口，进度与操作提示保持。
- 文档影响：同步更新 `docs/knowledge/observations-and-notes.md` 的遮罩说明。未修改原始设计与伴生整合包。

## Decisions

- 手持目标没有对应的中央世界投影，因此不保留中央空白窗口。

## Validation

- 源码检查确认手持分支只绘制全屏遮罩，目标投影计算仅在世界观察分支执行；`git diff --check` 通过。
- 默认 Java 8 启动构建失败；临时切换到已安装的 JDK 17 后尝试 `compileJava`，Gradle JVM 因本机原生内存不足退出。
- 将构建堆降至 1536m、元空间 512m、处理器 2、worker 1 后重试，仍在编译阶段退出（`hs_err_pid29148.log`、`hs_err_pid33752.log`）。未修改持久 Java 或 Gradle 设置，未关闭玩家运行中的游戏或其他进程。

## Remaining

- 编译验证受本机内存不足阻塞；尚未在游戏内验证修正后的画面，运行中的客户端也尚未加载本次修改。
