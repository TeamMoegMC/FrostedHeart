# 城镇建筑GUI增量同步、无空白刷新与真实线格式修复

- Time: `2026-11-07 16:22:09 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed` for implementation, unified Forge regression and the stated real rendered-client scenario
- Scope: 城镇同步入口、建筑元数据通知、客户端视图生命周期、通用ObjectWriter稀疏列表读取、相关真实测试与当前文档

## Completed

- `TownSync.ensureCurrent`让建筑菜单、镇长印章和无城镇的状态查询复用当前连接基线；完整请求在已有Baseline内合并并于Server END捕获，不因重复GUI检查重置退避。实际完整发送以DEBUG `TOWN_VIEW_FULL`记录原因。
- `TownBuildingStore`元数据回调只对增量未包含的管理元数据标记`viewChanged=true`。普通已解析建筑新增/删除沿REGISTRATION与已有建筑增量；保存、恢复原文、派生拓扑通知保留，维度确认和恢复摘要变化仍能请求完整视图。
- 同一holder的PENDING保留当前城镇显示对象，不触发整批GUI通知；暂停后续增量，完整候选解码后一次安装并恢复。不同holder、未知归属、ABSENT和断开连接清理旧视图。没有复制第二份旧快照，也没有新增生产类型、线程、ACK协议、队列或包字段。
- 真实客户端暴露出实际的反复恢复触发器：`ObjectWriter`把混合列表中的省略字段槽从Java `null`读成`DataOps.NULLTAG`，严格可选建筑字段随后报`Not a Number`。读取列表时类型标记0现恢复为null；其他类型和原线上字节保持。标记区由readBytes改为readSlice，退出一份未释放的临时复制buffer。
- 生产修改10个原文件，76行新增、58行删除，净增18行；包括诊断原因和注释，没有新生产框架。保留工作区既有仓库协议修复和其他会话的开发内容。
- Documentation impact: 更新[保存与运行边界](../docs/town/persistence-and-runtime.md)、[建筑数据](../docs/town/building-data.md)和[实现说明](../docs/town/implementation-reference.md)的同步规则、真实线错误、当前验证范围和入口。未改动`design/`、整合包、KubeJS、配方或数据包。

## Decisions

- 修复范围同时包含重复全量触发与使增量持续失败的根因。只保留旧画面会隐藏解码恢复循环，不能作为完成标准。
- 复用原有脏键集合、连接基线、有序通道和20到200tick退避；完整同步仍承担首次基线、归属/组件变化、罕见元数据和失败恢复。
- GUI内容继续通过当前数据supplier绘制；无需改动住宅、狩猎站、矿场生产屏幕或增加持久UI缓存。

## Validation

- 所有正式测试均使用当前共享源码，Java17，生成目录`build/town-view-sync-20261107`。沿用已有本机Forge工具仓库和原动态工具选择，没有复制旧工程、编译历史版本或引入路径hash。临时init排除既有未完成deposit夹具，以及另一个会话正在修改、捕获已移除IOException的`InfraredBandwidthGameTests`；没有修改其源码。
- 最终命令：`gradlew.bat --offline assemble runGameTestServer -I build/town-view-sync.init.gradle -PgameTestDirectory=build/town-view-sync-release-run-20261107 -PgameTestNamespaces=frostedheart_town_view,frostedheart_buildings,frostedheart_persistence,frostedheart_warehouse_network -PthermalTestHeap=4G -PthermalTestProcessors=8 --console=plain`。**36/36 required通过**，退出0，1m59s；[最终统一日志](../build/town-view-sync-release-20261107.log)。包含新同步7项、原建筑16项、队伍持久化10项和仓库网络3项。
- 双真实ServerPlayer使用原版登录、菜单事件和EmbeddedChannel上的实际Forge载荷。A保持原住宅菜单400tick，B经仓库quickMove存入20次并实际新增/拆除另一建筑；每连接20资源增量、2建筑增量，完整信封0。重复完整请求12次在busy窗口只发一次带holder的PENDING，随后一次PRESENT完成；组件移除向两连接发送ABSENT。客户端状态用例覆盖同镇显示保留、增量暂停/恢复、换holder、ABSENT和断开连接。
- 新增线回归覆盖六种注册建筑Codec的实际write/read，省略字段、嵌套null槽、整型列表、空列表和运输默认值。前一版34项服务器检查未发现真实接收端建筑解码失败；真实客户端r2记录Not a Number恢复循环，已据此补齐回归并修正，不将该轮计为通过。
- 最终真实客户端命令：`gradlew.bat --offline runTownViewClient -I build/town-view-sync.init.gradle --console=plain`，**通过**，退出0，2m41s；[客户端日志](../build/town-view-client-20261107-r6.log)、[JSON结果](../build/town-view-client-20261107/result.json)。实际Minecraft图形客户端、集成服务器和第二个EmbeddedChannel ServerPlayer；三种建筑各600tick。检查帧1678/1680/1679，共5037，数据空白0；21次存入，第二连接资源增量22、建筑增量13、完整信封0。普通操作不额外安装完整视图，一次主动延迟恢复完整安装一次；Not a Number、城镇恢复错误及无效运输/P2P列表警告均0。
- 已检查[等待时保留画面](../build/town-view-client-20261107/house-pending-retained.png)、[恢复后](../build/town-view-client-20261107/house-recovered.png)、[狩猎站](../build/town-view-client-20261107/stage-1-stable.png)及[矿场](../build/town-view-client-20261107/stage-2-stable.png)。前序客户端启动缺少测试研究目录、脚本未等待替换方块抵达便开菜单等失败已保留；最终脚本等待实际方块实体和建筑数据后开菜单，没有为通过测试修改生产屏幕或吞异常。
- 本批diff空白检查通过。最终安装包[0.7.9 JAR](../build/town-view-sync-20261107/libs/frostedheart-1.20.1-0.7.9.jar)为14609934 bytes，修改的四个核心类在包内，GameTest/客户端验证类0。版本0.7.9沿用工作区原值，本批未改版本，也未自动安装、提交或发布。

## Remaining

- 图形验收使用本地集成服务器与一个真实渲染客户端，第二玩家为原版ServerPlayer配EmbeddedChannel；未宣称两台远程图形客户端、网络延迟矩阵或百人长时性能已验证。
- 包含共享工作区其他原有改动；本批结果不扩大到未运行的其他系统测试。没有用单次运行给出整体CPU提速百分比。
