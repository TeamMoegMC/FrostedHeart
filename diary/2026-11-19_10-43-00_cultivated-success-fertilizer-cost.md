# 栽培树成功结算肥料，与旧树四格口径对齐

- 作者：OpenAI Codex
- 状态：实现、编译和专项回归完成，未提交、未推送。

## 决策与依据

旧 SaplingGrowTreeEvent 在生成放行时于生成原点扣4格，原版巨型也是一个事件，而非四根各扣一次。按用户指定的成功生长口径，新树在 TreeFeature 成功后恢复根部土壤，再由 CultivatedTreeFertilizer.consumeAfterSuccess 一次扣4格；不足4格耗尽转泥土。单棵在自身根部，巨型仅西北生成原点，其他三根不扣。旧事件扣费注释保持，普通作物扣费未改。

肥料储量s充足时 E[消耗|成功]=4，有限存量为min(4,s)；未施肥土壤0。生长、等待和失败0；加速不改变费用。原逐随机刻扣1在理想全程供肥时，无加速及三档加速耗16/8/4/2格，新规则统一4；逐刻1/4概率扣1会变4/2/1/0.5，不能对齐，故选择成功确定结算。

## 验证与场景修复

前两轮测试失败于地下模板遮光及提高坐标后仍遇自然地形，不能算通过。新增 CultivatedTestEnvironment 共用晴天正午露天场景，位于maxBuildHeight-48并清空20×20上方区域；保留生产光照门槛和真实生成检查。

compileJava、compileGameTestJava、runGameTestServer成功，30项必需测试全部通过，BUILD SUCCESSFUL in 3m 4s。日志build/cultivated-fertilizer-open-sky-validation.log。两项新增测试实际遍历三类三档肥料生成、成熟前不扣费、失败重试免费、余量3耗尽；现有巨型测试检查仅西北根扣4。git diff --check通过。未做本轮客户端视觉验收或普通作物独立专项。

## 文档与后续

同步docs/climate/cultivated-oak.md、cultivated-spruce.md、crop-fertilization.md。温度计此前修改保留。另完成原版生成器调查，记录plans/2026-11-19_10-42-00_cultivated-native-tree-redesign.md；原版完整树形与逐棵精确产量配额冲突仍待用户选择，尚未改动树形生产代码，不把本次测试当作新树形验收。
