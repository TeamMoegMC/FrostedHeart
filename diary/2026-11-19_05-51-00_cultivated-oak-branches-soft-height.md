# 栽培橡木枝杈与高度软饱和

- 作者：OpenAI Codex
- 时间：2026-11-19 05:51 UTC
- 状态：本轮两项实现及专项服务端验证完成。

## 改动与取舍

CultivatedOakProfile.earnedHeight 保留边界和理想温度高度参照，超过 max_height 后使用指数衰减：M+M*(1-exp(-(L-M)/M))，最后向下取整，趋近 2M。默认产量系数 1/2 的预算为 12/20，极高产量趋近 24，避免线性飙高；零产量与不足一格仍等待。

CultivatedOakBlockEntity 生成随机方向 1～3 格橡木横向枝杈，放在主干顶端下方两格并使用原有树冠。超过原版 trunk_placer 最高高度（默认 6）必出枝杈，优先于概率及平均高度门槛；其他满足平均高度门槛的情况使用 growth.branch_probability（默认 0.35）。每格消耗 SMALL_BRANCH_COST=0.6，主干不得扣到 min_height 以下；GIANT_BRANCH_COST=0.15 独立保留但本轮没有实现巨树。枝杈不覆盖障碍或流体，不读取未加载枝杈位置；强制枝杈无处放时等待空间。枝杈方向、数量不存档。原有恒速、满进度等信用、零产量不生成、short p/v、根部肥料与失败恢复逻辑保留。

## 验证

串行 compileJava、compileGameTestJava、runGameTestServer 成功，专项 frostedheart_cultivated_oak 8/8 全部通过，BUILD SUCCESSFUL（3m47s 为构建耗时，不是游戏生长时间）。日志 build/cultivated-oak-branches-regression.log。测试覆盖曲线单调/收益递减/饱和、实际分枝及预算守恒，以及既有进度、信用等待、温度、肥料、存档、网络状态和空间失败重试。git diff --check 通过。

文档同步 docs/climate/cultivated-oak.md 的配置、公式、枝杈位置及预算和当前限制。未运行客户端视觉验收；其他树种和 2×2 巨型树尚未实现。未提交、未推送。
