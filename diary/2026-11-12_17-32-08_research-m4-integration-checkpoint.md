# M4 整合复核与回退点

- Time: 2026-11-12 17:32:08 +08:00
- Author: Codex / OpenAI GPT-6 Astra；主协调、独立源码与证据复核
- Status: partial（服务端与打包通过，正式客户端运行中）
- Scope: FH M4机制及M3/M4客户端工具，TWR冶铁正式内容；两仓分别提交。

## Completed

复核阶段状态、临时权限、原型绑定、正式数据与配方差异；独立Astra复核发现的空候选清绑定问题已修并测。M3正式客户端发现的承接翻译前缀错误已修，待M4画面复核。README、总计划及协调记录同步维护；材料数量与原生陶窑前置以16:40更正记录为准。

## Decisions

本地选择性git add/commit作为进入M5前的回退点，不推送。保留TWR原已暂存config/.gitignore和两仓全部无关改动。M4客户端使用固定产物与正式数据快照，后续M5写源不会改变本轮测试版本。

## Validation

主协调逐行核对m4-validation-5.log的38 GAME TESTS COMPLETE / All 38 required tests passed，独立restart-write/read各All 1 required tests passed，无CharcoalPit环境All 4 required tests passed。首轮误报已纠正，未用Gradle退出0替代运行证据。jar+helper合并构建m4-client-build.log成功，2m18s；辅助代码不进入生产JAR。TWR的JSON/引用/双语、JS及173条shaped/79条shapeless材料与索引差异检查通过，实际整包运行仍在验收。

M3正式流程passed及截图已查看，包括真实菜单C2S消耗、成果权限和IE事件门控；不将事件门控等同手摆成型，不将创造测试fixture等同真实生存。

## Remaining

M4客户端画面/真实菜单、正式包配方/风箱，M5控温炉，M6最终目录对照，以及M7多人/生存综合验收。详见协调记录和各阶段交接，源码与日志范围分别记录。