# 热液密度原型的完整客户端实际预览

- Time: `2026-10-26 17:59:20 +08:00`
- Author: `OpenAI Codex / GPT-6，客户端与存档验证`
- Status: `partial`（实机演示完成，最终矿床/全类型验收未完成）
- Scope: 独立测试JAR、测试客户端目录、gameTest相机驱动；生产代码/数据、已安装JAR及用户原存档不变

## Completed

- 实际运行安装的MC1.20.1/NeoForge47.1.106、61模组和当前包数据，新建用户种子世界。
- 正常注册测试密度和包络BiomeSource，固定(-1335,-10,-269)核心，后续原热液feature与原含水层生效；测试目录关闭旧主体后期开挖。
- 单独编译reobf deposit-density-preview JAR。密度原型改为FH preview包名，避免向Minecraft模块添加同包类；独立原生proof相应更新import。
- 两轮完成七张实际framebuffer截图。324FULL区块采样，核心quart6327/6327热液；只读保存数据411621/411621核心方块位置热液，空气最大分量316932，占99.9984%。
- 实际中央空气高度30，仍未达到40目标。素材配比和层带自然程度未通过最终视觉设计判断。

## Decisions

- 通用的是布局/归属/密度接线，胶囊开挖不能直接复用为整座火山；火山需另有正密度山体与负密度火口构型。
- 本轮是固定锚点演示，不声称已实现随机区域、全矿床尺寸、普通区缓存适配或最佳性能。
- 不使用fill/setblock/fillbiome改地形；允许普通流体和热系统变化，拍摄仅调整玩家模式、夜视、天气等。
- 高视距补拍与最后overview未产生完成截图，未纳入成功结果；首轮部分远景白边有视距限制，保留原图不改图。

## Validation

- preview.gradle独立JAR构建成功，运行中正式Codec注册/数据加载及已生成世界重新载入成功。
- 初次JAR漏相机驱动，修正为绝对source路径并重建；未使用未完成启动作为证据。
- close-inspection和native-aquifer均CAPTURE COMPLETE；实际打开检查全部七图。
- 只读Anvil检查与Python py_compile完成；普通生产JAR不含preview类，安装JAR保持原15,269,028字节。
- [实机报告](../../TheWinterRescue/Scripts/worldgen/deposits/terrain_proof/results/2026-10-26-client/REPORT.md)。

## Remaining

- 水文、自然形态与资源密度需要继续设计，固定核心未做到最终40格空气净高。
- 火山、其他矿床、动态布局与全系统性能未实现或未验证。
- Documentation impact: 新增实机报告；living docs当前生产行为不变。未运行完的补拍不写成成功。
