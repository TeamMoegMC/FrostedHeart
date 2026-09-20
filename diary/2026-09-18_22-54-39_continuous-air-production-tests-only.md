# 连续空气验证改为真实生产测试

- Time: `2026-09-18 22:54:39 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed; 计划修订，未开始生产实现或运行测试`
- Scope: `连续空气工程计划与代码设计的实施顺序和测试范围`

## Completed

- 按用户要求修订[工程计划](../plans/2026-09-18_20-53-28_continuous-air-r4-cpu-engineering.md)及[代码设计](../plans/2026-09-18_21-27-47_continuous-air-code-maintainability.md)：不新增或运行JUnit，删除独立数值参考解、纯Java驱动、矩阵/算子单元测试和合成能量测试要求。
- P1改为直接接通真实source、Engine、topology、solver、publication和查询；P2在同一生产实现上测试实际场景与优化；后续阶段补全覆盖，不再先做独立数学原型。
- 验收通过真实世界放置/点火/机器操作与世界tick，读取正式温度、材料BlockState和实际保存恢复。GameTest仅作场景自动化，不准通过创建隔离engine、写arena H或伪造ACK替代生产路径。

## Decisions

- 核对发现现有ThermalTimingGameTests/ThermalAirMixingGameTests部分用例直接创建engine或改H；这些不作为本次生产证据，也不无筛选运行整个旧套件。
- 旧实现和优化前后分别运行真实服务器场景；不新增影子服务、测试参考内核或微基准项目。
- 取消细参考后，原相对细参考≤2°C的证明不再是交付；生产场景平移差、温度跳变、优化前后差和保存差各自记录，不冒称绝对精度认证。求解收敛仍是生产算法本身。
- 旧存档兼容仍排除。此前diary保留历史，本条更新原测试决定。
- Documentation impact: 仅修订plans，当前生产行为未改变，活文档无需更新。

## Validation

- 核对现有GameTest调用及build.gradle的默认namespace选择；记录真实生产测试需明确选用例，重启测试须保留实际测试世界。
- 检查两份计划中的测试要求、阶段依赖、运行入口和术语一致性。
- 本次未运行JUnit、编译、GameTest或服务器。

## Remaining

- 实施时从实际服务器基线及首个生产闭环开始，所有通过结论以实际执行的生产场景为依据。
