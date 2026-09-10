# 材料闭环计划的复杂度与性能复查

- Time: `2026-09-10 01:14:08 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `复查并补充现有材料温度计划；未改生产代码`

## Completed

- 原位修订[材料闭环计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md#material-temperature-closure)，增加工程复查结论并直接替换不够精简或不可执行的条款。
- 删除持久finiteNodeCount，复用arena kind表达材料transport，使纯Air变化不触发材料红外。
- 红外改为热pass直接输出R16I温度，去掉R32UI对象标记附件；背景由11,664值降到729值，统一到物理Page自然边界。
- 补充cut读取、材料替换reset、一次性dormant恢复、墓碑压实、source删除revision、实体首次tracking、DDA起始voxel/形状、物品满额近似与具体开销。

## Decisions

- 不将纸面状态减少宣传为已实测最佳帧率。保留统一热pass以满足可靠归属/玻璃/实体，最后做一次同画面的GPU对照选型，不保留多套渲染方案。
- 只读检查当前Embeddium缓存jar的类和字节码：CompactChunkVertex stride为20，0..19字节均被使用；新增owner/class字段按24字节stride计成本，不能假设有空洞或挪用光照位。没有复制/重编依赖，也未改变版本。
- 现有ItemEnvironmentSampleCache只保存本tick64位置，不存在旧样本保留和调度队列；本轮计划不新增这些机制。
- 当前BrickMigrationKernel确实会把dormant Air mean写给材料；计划明确删除此回退，并防止重编时重放旧checkpoint。
- Documentation impact: 游戏行为未变，living docs不写入预期功能；实施时的文档清单保留在计划内。

## Validation

- 对照当前mesh、arena、query、topology、migration、source索引、DDA、物品缓存和自然背景代码检查具体符号与成本。
- 使用javap只读核对当前Embeddium的CompactChunkVertex、ChunkVertexEncoder、DefaultChunkRenderer和ChunkShaderInterface接口及顶点布局。
- 检查改动Markdown链接、冲突旧条款、数值预算及git diff --check；本轮无生产代码改动，不执行编译或游戏测试。

## Remaining

- 计划仍为ready。实施A–G，并实际验证真实Forge场景、Vanilla/Embeddium/Oculus图像、100观察者与高物品密度负载；性能目标尚未实测。
