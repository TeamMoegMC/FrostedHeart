# 栽培树苗温度计作物同款布局与无数值进度条

- 作者：OpenAI Codex
- 状态：实现及服务端验证完成；未进行本轮渲染客户端验收。

## 玩家展示

CultivatedTreeTempStats替代单行状态文本，复用PlantTempStats/FHTextIcon的颜色、图标条、根部肥料条与Shift展开布局。默认显示状态/等待原因、阶段名称、生长进度条和当前温度位置条，施肥时增加带等级图标的肥料储量条；Shift展开能长/理想温区和光照要求条。树苗不显示温度度数、进度分数、百分比或阶段编号。信息不足时仅显示等待配置，不回退到旧数值状态行。原有普通作物使用默认数值模式，既有颜色格式保留；无数值模式仅供树苗调用。

## 同步和边界

新增服务端TreeInfo快照包含阶段、进度/总进度、等待原因、保温修正后的温区、理想区间及光照门槛；客户端不依赖服务端Profile静态缓存。沿用坐标对应缓存与20客户端tick刷新。目标产量信用、目标高度、种子不包含在新快照中。网络协议后缀改为soil-thermometer-3，客户端/服务端需共同更新。生长、肥料消耗、掉落及p/v存档逻辑不变。

## 验证

初版编译通过，但首轮27项中10项在fixture light等待条件超时，未到达生长断言。测试光源异步传播等待上限由100/160tick延长至600tick，保留原光照条件，未修改生产逻辑。补充保温肥温区快照与非树苗清空数据检查、原快照编解码字段等值检查后，compileJava/compileGameTestJava及完整GameTest28/28通过，BUILD SUCCESSFUL；日志build/cultivated-thermometer-bars-validation.log。随后仅保留普通作物光照分隔符原样颜色并再次compileJava通过，日志build/cultivated-thermometer-final-compile.log。git diff --check通过。未将服务端测试当作客户端视觉验收。同步docs/climate/cultivated-oak.md及中英文翻译。

本轮未提交或推送。
