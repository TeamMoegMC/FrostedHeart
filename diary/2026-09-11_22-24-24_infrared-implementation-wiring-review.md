# 红外限定架构的工程接线复查

- Time: `2026-09-11 22:24:24 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `复查当前方案能否接入现有工程并补齐最小实现细节；未修改生产代码`

## Completed

- 修订[当前计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#block-surface-infrared)，补齐共享表面资格、Page局部可用性、完整包合同和客户端状态转换。
- 撤回MATERIAL_TRANSPORT新kind，改为现有不可变BlockBrickLayout中的一个surfaceNodeMask。当前arena、worker与PagePublication共享该layout，资格能跨线程一致读取，同时保留原Air/source/center语义。
- 将surfacePresence定义为本次可读且有表面节点的Page，复用现有96 B位图，避免局部重编使整个窗口材料都退成背景；Page恢复按新增Page补全，不仅依赖epoch。
- 增加MATERIAL_UPDATE与背景/控制的明确区分，防止背景包错误建立delta基线、改变材料origin或上传partial CPU镜像。补充客户端提交中心、服务器背景bucket、包尾背景和完整包字节预算。
- 明确背景读取主线程现有环境条目，禁止访问worker PageState；背景GPU线性序与presence位序分开，不增加转换表。

## Decisions

- 背景、材料、控制共享原两个packet，状态正交；不新增接收队列、第二份材料镜像、每玩家服务端缓存或渲染框架。
- surfaceNodeMask每个已有layout增加8 B数值载荷，full-Air为0；不增加每slot数组或全局kind分支。
- 已核对当前Embeddium仍派发Forge阶段事件，现有AFTER_CUTOUT_BLOCKS订阅足以表达挂点，无需专用Embeddium hook。
- 保持黑体近似、完整扫描圆、材料优先/背景回退和圈内统一热色，不改动既定范围。
- Documentation impact: 本轮仅改计划和日记，living docs保持当前实现描述。

## Validation

- 只读核对ThermalBrickCellLayout、BlockBrickLayout、arena staging/mixed support、PagePublication、TopologyCommitter及engine发布顺序，确认共享引用和RESERVED helper要求。
- javap只读核对当前Embeddium的WorldRendererMixin.m_172993_与SodiumWorldRenderer.drawChunkLayer：SOLID合并绘制CUTOUT，外层仍派发各RenderType阶段；不推断实际FBO已验证。
- 检查计划链接、旧kind方案残留、背景/材料控制冲突、XYZ索引与diff空白。
- 未编译或运行游戏，本轮未实施生产改动。性能状态仍为待实现与实测，静态核对不等于最佳性能证明。

## Remaining

- 按计划先验证地形FBO/depth，再实施共享mask、表面读取、恢复过滤、协议与显示；完成局部更新、partial/full、背景轴序、图像及目标负载验收。
