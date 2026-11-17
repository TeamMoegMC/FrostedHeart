# 红外开关不再依赖附近温度覆盖

- Time: `2026-11-05 16:02:05 +08:00`
- Author: `OpenAI Codex / GPT-6; primary engineering agent`
- Status: `completed`（源码与生产构建；本次未独立进行客户端画面验收）
- Scope: `InfraredViewRenderer`、红外当前行为文档、生产 JAR

## Completed

- 在[原因定位](2026-11-05_15-55-54_infrared-empty-coverage-diagnosis.md)后，用户质疑无篝火区域无法开启红外的处理。本次删除 `displayHasCoverage` 及其派生检查，使扫描半径只随 `open` 开关展开或收回。
- `visitTerrainPass()` 与 `prepareSurfaceCapture()` 不再要求材料/解析场覆盖。首个响应到达前，首次实际捕获按需初始化既有温度纹理为 `INVALID_TEMPERATURE`，采用原 shader 的缺失值蓝色占位；整窗无覆盖也可以显示扫描。
- 首次捕获初始化会清除复用 CPU mirror 中的旧样本，覆盖 `reset()` 后重新进入世界、而新响应尚未到达的路径。后续 full/delta 继续更新同一张温度纹理。
- 数据覆盖消失不再触发缩圈；用户关闭开关仍按原扫描动画收回。
- 生成修复后的 `build/libs/frostedheart-1.20.1-0.7.7.jar`。本次没有替换正在运行的本地游戏实例 JAR。

## Decisions

- 将开关交互与是否已有可显示温度解耦，去掉导致隐含篝火条件的派生状态；缺失值显示沿用既有 shader。
- Documentation impact: 同步更新 [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md)，明确无覆盖/等待首包时的扫描、蓝色占位及跨世界初始化行为，并更新本次核对日期。
- 配套整合包仓库与实例配置没有变更。

## Validation

- Java 17 在当前工作区运行 Gradle Wrapper `build --offline --no-daemon --console=plain`，`BUILD SUCCESSFUL in 1m 10s`；生产编译、打包和重新混淆成功。日志：`build/infrared-empty-coverage-build-20261105.log`。
- 按仓库既有约定，`compileTestJava` 与 `test` 均为 `NO-SOURCE`，没有新增 JUnit 或镜像实现的测试。本次不涉及服务端温度求解，未重复运行服务端热模拟 GameTest。
- 用 `javap` 核对最终生产 JAR：红外类中不再存在 `displayHasCoverage`；`clientTick()` 的半径分支只读取 `open`；`prepareSurfaceCapture()` 包含首次纹理初始化和旧 mirror 清除。
- 本次修改文件的 `git diff --check` 通过。

## Remaining

- 修复包尚未在真实客户端独立验收无篝火开启、首包延迟、失去覆盖后保持开启、关闭及跨世界重开画面。当前已启动的客户端仍运行其启动时加载的代码。
