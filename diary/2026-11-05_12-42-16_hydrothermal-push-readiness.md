# 热液景观测试与推送就绪核对

- Time: `2026-11-05 12:42:16 +08:00`
- Author: `OpenAI Codex / GPT-6，实现清理与推送范围复核`
- Status: `partial`
- Scope: 最新岩脊固定样板、验证工具、结果忽略规则和文档；没有提交或推送

## Completed

- 完成缺失的61模组基线客户端，与岩脊版本做同种子、同六机位对照；原矿石/小feature规则相同。
- 最终存档证明实体轮廓变化，但大面积白色表面及胶结辨识未完成美术验收。
- 撤回的矿石搬移、藤蔓过滤、旧预览类型/Source包装均不在清理后的测试JAR中。预览核心四个源码文件合计527行。
- 限定忽略本地results目录，约178MiB的截图/日志/结果不进入源码提交，本地记录保留。
- 检查新增Mixin为0；现有NoiseBasedChunkGeneratorMixin内容与HEAD一致，只有换行格式状态。

## Decisions

- 不能把固定锚点实验当成正式生成更新推送。普通JAR尚未包括新岩脊，旧Deposit路径仍独立存在。
- 不提交无关研究UI、城镇资源、测试删除和其他群系改动；未全量add/commit/push。

## Validation

- 清理后Java/reobf、Python语法通过。
- [实机报告、性能与存档证据](../../TheWinterRescue/Scripts/worldgen/deposits/terrain_proof/results/2026-11-04-solid-ridge/REPORT.md)。
- 核心NOISE中位数28.725/28.500ms；不能据此宣称完整最佳性能。
- Documentation impact: world-generation.md明确当前实验核心、已撤回入口及正式推送未就绪。

## Remaining

- 将认可的地貌方案接入普通源码与正常数据、统一旧实现；目前仍是固定样板。
- 视觉效果待完成；没有把“多了一块实体”认定为所有热液特色已完成。
- 生产发布前应提供单一、精简且可审阅的变更范围，目前未推送。
