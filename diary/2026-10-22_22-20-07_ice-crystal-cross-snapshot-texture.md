# 官方冰晶纹理与十字交叉模型

- Time: `2026-10-22 22:20:07 +08:00`
- Author: `OpenAI Codex，主代理`
- Status: `completed`
- Scope: FH 大小冰晶模型、纹理、当前文档、Prism JAR

## Completed

- 用户明确要求紫水晶簇式十字交叉模型，并使用原版最新测试版冰晶材质。
- 查询官方版本清单确认当时最新为 `26.4-snapshot-5`，下载其官方客户端JAR，原样提取 `assets/minecraft/textures/block/ice_crystal.png`。复制至 `src/main/resources/assets/frostedheart/textures/block/ice_crystal.png`，未改色、重绘或缩放PNG。
- 大小模型改为两片双面交叉面，兼容1.20.1 `minecraft:block/cross`，使用 `minecraft:cutout`。同一原始纹理以几何缩放形成7/16和14/16格可见高度；亮度1/3、碰撞、附着和生成规则未改。
- 文档影响：更新[当前实现与来源](../docs/worldgen/ice-caves.md)，旧立体晶柱截图明确作为历史；新增[来源记录](../docs/worldgen/validation/ice-crystal-cross-2026-10-22/source-and-model.json)及[实际对照图](../docs/worldgen/images/ice-crystal-cross-2026-10-22/seed-111-crystal_model_fixture.png)。

## Decisions

- 原PNG为16×16，alpha仅0/255，采用镂空渲染保持原版轮廓。使用本项目1.20.1的cross结构，未照搬新快照独有渲染字段。
- 保留完整UV，在附着点缩放几何；无需新增Java代码或测试复制模型实现。
- TWR生成数据和用户其他改动均未动；本次只提交FH相关资源、文档与验证画面，未push。

## Validation

- 原始提取文件与项目PNG逐字节一致；两模型均两个element、四个面、完整UV，缩放旋转后位于单格范围。
- 真实正常预设seed111客户端运行完成、保存洞穴和人工对照台截图并正常退出，`build/ice-crystal-cross-client.log`成功7分10秒。查看新十字轮廓、大小辨识及自然附着；没有新冰晶资源缺失。
- 上轮已存在的小晶体远区块候选检查诊断仍在日志中（旧225条，本轮247条）；本次只修改模型资源，没有将这类诊断归为新渲染回归。该生成边界检查的日志噪声另行处理。
- 采样临时文件已cleanup；`jar/reobfJar`成功55秒，打包一次。`build/ice-crystal-cross-jar.log`。
- 新JAR为13788856字节，已包含官方PNG和两个cutout模型，部署复制后与产物逐字节一致。

## Deployment

- 已更新 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Development/.minecraft/mods/frostedheart-1.20.1-0.7.7.jar`。
- 旧JAR备份 `build/prism-backups/2026-10-22_22-20-07/`，部署记录 `build/ice-crystal-cross-deployment-result.json`。
- 实例TWR提交仍为 `f7c868e5`；原client/common配置改动保留。

## Remaining

- 重启实例即可在已有冰晶上看到新外观；最终美术效果由用户验收，无需为此次资源修改生成新地形。
