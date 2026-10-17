# Frosted Research V2 Phase 1 manual test datapack

这是一个只用于手动游戏内冒烟测试的数据包，不是正式研究内容，也不会进入模组生产资源。

将整个 `frostedresearch_v2_phase1` 目录复制到目标存档的 `datapacks/`，进入存档后执行 `/reload`。可授予的结果 ID：

```text
frostedresearch_test:smoke_finding
frostedresearch_test:smoke_design
frostedresearch_test:smoke_construction
frostedresearch_test:smoke_procedure
frostedresearch_test:smoke_prototype
```

使用 `/research result grant|revoke|info <result-id>` 操作自己的队伍，或使用 `/research <online-player> result grant|revoke|info <result-id>` 操作目标玩家的队伍。Design 管理 `minecraft:stick` 配方，Construction 管理 IE 简易高炉成型，Procedure 管理工作台右键，Prototype 引用 revision 1 的 `frostedresearch_test:smoke_profile`。普通纸张 reward 只用于验证目录声明，不会被 grant 命令发放。

测试完成后从存档中移除该目录并执行 `/reload`；已经授予的四类团队 ID 会按设计保留为 orphan 历史，但不再产生投影。
