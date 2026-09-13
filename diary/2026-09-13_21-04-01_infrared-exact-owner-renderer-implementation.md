# 红外真实方块归属、原地形捕获与直接混合

- Time: `2026-09-13 21:04:01 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `本轮红外生产代码替换与有限自动化验收；完整负载/场景矩阵仍留在plan`

## Completed

- [InfraredViewRenderer](../src/main/java/com/teammoeg/frostedheart/content/climate/render/InfraredViewRenderer.java)使用原Forge AFTER_LEVEL入口直接混合；新增[infrared功能包](../src/main/java/com/teammoeg/frostedheart/content/climate/render/infrared)，实现真实方块scope、自有20/32-byte格式、原renderer/ShaderInterface子类、R16I温度图与借用附件FBO。
- 继续调用Embeddium原mesh/culling/sorting/multiDraw。生产桥接为4个小类、6个明确位置；无全局ShaderLoader补丁、原encoder修改、原格式常量修改或额外terrain draw。
- 删除深度/法线/整数面猜块、RGBA8中转及复制路径。保留-20..20℃色标、0.43混合、蓝色缺值、原扫描、材料数据与解析场合成；本轮未改服务端热行为。
- 增加可选`runClient -PinfraredValidation`，在独立隐藏测试世界验证实际变换后的后端，自动准备已有research测试catalog。测试类、窗口Mixin不进入普通运行；测试失败由结果文件传给Gradle。
- Documentation impact: 同步[世界温度](../docs/climate/world-climate-and-temperature.md)、[runtime架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[IR plan](../plans/2026-09-13_17-18-00_infrared-exact-surface-capture.md)及材料主plan。当前文档不再把旧猜测描述为运行路径；没有编辑design或重写既有diary。

## Decisions

- 原计划3个桥接遗漏GPU arena单独硬编码的高精度stride。实际20→32切换出现上传错误后，核实`RenderRegion.DeviceResources`没有公开替换工厂；新增单独的`InfraredArenaFormatMixin`仅修正VANILLA_LIKE读取。Oculus已占用的COMPACT读取不动，因为compact步长仍20。放弃多目标混入同一类，不用可选匹配掩盖缺口。
- 实际Forge AFTER_LEVEL使用另一PoseStack。热图捕获成功不代表最终blend执行；删除错误对象身份判断后，追加最终原图/红外图逐像素验证，防止只凭温度图通过宣布完成。
- 复用base encoder而非复制位打包逻辑；ThreadLocal primitive cursor只在网格构建时读取，不持有world/方块对象。普通shader保持原逻辑；捕获program首次使用才创建。
- 空的已访问terrain pass也清温度为INVALID，省去空图uniform/额外采样分支；活动空帧仍付一次2P-byte清图成本。缩圈完成释放屏幕图；不删除借用main颜色/depth。

## Validation

- Java17：`compileJava processResources compileGameTestJava`通过。
- `gradlew.bat runClient -PinfraredValidation --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`最终BUILD SUCCESSFUL；`build/infrared-client-result.txt`为PASS，日志`build/infrared-client-validation.log`。
- 实际Embeddium+Oculus无光影客户端：20/32-byte均捕获23,932热像素；resize到800×450后37,246。resource reload、缩圈释放、重新开启通过；735次实际模型回调检查owner一致。
- 实际原图/热图37,246像素RGB误差≤1、alpha精确一致；查看了隐藏客户端截图，楼梯与完整块热色覆盖连续。截图`build/infrared-client-stage-5.png`为关闭原图，stage6为重开图。
- [InfraredRasterValidation](../src/gametest/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/InfraredRasterValidation.java)：生产encoder/属性/GLSL，8,640场景、353,123,074分类像素、错误0、GL error 0。覆盖两种格式、D24/D32F、平面/完整块/楼梯/cutout、无边缘豁免；额外温度、混合、清图、resize、资源所有权和并发scope合同通过。独立运行与真实客户端内独立GL context均通过。
- 临时实际encoder成本实验：4096块×24顶点，160轮预热、96轮交错顺序。compact base/owned p50为0.5109/0.9096 ms，p95为0.6942/1.3039；高精度0.8230/0.9629 ms、1.0292/1.2460。测量区间总heap分配128/0 bytes，无逐块scope对象。该实验不是完整chunk/游戏帧时；详情与GPU前期探针引用集中在plan第15节。
- 默认compact网格与native顶点有效载荷不增宽；屏幕自有图像4P→2P bytes。高精度额外4V bytes，构建初始scratch按现有分配关系额外10.5 MiB/context，未冒充进程峰值实测。
- 定向旧符号检索无残留，`git diff --check`通过。服务端代码未改，未重复72项无关服务器GameTest。保留已有热模型工作和无关暂存删除。

## Remaining

- 完整整合包CPU/worker/native/GPU长期基准、能量塔/营火动态场景、实体与特殊资源包组合、维度/断线/交错离屏的扩展自动化验收尚未全覆盖。IR总plan因此保持in-progress，代码与本轮已列测试完成状态单列。
- 当前语义范围为普通terrain solid/cutout、Oculus安装但无光影包；透明介质和独立实体/BE热成像仍是以后工作。未宣称全硬件、全场景绝对最优。
