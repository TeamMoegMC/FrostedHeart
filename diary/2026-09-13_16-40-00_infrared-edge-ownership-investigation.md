# 红外边缘归属修复候选与严格GPU验证

- Time: `2026-09-13 16:40:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial`
- Scope: `infrared_view.fsh、AFTER_LEVEL挂点、独立GPU边缘验证`

## Completed

- 按用户要求保留已恢复的`AFTER_LEVEL`。最终世界画面中的实体等可被空间显示值染色，但没有实体体温模型。
- shader保留一张显示温度纹理、原色标、0.43混合、解析场数据和蓝色缺值占位。切线以深度连续性选择邻点；同平面快路径只读第一圈，两个方向都不连续时才合计最多9次深度读取。温度只读一次，没有新增CPU方块扫描或常驻缓冲。
- 整数面法线一致时改用像素射线/平面交点后小幅内移，齐次形式保留view矩阵平移；不可靠邻点仅在位置余量内选最近整数面。其他面保留法线内移。边缘侧移限于1/256邻点向量，是显示近似，不是精确误差上界。
- 撤回失败的候选平面打分、固定深度阈值和扩大偏移实验；这些实验有的改善拐角却破坏平面或串到相邻材料。未增加温度邻域补洞。
- `InfraredRasterValidation`新增真实完整块与楼梯的独立材料标识场景。楼梯两个盒子共用本块标识；仅测试夹具携带标识，不改变生产顶点。分类区分缺值蓝色、错误材料红色和正确白色。
- Documentation impact: 更新[世界温度](../docs/climate/world-climate-and-temperature.md)、[runtime](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[显示plan](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)和[材料主plan](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)，明确当前候选和未完成状态。既有日记不改写。

## Decisions

- 不把深度恢复方块归属与读取材料温度混为一谈；仍是一块一个材料温度，未引入每面或气隙温度。
- 保留严格断言，不排除轮廓像素、不以比例小宣称修好。有些像素的理想视线已落到光栅化面外，但不能把所有剩余错误都归咎于这个现象。
- 在低成本后处理保留误差与直接提供归属的管线改动之间询问用户最终验收取舍。未实施归属缓冲、Embeddium适配或新的世界绘制。

## Validation

- Java17：`gradlew.bat compileJava processResources compileGameTestJava --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，日志`build/infrared-edge-compile.log`。
- RTX4070 Laptop隐藏GL上下文；生产shader、生产相同float逆VP、24-bit/32F depth：1,440平面场景错误侧0；720完整块场景缺值310、错误材料350；720楼梯场景缺值858、错误材料1,040。GL error均0，但完整严格测试仍以AssertionError退出。
- 日志`build/infrared-raster-validation.log`；诊断图片`build/infrared-edge-diagnostic.png`、`build/infrared-stairs-diagnostic.png`。入口仍是现有classpath运行Java源文件，不加依赖，不注册到Forge服务端。
- `git diff --check`通过。未操作用户游戏或重新运行无关服务端热模型测试。本轮没有GPU帧时对比，减少纹理读取不等于已经实测最佳性能。

## Remaining

- 复杂几何的严格归属测试未通过，当前源码只是修复候选，不是完成交付；需根据用户验收取舍继续。
- 实际游戏视觉验收、透明/跨块/特殊模型的归属和大型世界性能仍未完成。
