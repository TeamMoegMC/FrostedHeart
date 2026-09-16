# 温度可读性与可维护性重构实施

- Time: `2026-09-16 15:24:40 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `按八批计划整理温度链路；不改热模型、材料导热规则、网络格式或渲染技术`

## Completed

- `TopologyPlan` 改为 `TopologyUpdatePlanner`，phase slots、实际 Air 节点、材料节点与 membership mask 名称统一；移除旧名称引用，没有兼容包装。
- `MaterialSample` 从发布器嵌套类型移到 mesh，保持原字段与复用方式；用完整 live 赋值操作交接 requestSequence。`ReadCursor` 继续嵌套，版本检查不变。
- `InfraredCapture` 移为同包具体类，Input 继续管理唯一惰性实例，调用传入当前 pages/publication/generation。保留全部工作数组、两次尝试、finally、差量协议及 closeAll 生命周期。
- Brick 编译器/迁移器、空气路由、存档容器、Page 管理和主入口展开紧凑代码。真实 Air 连通计算提取为一个返回节点数的私有方法，没有新增结果对象或遍历。
- `Transition.transitionTemperatureC` 区分相变阈值和实际温度；`isMaterialCell/materialNodeMask` 表达真实含义，不改变发布范围。
- 休眠 Air 明确 1/16°C 残差、四 short/long、节点覆盖位图和 640 B 数值预算；材料 H 仍精确存储。偏移、变更记录 stride 与 cause offset 命名，原打包和数组保持。
- 移除无效初温参数与无调用的旧温度常量，更新 WorldTemperature 查询语义。数据编译、源绑定及解析场按原行为整理；已清楚的 solver、源账本、分配器、线程池和静态辐射核心保留。
- 更新当前架构维护入口表、热源和世界温度文档；[计划](../plans/2026-09-16_00-15-34_thermal-readability-maintainability-refactor.md)标 completed，并逐组记录完成/保留理由。

## Decisions

- 不为消除每段短公式复制引入策略类或统一变更结果；活动与休眠的冷却、删除及计账生命周期分别保留。
- 除改名外，新增文件只有搬移后的 MaterialSample 和 InfraredCapture。没有新增运行期对象实例或大数组；源码行数增加主要来自展开原一行多操作与长表达式。
- 使用临时标准 Java formatter 辅助涉及文件排版，不加入构建依赖；随后人工修正语义名称和动作边界，没有使用 Python 重写源码。
- 既有植物提示缓存、半透明红外覆盖、天然岩浆初温等用户接受的边界保持；静态辐射、两层材料、原水采样及 1/0.5°C 滞回不变。

## Validation

- 重构前运行现有完整 GameTest，101/102 通过，一项水边界测试失败；重构后前两次完整运行各 102/102 通过。预热复测再次遇到同项失败，诊断明确为测试点处于 `minecraft:deep_dark`，该群系按现有配方不结冰，loaded=true、floor=-Infinity、目标仍为水。
- 只修正水边界测试：沿用既有数据注入方式隔离群系排除条件，保留 fluid boundary、能量和结果断言，finally 恢复配方与 profile。生产相变逻辑未修改。
- 最终 `gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：102/102 通过，BUILD SUCCESSFUL。之后仅整理存档局部/偏移名并用等值命名常量替换 640，`compileJava compileGameTestJava` 再次通过。
- 旧类型/方法及过时嵌套 `.class` 搜索无剩余；`git diff --check` 通过。未恢复或新增 JUnit。

### 成本证据及限制

16 源、2176 节点的既有 solver 微基准（单位 μs）原预热/采样方法未变：

| 运行 | p50 | p95 | 测量区间分配 |
|---|---:|---:|---:|
| 重构前 | 83.8 | 87.5 | 0 B |
| 重构后首次 | 68.7 | 75.0 | 0 B |
| 后续完整运行 | 104.399 | 113.5 | 0 B |
| IR 预热复测运行 | 70.199 | 75.2 | 0 B |
| 最终通过运行 | 76.5 | 82.401 | 0 B |

- 不挑某一轮宣称加速；该范围显示运行/JIT 波动。算法、节点/边工作量及分配保持，不代表完整多人 MSPT。
- 休眠 8 Section ×64 记录场景：原 p50 114.066 ns/attempt、0 B；最终 142.188 ns/attempt、0 B，其他重构后轮次为 102.734～111.328 ns。未新增持续分配；不声称每轮耗时完全相同。
- IR 原方法只测 100 请求、没有独立预热。同样 248,300 B 全量输出的原中位数 0.1646 ms，重构后未预热为 0.4537/0.4659 ms。因该差异补 256 次预热；两次结果为 0.3086/0.173101 ms，最终 p95 0.186701 ms。对应增量最终为 0.150699 ms、289,300 B。没有额外生产扫描或缓冲；不同预热方式不能据此算严格百分比收益，也不掩盖早期较慢样本。
- 空窗口最终预热结果：无变更增量无响应，全量 14,100 B；第一组大型场景前后输出量不同，不作为严格速度对照。
- GPU 控制流只补注释、未改绘制/纹理操作，没有重复做客户端图形验证。没有全量多人 heap/MSPT 或所有硬件结论。

## Remaining

- 已完成本轮重构与对应验证。性能报告限定在结构检查和上述测量；不承诺任何软件以后永远不需维护。
- 临时 formatter、运行日志与测试世界在本次收尾清理，保留有意义的 GameTest 与此前任务的文件。
