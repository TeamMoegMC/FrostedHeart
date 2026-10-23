# 冰锥半透明交叉面排序修复

- Time: `2026-10-21 00:44:03 +08:00`
- Author: `OpenAI Codex；实现与验证代理`
- Status: `completed`
- Scope: `template_icicle_cross.json`、五份 `icicle_*` 模型父引用、冰锥当前说明

## Completed

- 针对用户截图中固定方向面片随观察角度消失的问题，新增共用父模型，将原版两整片交叉面沿中心交线拆为四个双面半片。
- 五种冰锥外观引用新父模型；保持外缘尺寸、45° 旋转与 rescale、正反面 UV、现有半透明贴图和渲染层。
- 更新[冰锥当前说明](../docs/climate/icicle.md)的模型结构、原因与验证边界。

## Decisions

- 本地 1.20.1 `BufferBuilder` 按四边形中心生成排序点；原模型的相交整片中心完全重合。`rendertype_translucent.fsh` 不丢弃 alpha=0 像素，透明层写深度，前面的整片可能遮掉后画的另一个方向。这与用户报告及几何复现吻合。
- 沿交线拆分后，四个半片的排序中心不同，可分别按远近绘制；采用普通模型 JSON，单个模型由 4 增至 8 个单向四边形，不加入逐帧自定义逻辑、特殊渲染器或全局渲染修改。
- 本轮不编辑 Java 或 PNG，没有编译 Java、重新打包 JAR、暂存或提交。之前尚未提交的碰撞与粒子修改完整保留。

## Validation

- 核对五种外观均引用新父模型并保留 `minecraft:translucent`；正反面 UV 在各半片多个采样点与旧模型一致，拆分前后无纹理镜像或缩放变化。
- 几何复现按面中心距排序并模拟写深度，覆盖 720 个水平角度 × 4 种距离 × 255 条射线：旧模型 249704 条相交射线中 123775 条出现后画的远面被遮掉；新模型 246824 条非边界相交射线中为 0。模型交线上的退化射线不计入新模型的该项统计；此检查不是游戏 GPU 渲染截图验证。
- `gradlew.bat processResources --offline --no-daemon --max-workers=2 --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：`BUILD SUCCESSFUL in 30s`。新父模型及五个子模型已输出到开发运行资源，日志 `build/icicle-translucent-sorting-resources.log`。仅有既有无关资源重复警告，未改构建配置。
- `git diff --check` 通过。

## Remaining

- 当前开发客户端按 F3+T 重载后，从原问题视角及四周观察确认；尚未进行实际客户端视觉验收。与水、玻璃等其他半透明物体交叠仍受原版排序规则限制。
