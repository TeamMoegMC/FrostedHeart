# 城镇建筑非对称平顶温度评分

- Time: `2026-11-17 UTC`
- Author: `OpenAI Codex`
- Status: 实现及离线编译、数值验证完成。

## 修改与决策

`TownMathFunctions.calculateTemperatureRating` 改为用户指定的 -15/18/28/42℃ 分段曲线；冷侧 u²(2−u)，热侧反向 quintic smoothstep。新增三参数入口实现 minimum+(maximum−minimum)×S，现有五参数入口委托新实现，满分为1。保留默认最低分0.017，因此极端温度实际评分仍为0.017，平顶严格为1。

核实住宅 HouseBuilding、HouseDailyModel、HouseOverviewElement、狩猎 HuntingBaseBlockEntity/HuntingBaseScreen、BoardPresenters 和 TownStageFourModel 均调用共享入口，无需逐个改动。旧中心、斜率与中点参数为接口和配置兼容保留，不再控制评分；comfortableTemperatureCelsius 在界面温度标签中仍有独立用途。配置注释及 docs/town/town-model.md 9.4 已同步。未改住宅工作温度范围和独立温度压力。

仓库要求的 diary/README.md 当前不存在；参照近期日记记录时间、作者、决策和验证。

## 验证

- `./gradlew --offline compileJava`：通过（1分13秒），21项现有弃用/Mixin等警告。
- 独立 javac 编译实际 TownMathFunctions，JShell 检查 -100/-15/18/23/28/42/100℃ 边界与平顶；以0.001℃步长扫描冷侧33001点、热侧14001点，验证单调性及[0,1]范围；检查冷侧中间值、显式最低/满分映射和旧入口平顶，全部通过。
- `git diff --check`：通过。
- 未运行 Forge GameTest 或客户端游戏验收；本次仅验证纯数值函数及生产代码编译。
