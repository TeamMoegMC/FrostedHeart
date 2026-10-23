# 独立小冰晶像素稿、白冰配色与20%透明

- Time: `2026-10-22 22:50:51 +08:00`
- Author: `OpenAI Codex，主代理逐像素绘制和部署；GPT-6.1 Sol 子代理只读复核模型/渲染约定`
- Status: `completed`
- Scope: 大小冰晶纹理/模型、可重建像素稿、当前文档、Prism JAR

## Completed

- 按用户明确授权逐像素绘制：新增 `small_ice_crystal.png`，16×16画布底部独立三尖短晶簇，高7个原生像素、39个可见像素。大图保留原版15像素高轮廓、122个可见像素，重配五色偏白冷灰。
- 两者使用同样的像素—世界坐标映射，小图不再是大图或大模型的缩小版本。模型可见高度小7/16、大15/16格，原碰撞、亮度1/3、附着及生成规则保持。
- 可见像素统一Alpha204，即80%不透明、20%透明，空白Alpha0。模型 `minecraft:translucent`，不额外乘顶点alpha，以免产生重复透明度及渲染兼容路径差异。
- 按各自轮廓裁剪交叉面并在交线处分半，保持四朝向UV。大60面、小40面；没有额外运行期渲染器。
- [像素稿脚本](../Scripts/ice_crystal_assets.py)包含两张原生ASCII像素稿、明确RGB调色板及模型离线生成/检查；Pillow可重建资源。
- 文档影响：更新[当前实现](../docs/worldgen/ice-caves.md)，保存[纹理及模型统计](../docs/worldgen/validation/ice-crystal-pale-2026-10-22/texture-and-model.json)与[客户端对照图](../docs/worldgen/images/ice-crystal-pale-2026-10-22/seed-111-crystal_model_fixture.png)。

## Decisions

- 最初尝试内置imagegen时被账号组403拒绝，未产生素材；随后用户明确允许Minecraft像素画逐像素绘制，最终使用确定性像素稿完成，未切换API服务。
- 五色为 `(190,201,209)`、`(210,219,224)`、`(229,234,238)`、`(242,245,247)`、`(252,253,254)`，大幅减少原图蓝色差值，保留浅冷色层次。
- 透明度是单个贴图可见像素的alpha；多个有色半透明面叠加仍服从引擎规则，不声称物体所有视角都恰好透过20%背景。

## Validation

- 原生PNG实测alpha均仅0/204。两个模型各1024个纹素—朝向中心样本通过，独立子代理直接读取最终文件重复核对：可见像素恰覆盖一次、空白零覆盖、UV正确，所有面止于交叉轴一侧。
- 实际客户端正常预设seed111与模型对照台完成截图并正常退出，`build/ice-crystal-pale-client.log`成功5分18秒。观察淡色、独立短晶簇、大小差异及邻冰交叠；所观察区域没有先前的明显黑三角。不是对全部视角/着色器的通用排序保证。
- 资源修改没有重跑矿物或其他玩法测试；临时采样资产已cleanup。
- `jar/reobfJar`成功59秒，打包一次，`build/ice-crystal-pale-jar.log`。JAR中PNG与源文件逐字节一致、两种模型均为新半透明裁剪模型。

## Deployment

- 已更新 `D:/games/minecraft/prismlauncher/instances/The-Winter-Rescue-Development/.minecraft/mods/frostedheart-1.20.1-0.7.7.jar`，13790501字节。
- 旧JAR备份 `build/prism-backups/2026-10-22_22-50-51/`；记录 `build/ice-crystal-pale-deployment-result.json`，复制后逐字节一致。
- FH仅提交本轮资源、脚本、文档和图片，未push；TWR及实例的pack提交仍为 `f7c868e5`，原配置修改和其他任务工作保留。

## Remaining

- 重启实例即可在已有冰晶上观察新外观；用户最终美术验收开放。
