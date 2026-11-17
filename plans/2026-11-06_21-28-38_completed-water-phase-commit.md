# 已完成水体相变的实际提交与余温退出

- Time: `2026-11-06 21:28:38 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary implementation and production-validation agent`
- Status: `completed`
- Scope: 已完成相变的主线程提交、自然冻结边界、在线/休眠/粗档及旧H恢复
- Related: [上轮实测](../docs/climate/validation/thermal-similar-mechanisms-2026-11-06.json)、`MinecraftPhaseController.canTransition/applyDormantTransition/apply`

## Verified Current State

无热量记录的自然冻结与已完成潜热的物理转换共用 `canTransition`，两者均重查 `world_conditions.fluid_boundary_tag`。已冻结到终点的内水体因此等不到变块，既不能继续按目标材料散热，也不能释放保存记录。上轮真实Forge12000tick保持-5°C，改成岸边后才继续。

## Decision

`fluid_boundary_tag`约束无H记录的自然冻结启动；已经完成能量过程的在线/保存材料提交不再受这项启动几何约束。仍核对真实方块和已完成H，并保留随机刻开关、群系禁止、最低高度、解析场保底等明确玩法约束。复现中的水不再等待人为造岸，直接兑现薄冰，然后依现有H和后续相变继续降温。无需新增第二份温度、强制清空记录或改变能量参考。

这项修正针对已复现的流体边界等待；明确禁止转换的群系/开关不被改写成允许转换。测试和文档需说明这项范围。

## Validation

1. 将原休眠刻画场景改为真实回归：无记录的内水仍遵守边界；半程潜热不可提交；完成H的旧记录应及时变块、H原样交接、继续降温并最终释放。
2. 修正前先保存该回归的真实Forge失败。
3. 用真实燃料源激活材料，分别在细档和实际粗档验证有限相变控制、主线程ACK、开关暂停/恢复及H没有重复扣除。
4. 重跑材料/存档、河道扩张和红外/余温回归，完成当前生产打包。

## Outcome

- 控制器已区分自然启动的液体边界与实际完成H的提交；补充休眠完成度，保留其他明确世界规则及原H/存档格式。
- 旧完成记录v6读回、自然正反例、半程拒绝、H原样交接、后续降温/冰阶段/释放均通过；在线细档和实际粗档的开关暂停/恢复及ACK通过。
- 最终五个分组共25项真实Forge检查通过，均错误0、关闭root0。河道180秒正常20TPS回归仍通过。验证和中间失败见[摘要](../docs/climate/validation/thermal-completed-phase-2026-11-06.json)。
- 用户明确取消后续build，排队打包已停止，没有新生产JAR。现行行为由[余温与提交说明](../docs/climate/thermal-residual-lifecycle.md)维护，完成记录见[日记](../diary/2026-11-06_22-05-37_completed-water-phase-commit.md)。
