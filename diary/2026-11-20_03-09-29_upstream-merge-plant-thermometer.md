# 合并远端栽培树木与本地温度计改动

- Time: `2026-11-20 03:09:29 +08:00`
- Author: `OpenAI Codex`
- Status: `origin/master` 已快进合并，七处工作区冲突已解决，29 项专项测试通过。
- Scope: `5edad63b6..ff48ad340`、`CultivatedOakBlock.randomTick`、`SoilThermometerUpdatePacket.sample`、`TemperatureGoogleRenderer.receivePlantReadout`、`FHNetwork.protocolVersion`。

## 合并结果

- 拉取栽培树木接入原版树苗与市长印章配方两个提交。合并前将七个重叠文件保存到 Git stash `907dd47b3`，合并后恢复本地改动并保留这份备份。
- 栽培树苗随机刻采用远端的随机源及本地材料优先取温。土壤温度计回包携带坐标、温度、天气暴露标志和可选树苗状态，继续按当前目标刷新。客户端与服务端使用合并后的 `soil-thermometer-3` 协议。
- 更新栽培橡树、作物肥料、世界温度文档和气候系统入口，使取温入口、回包字段和协议标记与代码一致。

## 验证与剩余范围

- JDK 17 下运行 `gradlew.bat --offline runGameTestServer -PgameTestNamespaces=frostedheart_cultivated_oak,frostedheart_crop_temperature -PgameTestDirectory=build/upstream-ff48ad340-gametest`。生产与 GameTest 源码编译通过，29/29 项通过，其中 27 项栽培树木测试和 2 项作物取温测试。日志位于 `build/upstream-ff48ad340-gametest.log`。
- `git diff --check` 通过，`master` 与 `origin/master` ahead/behind 为 `0/0`。合并后的客户端悬浮面板待实机验证。
