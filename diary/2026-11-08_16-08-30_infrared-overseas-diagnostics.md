# 国外玩家仅蓝色扫描的架构核对与现场诊断

- Time: `2026-11-08 16:08:30 +08:00`
- Author: `OpenAI Codex / GPT-6; primary investigation and implementation agent`
- Status: `partial`（架构说明、诊断工具与构建完成；远端故障待现场证据）
- Scope: 红外请求/回复可观测性、客户端提交/镜像/渲染状态、自身位置服务端探测及文档

## Completed

- 用户确认两端使用同一份新JAR；国外玩家有蓝色扫描，同位置国内玩家能看到同一个热源热色。按此确认继续，不再将版本不一致或单纯跨国延迟作为既定原因。
- 核对当前链路：服务端合成最终材料/解析场显示温度，0.25°C量化后发送标量与增量位置/版本；客户端以144³镜像和整数纹理渲染。没有传输求解矩阵或完整物理模拟状态。
- 明确前期20项回归与带宽数字的资格：没有实际国外客户端收包和GL显示证据；当前反馈不能被称为整体联机问题已修复或架构已最优。
- 新增`InfraredDiagnostics`与`/fh_ir_server status|trace`。status使用现有raw-baseline full查询，不改变客户端事务或显示去重基准；Summary区分无snapshot、无有效值和实际温度数据，跟踪按玩家显式启用，离线/停服清除。
- 新增本地`/fh_ir status|trace|resync`，报告请求等待、数据片/ACK/提交/忽略计数、暂存与基准、CPU镜像有效值/范围及渲染标记。跟踪记录SEND/PART/ACK/COMMIT/REJECT，默认关闭；镜像扫描仅用于人工查询或显式跟踪。
- 请求与回复数据格式未变化，没有添加周期重试、身份限制或新的热模拟状态。没有根据未知现场原因改写协议或渲染算法。

## Decisions

- 先取得同一请求在服务端生成、客户端收到、LAST提交和实际渲染之间的证据，再决定功能修复。蓝色既可以是缺失数据，也可以是有效冷值，不能独自证明“网络收不到”。
- 本次沿用当前工作区版本0.7.10，产物为新增命令的诊断构建；未替换实际测试服或客户端文件。
- Documentation impact: 新增[红外架构与诊断](../docs/climate/infrared-view.md)，更新气候README入口及世界温度链接；[验证摘要](../docs/climate/validation/infrared-diagnostics-2026-11-08.json)记录当前反馈和未定位状态。

## Validation

- Java17 `assemble runGameTestServer`，diagnostics/network/refresh三个namespace，13项全部通过，`BUILD SUCCESSFUL in 1m 48s`。日志：`build/infrared-diagnostics-validation-20261108.log`。
- 新增摘要实际解码、空full与无snapshot、INVALID与实际冷值、零权限玩家自身服务端查询/跟踪命令四项，保留原有请求/编解码与旧区域删除回归。
- 随后补充客户端拒绝原因日志和计数，并重新`assemble`成功，`BUILD SUCCESSFUL in 56s`；它们未改变接收分支或wire。日志：`build/infrared-diagnostics-package-20261108.log`。
- 生产JAR：`build/infrared-refresh-20261105/libs/frostedheart-1.20.1-0.7.10.jar`，14622983B；Manifest为1.20.1-0.7.10。诊断生产类存在，GameTest不进入生产包。
- 使用当前工作区独立输出和既有工具解析，未复制旧源码、固定工具版本或修改配套仓库/配置；本次修改文件`git diff --check`通过。

## Remaining

- 需要问题客户端的`IR_CLIENT`状态和对应服务端的`IR_SERVER`状态/同请求trace，才能确认是请求、生成/排队、接收/提交还是渲染问题。当前没有读取该国外客户端或实际测试服务器日志，未独立验收客户端GL命令/画面。
