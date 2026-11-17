# 温度评分展示与说明同步

- Time: `2026-11-17 UTC`
- Author: `OpenAI Codex`
- Status: 完成代码、翻译、文档与编译验证。

住宅温度提示从“越接近24℃越好”改为18～28℃满分、冷侧逐渐下降、热侧平缓下降，以及−15/42℃达到配置最低分。狩猎提示同步说明评分和独立工作门槛；详情温度评分行注明满分区间。中英文同步，住宅提示删除旧单温度占位符及参数。

BoardPresenters 的冷热分界固定为平顶中点23℃，HouseViewPresenter 的过热弱项使用28℃上界，HouseOverviewElement 的低温判断使用18℃下界，避免旧舒适温度配置改变新曲线的冷热说明。TownFormat 注释同步。保留现有暖和评分阈值及工作范围。

核实 comfortableTemperatureCelsius 仍用于 HouseBlockEntity 供热修正下限，配置注释明确其用途；同步 docs/town/town-model.md，纠正上一条日记仅提及界面用途的不完整描述。供热行为未修改。

验证：`./gradlew --offline compileJava` 通过（40秒，21项既有警告）；中英文JSON解析、住宅提示无旧占位符与狩猎工作门槛占位符检查通过；`git diff --check` 通过。未运行客户端，长提示实际排版尚未进行游戏内验收。
