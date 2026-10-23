# 冰锥碰撞缩细、开裂冰屑内移与半透明

- Time: `2026-10-21 00:31:25 +08:00`
- Author: `OpenAI Codex；实现与验证代理`
- Status: `completed`
- Scope: `IcicleBlock`、五份 `icicle_*` 方块模型、冰锥当前规则文档

## Completed

- 在 `IcicleBlock.getCollisionShape` 返回独立的预创建碰撞框；按贴图非透明行最窄/最宽的平均值，将尖端、尾段、中段、根部的宽深分别设为 3/16、5/16、7/16、8/16 格。尖端仍为上半格高，`getShape` 保留既有选中框。
- 开裂 `FALLING_DUST` 的本地 x/z 从 0.06/0.94 内移至 0.14/0.86，保留生成频率、高度和正常地形碰撞。
- 五套冰锥模型从 `minecraft:cutout` 改为 `minecraft:translucent`，使用已有位图的半透明信息，无新增纹理或面片。
- 文档影响：更新[冰锥当前说明](../docs/climate/icicle.md)的外观、选中/碰撞契约、粒子边界及验证记录。

## Decisions

- 读取实际纹理确认宽度范围：尖端 2–4、尾段 4–6、两套中段 6–8、根部 6–10 像素。选择固定方形碰撞柱，不在查询时扫描纹理或动态创建形状。
- 纹理可见像素已有 alpha=162/170/179/185，接近普通冰的 alpha=190，启用混合渲染即可，不编辑 PNG 或额外降低 alpha。
- 原版 `FALLING_DUST` 碰撞宽度为 0.2 格。粒子中心 0.14/0.86 加上半宽后，与最宽根部新碰撞范围 0.25..0.75 仍相隔约 0.01 格，避免只检查中心而让冰屑仍被下段阻挡。
- 本轮只调整洞穴冰锥的静止碰撞和模型；不改房檐冰溜子或原版坠落实体包围盒。温度系统无改动，没有暂存、提交或重新打包 JAR。

## Validation

- Java 17：`gradlew.bat compileJava --offline --no-daemon --max-workers=2 --console=plain -Dnet.minecraftforge.gradle.check.certs=false` 通过，`BUILD SUCCESSFUL in 1m 45s`；命令级设置有效 CPU=4、Gradle JVM=1 GiB，没有修改构建配置。日志：`build/icicle-collision-translucency-compile.log`。
- 资源核对通过：五份模型 JSON、逐图 alpha 和非透明行宽度、新碰撞宽度与平均值一致、粒子完整包围盒避开四套新碰撞框。
- 核对 Forge 47.3.0 的 `FallingBlockRenderer`：坠落渲染使用模型声明的渲染层，继续复用同一份半透明模型。
- `git diff --check` 通过。本轮没有新增自动测试或重跑完整 GameTest；上一轮 130/130 是 2026-10-20 的验证结果。

## Remaining

- 客户端确认半透明交叉面的实际观感、变细后的碰撞手感及内移冰屑；本轮未启动客户端，编译与资源检查不代表已完成视觉验收。
