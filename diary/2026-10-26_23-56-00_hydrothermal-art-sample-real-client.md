# 热液矿床固定首样与真实客户端画面

- Time: `2026-10-26 23:56:00 +08:00`
- Author: `OpenAI Codex / GPT-6，原型实现与实机验证`
- Status: `partial`（固定首样与实机取证完成；顶部美术尚未通过）
- Scope: 伴随包terrain_proof/client测试Java、preview.gradle、run_visual_validation.py、FH的opt-in DepositClientValidation与相关文档；未迁移生产

## Completed

- 实现固定锚点布局、局部NOISE密度/含水层字段、匹配测试群系与普通材质Feature；主壁、洞顶、水下矿化使用相同三维矿化面。
- 只使用已有岩石、矿石、普通水和少量藤蔓/地衣；无新方块、矿床tick或热量注入。
- 三次完整客户端运行分别检查初样、地板/曲率修正版和最终插值接线；保存七个最终相机，包括普通光照与水下。
- 首样地面过平、壁面过直，已复用XZ场增加起伏和曲率。最终只插值新洞腔，保留样板外原final_density直接调用。
- [完整结果与PNG](../../TheWinterRescue/Scripts/worldgen/deposits/terrain_proof/results/2026-10-26-art-v1/REPORT.md)。

## Decisions

- 本次明确为固定地点的形状样本。房间中心、尺寸和旋转固定，布局seed显式提供，尚未自动选址。
- 数据仅在隔离游戏目录覆写，测试群系内部以布局内容替代原装饰列表；未宣称已接回全部旧小feature或完成资源量平衡。
- 用户观看后指出穹顶缺乏特色。该判断成立，后续应先审核断裂/错动顶板等轮廓方案，不能依靠加密白色矿脉冒充形态变化。

## Validation

- Java 17下独立preview JAR编译、reobf成功；普通生产JAR中检查未包含10个测试类。
- 原生NoiseChunk/fillFromNoise：中心净空23格；区块(0,0)全高度98,304方块与原NOISE路径差异0。
- 真实安装环境61个mod，FH替换成工作区测试JAR；MC1.20.1，NeoForge47.1.106/FML47.2.2。最终16×16 FULL范围加生成halo，客户端正常保存退出。
- 只读最终存档：检查711,349位置全部为样板quart群系；洞腔1109/1109 quart抽查一致。四个房间空气探针同一连通分量。
- 主池抽查水深4格；三个独立水体为668/293/221格。四柱顶连续实心14/18/16/16格。详见原始JSON；不推广为全域保证。
- 拍摄关闭随机刻、天气、生物等，未做600秒水文或生产热行为验收；未把一次60秒请求总耗时当成正式性能测试。
- Documentation impact: 更新world-generation.md区分独立样板与生产；设计稿记录首样结果与顶部未通过，原型报告明确测试适配和待验收范围。

## Remaining

- 顶部新构型审核、旧小feature完整调度、资源预算、多种子/方向、角色走路、600秒水文、P95/分配量/普通区域性能和自动分布。
- 最终运行目录：`D:/Frosted-Heart/build/hydrothermal-art-preview-final-20261026`；存档`deposit-inspection-1793029361306`。已生成关闭自动拍摄驱动的手动启动入口，未另外启动一次人工游玩。
