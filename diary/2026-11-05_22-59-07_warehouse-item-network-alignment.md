# 修复仓库物品同步注册表前缀读取错位

- Time: `2026-11-05 22:59:07 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for the protocol correction and focused Forge validation
- Scope: `SimpleItemKey.fromBuffer`、仓库网络回归检查与城镇实现说明

## Completed

- `SimpleItemKey.writeTo`已写入注册表名称和物品ID；原`fromBuffer`却用`readRegistryIdUnsafe`只读取ID，导致`minecraft:item`的剩余字节被当成NBT。`i`、`n`错误解释出的长度为`0x696E = 26990`，与用户断线截图相符。
- 生产代码只将读取改为配对的`readRegistryId()`，完整消费注册表前缀后再读NBT。保留现有发送格式、数量VarLong、库存归属和拆除生命周期，不增加网络版本分支或存档迁移。
- 新增`WarehouseNetworkGameTests`三个Forge病例，使用真实物品注册表及正式数据包构造函数：普通无NBT物品、模组物品与嵌套Unicode NBT、完整和增量多条目。覆盖`1L << 40`数量、数量零删除及空全量更新，并核对完整消费输入和后续字段对齐。
- Documentation impact: 更新[城镇实现说明](../docs/town/implementation-reference.md)中的仓库包字段、读写约定、错误机制和验证入口。没有修改`design/`、整合包、配方、KubeJS或其他玩法。

## Decisions

- 修正接收端来匹配既有发送格式，消除根本的字段错位；没有增加截断、吞异常、内存额度或NBT长度放宽。
- 用户报告的场景为其他玩家拆除仓库后放物品。确定的网络错误不依赖拆除；现有`canAccessWarehouse`与核心移除规则保留，本批不将另一个尚未复现的生命周期问题混作已修复。
- 不复制历史工程或编译旧版本；当前源码使用独立生成目录，沿用本机现有Forge构建工具仓库和原有动态工具选择。没有改变项目依赖或版本要求。

## Validation

- Java17 `gradlew.bat --offline assemble compileGameTestJava -I build/warehouse-network.init.gradle --console=plain`通过，5m03s；[编译日志](../build/warehouse-network-compile-20261105.log)。前期ForgeGradle工具解析曾等待外部连接，随后正常完成。既有聚焦验证配置排除了无关、未完成的deposit GameTest夹具，没有编辑其源码。
- `runGameTestServer`选择`frostedheart_warehouse_network`，**3/3 required用例通过**，退出码0，52s；[运行日志](../build/warehouse-network-gametest-20261105.log)。使用真实Forge运行时、4GiB最大堆、8个可见处理器，没有启动JUnit。普通和带NBT键、正式完整/增量包、大小数量与尾部字节均通过。
- `git diff --check`在本批修改范围通过。正式[安装包](../build/warehouse-network-20261105/libs/frostedheart-1.20.1-0.7.8.jar)为14929533 bytes，检查`SimpleItemKey.class`包含`readRegistryId`且不含旧`readRegistryIdUnsafe`引用，GameTest类0。
- 构建沿用共享工作区当时已有的`mod_version=0.7.8`，该版本调整不是本批所作。安装包包含工作区其他原有改动；未自动安装、提交或发布。

## Remaining

- 未进行两位真实客户端同时拆除和操作仓库的现场重放；本批已验证并修正的是截图对应的确定性数据包读取错位。
