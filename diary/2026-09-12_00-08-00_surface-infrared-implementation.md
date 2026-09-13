# 方块表面红外接线及纯色显示回退

- Time: `2026-09-12 00:08:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial`
- Scope: `普通terrain方块表面红外；代码与自动测试完成，修正版实机验收未完成`

## Completed

- 接入共享`BlockBrickLayout.surfaceNodeMask`与material-only`PagePublication.Brick.firstSlot`；保留Air/source/center语义。发布只比较符合表面资格的温度。
- `BrickMigrationKernel`不再用Air checkpoint恢复材料/有材料热容的mixed；原存档格式及普通玩法fallback保留。
- 重写`MinecraftThermalInput.InfraredCapture`，移除analytic与dormant Air显示；保留局部可读Page、代次、增量与有界重试，独立发送9³自然背景。移除无调用者的dormant红外缓存。
- 复用原材料CPU/GPU镜像与Page上传；增加1,458字节背景网格，修正18字节行的unpack alignment=2。分包只在LAST提交，控制/背景不提交材料基线。
- 移动原回调至AFTER_CUTOUT_BLOCKS；不改Embeddium顶点、不增加世界绘制。后侧深度取样拆分相机整数/小数偏移，避免近表面float舍入把样点推回错误侧。
- 用户截图显示圈内纯色覆盖导致地形细节消失。已恢复原0.43热色叠加，并撤回计划中的纯色最终RGB要求；表面取值、自然背景与完整扫描覆盖仍保留。
- Documentation impact: 更新世界温度、runtime、网络生命周期三个living docs及[执行plan](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)。未编辑design，未更改伴生整合包。保留开始前工作区的其他改动。

## Decisions

- 黑体近似简化的是温度取值，不要求剥夺游戏画面的贴图细节。最终显示恢复用户要求的原overlay观感；混合前heatColor仍由表面/背景温度决定。
- 自然背景只是缺材料处的热平衡估计。现有材料仍是单块有效表层平均温度，未实现六面独立温度、固定体积热容、固体导热或材料持久化。
- GameTest通过不能证明GPU显示正确。客户端控制被用户Esc终止后停止界面操作；随后根据用户截图修复着色，不冒充已经看过修正版画面。

## Validation

- Java17 `compileJava`、`compileGameTestJava`通过。第一次完整测试54/56通过；修正测试场景的相邻Air驻留和未加载背景预期后56/56通过。
- 最终完整`runGameTestServer --offline --no-daemon --console=plain`：57/57全部通过，包括纯Air不推进IR epoch、仅材料变化推进准确Brick、营火材料/空气分离、局部Page缺失/恢复、runtime代次、729 Page分包边界与背景XYZ、144组深度数值用例。日志：`run-gametest/surface-infrared-verification.log`。
- 144组深度样例覆盖六面、1/4/16/64 blocks、正视/斜视、负坐标与接近三千万坐标；最大后移0.0156234214 block。普通坐标合并方式原有3/48个近表面反向面错误，拆分相机偏移后通过。
- 真营火场景100次稳定delta：capture中位0.0266 ms、p95 0.046 ms、零响应。100次full且背景键命中：中位0.0387 ms、p95 0.1676 ms、24,400 S2C字节。串行相同窗口样本，不等于100玩家在线负载，不含受控GPU/堆分配对比。
- ForgeGradle联网预检在离线构建中卡住；后续命令加入`-Dnet.minecraftforge.gradle.check.certs=false`跳过该联网预检，未改变项目配置。
- 恢复0.43叠加后`processResources`通过，确认`build/resources/main/assets/frostedheart/shaders/infrared_view.fsh`含新混合表达式。客户端可F3+T热重载；没有为这一shader表达式改动重跑未受影响的服务端测试。
- `git diff --check`通过。客户端启动日志有红外shader资源加载成功，但不能以此证明FBO、取样或实际画面正确。

## Remaining

- 修正版实机画面与后续绘制遮挡、原版/Embeddium图形模式、resize/第三人称、分包提交视觉验收。
- 受控CPU/GPU与真实多人分配/网络性能对照。因此本阶段plan仍为in-progress，不能宣称“已测得最佳性能”或完整图像验收完成。
