# 栽培橡树温区与产量高度参照

- 作者：OpenAI Codex
- 时间：2026-11-19 05:26 UTC
- 状态：部分完成，尚非全部需求交付。

实现 ideal_min/ideal_max（15/25）、能长区间满速、两侧各一个能长区间宽度的停长带、带外 0.125 倍速与零产量。高度以 min_height/max_height 为产量参照，不再截断，最低一格。原工作区温度计、short p/v、迁移和 Forge 随机刻入口改动保留。

验证：compileJava 与 compileGameTestJava 成功。串行专用 Forge GameTest 7/7 通过；git diff --check 通过。首轮服务器与后续编译重叠，发生 AITownManager 缺类退出，不计为有效基线。未运行客户端。未提交或推送。

文档：同步 docs/climate/cultivated-oak.md 温区、字段与高度公式。当前仅支持直干橡树，枝杈随机预算、概率配置、0.6/0.15 常量与 2×2 巨型组合机制尚未实现，须继续扩展树形配置与生成流程。
