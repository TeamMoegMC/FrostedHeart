# 栽培橡树真实客户端验证

- Time: `2026-11-18 03:41:45 UTC`
- Author: `OpenAI Codex`
- Status: 指定使用路径验证通过，按用户授权提交并推送；提交及远端结果由 Git 记录确认。
- Scope: 独立栽培橡树；本轮仅补充验证文档，不修改功能代码。

## 执行与结果

运行 `bash FrostedHeart/gradlew -p FrostedHeart --offline runClient -I Scripts/dev_client_bridge.init.gradle`，日志 `build/cultivated-oak-client.log`。首次 bridge state 查询在资源加载期间超时，待初始化完成后成功；不是游戏崩溃。

创建一次性创造世界 `run/saves/cultivated-oak-check-20261118`，种子 20261118，未操作现有玩家世界。

1. 通过 `/item replace` 持有注册的树苗 BlockItem，真实鼠标右键点击 `(0,80,0)` 泥土顶面，在 `(0,81,0)` 成功放置树苗；读取 NBT 确认初始进度为 0。检查截图，物品和幼苗均正常渲染。
2. 切换空手，真实右键树苗，收到 `Stage 0/4 ... Growing`，没有异常。
3. 临时命令 `heat_adjust set 0 81 0 6 20` 覆盖环境温度为 20°C，临时设置 randomTickSpeed=500，执行自然随机刻而非直接调用 advance。放置 `(0,86,0)` 障碍，等待 320 ticks 后暂停随机刻，NBT 为 Progress=16、YieldCredit=16、TargetHeight=6、TargetLogs=6；age=4 条件命令输出 `OAK_MATURE_PASS`。
4. 空手右键成熟树苗，收到 `Waiting for space`。移除障碍、再次启用随机刻后生成树，逐格检查 `(0,81..86,0)` 全是 oak_log，输出 `OAK_SIX_LOGS_PASS`；查看生成树截图，树干与树冠均正常渲染，没有崩溃。
5. `/save-all flush` 在集成服务端不是可用命令，未把该命令计为成功验证。随后正常 disconnect 返回 TitleScreen，再 quit；服务端退出过程中完成保存，日志显示 `BUILD SUCCESSFUL in 4m 43s`。外层执行会话返回 143，桥接 disconnect/quit 均返回成功，日志未出现本功能的崩溃报告。

所有命令结果保存在 `build/cultivated-oak-client-check.json`，截图：

- `run/agent/screenshots/01794973189643210900-cultivated-oak-placed.png`
- `run/agent/screenshots/01794973249308808700-cultivated-oak-mature-blocked.png`
- `run/agent/screenshots/01794973285483854000-cultivated-oak-grown.png`

已查看三张原始 1280×720 截图。客户端存在此前已有的无关资源缺失、认证联网超时及旁白库缺失日志，未修改这些问题。临时世界、截图、命令产物和日志均不纳入 Git。

## 验证边界与文档

此前 4/4 Forge GameTest 和 assemble 已通过，本轮功能代码未修改；未重复运行这些检查。自然随机刻使用恒温和加速刻率，不代表默认速率下所有气候的长时间稳定性；未覆盖独立进程重启或全部模组组合。更新 `docs/climate/cultivated-oak.md` 的当前验证状态；保留上一条开发日记原文。

Git 操作前 fetch origin，HEAD 与 origin/master 均为 812b293d4，ahead/behind 为 0/0，工作区改动均属于本任务。
