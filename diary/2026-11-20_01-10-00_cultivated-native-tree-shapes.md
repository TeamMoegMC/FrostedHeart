# 栽培树完整原版结构与产量期望校准

- 作者：OpenAI Codex
- 状态：生产及测试源码编译成功，最终专项36/36通过，文档和图表同步；未做客户端目视验收，未提交、未推送。

## 用户已确认的取舍

保留原版完整树形，允许木头数围绕期望波动；高度可以超过原版，不人为封顶。此前逐棵精确配额与原版分叉冲突，以完整树形优先。2×2每株多2块保留为期望，不再补平顶部。此前温度计和成功生成扣4格修改保留，未扩大到普通作物。

## 实现

CultivatedNativeTrunkPlacer只固定尺寸，实时委托原版生成器，删除候选收集后截断、原点补柱和树冠过滤。CultivatedOakBlockEntity删除人工短横杈，CultivatedGiantTree删除三根短柱补顶。Profile恢复普通／Fancy橡木、长冠／短冠巨型云杉、高矮红树及花卉蜂巢来源，完整保留根、装饰、树冠与空间规则。默认橡树JSON改用minecraft:oak，旧内嵌配置仍兼容。

CultivatedTreeYield依据本地1.20.1原版生成器抽样反算尺寸，保留整数台阶，只需少量二分与插值。7类分叉结构共67个均值约536字节；运行时不反复预生成或建立大表。32以上继续线性外推，大型橡树用连续二次外推；没有几何高度封顶，仍受Minecraft世界建造高度约束。主干加全部枝杈计木头期望，旧0.6/0.15人工短枝费用不再使用。樱花最低完整分叉期望约7.71，最低预算8，低于它保留满阶段并继续攒信用，避免捏成直杆；其他单生物种最低仍1。

## 验证过程与结果

前两轮抽样失败在Forge要求的LevelReader/LevelSimulatedReader组合接口；测试场景修正为同时实现两接口的活内存场景，不改生产代码适应测试。初步估计系数产量偏差大且红树根允许拒绝个别随机种子，改用原版实测均值与真实随机刻重试。首次校准后33/33通过，再补实际高树、根／装饰来源和樱花信用等待，最终36/36通过。

最终命令：bash gradlew --offline compileJava compileGameTestJava runGameTestServer -I Scripts/cultivated-oak-validation.init.gradle -PgameTestNamespaces=frostedheart_cultivated_oak -PgameTestDirectory=build/cultivated-oak-gametest。日志build/cultivated-native-final-validation.log，BUILD SUCCESSFUL in 3m 12s。

13原版配置×7尺寸×32种子=2912组逐块写入、轴向和全部树冠附件一致。12配置×35尺寸×128种子输出原版参考（包含48/64/96格）。75组预算×512独立种子输出期望校准，最大相对偏差6.69%（目标256的大型橡木，实测均值238.87），通过max(0.5,8%)容差。真实世界64格云杉生成通过，不用仅调用原版函数代替高树整树验证。所有既有温度、进度、温度计、存档、放置、2×2、肥料专项保留通过。

## 文档和产物

新增docs/climate/cultivated-tree-shapes.md为树形和模型权威说明，同步cultivated-oak/spruce/species。docs/climate/validation两份CSV记录参考及独立验证。Scripts新增共用Python渲染／换算与橡木绘图入口，更新云杉入口；两张PNG已重画并查看，保持按肥料类型分图、无肥基线和三档实线。Python处理原版Fancy相邻尺寸木头数相同的零分母台阶，与Java一致。

计划plans/2026-11-19_10-42-00_cultivated-native-tree-redesign.md已完成。服务端结构对照和均值测试不是客户端目视验收；数据包改动主干分叉参数需重校准，不承诺旧模型适用所有自定义配置。

## 本轮树形重写文件清单

- `src/main/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedOakProfile.java`
- `src/main/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedOakBlockEntity.java`
- `src/main/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedGiantTree.java`
- `src/main/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedNativeTrunkPlacer.java`
- `src/main/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedTreeYield.java`
- `src/main/resources/data/frostedheart/cultivated_trees/oak.json`
- `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedGrowthGameTests.java`
- `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedOakGameTests.java`
- `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedSpeciesGameTests.java`
- `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedSpruceGameTests.java`
- `src/gametest/java/com/teammoeg/frostedheart/content/agriculture/tree/CultivatedNativeShapeGameTests.java`
- `Scripts/cultivated_tree_plot.py`
- `Scripts/plot-cultivated-oak-yield.py`
- `Scripts/plot-cultivated-spruce-yield.py`
- `docs/climate/cultivated-oak.md`
- `docs/climate/cultivated-spruce.md`
- `docs/climate/cultivated-species.md`
- `docs/climate/cultivated-tree-shapes.md`
- `docs/climate/validation/native-tree-yield-reference.csv`
- `docs/climate/validation/cultivated-native-yield-validation.csv`
- `docs/climate/images/cultivated-oak-yield-vs-temperature.png`
- `docs/climate/images/cultivated-spruce-yield-vs-temperature.png`
- `plans/2026-11-19_10-42-00_cultivated-native-tree-redesign.md`
- `diary/2026-11-20_01-10-00_cultivated-native-tree-shapes.md`
