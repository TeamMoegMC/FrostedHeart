# 补齐三个正式构想的桌面知识组合

- Time: `2026-11-13 20:15:00 +08:00`
- Author: `Codex / OpenAI；正式内容修正与部署`
- Status: `completed`（数据、匹配回归与 Playtest 同步完成；客户端新入口待重走）
- Scope: TWR三条LinkRule、双语研究/任务书说明；FH正式fixture、M2回归与living docs。

## Completed

- 土制炉：黏土球+沙的item观察；冶铁炉：铜或青铜锭+煤/木炭/焦炭的item观察；耐火砖：高岭土+铝土矿+黏土球的item观察。选中已学会的记录后联络，替代三项目原有inputs=[]。
- 行为入口和 acquisition.required=[] 保留，观察仍为 acquisition.optional；主动联络的材料由LinkRule.inputs单独要求。行为概率与联络等待继续按原optional规则计算；默认对应160/160/140tick（20TPS时8/8/7秒）。没有改变项目开启条件、实验、已得想法过滤或现有档案。
- 修正两语言的研究正文、联络提示、任务书教程及四份玩家路线文档，明确真实桌面组合；泛用零输入功能和示例仍可由其它作者使用。
- 旧M3/M4/M5正式link fixture同步更新；新增与TWR正式JSON字节一致的三个组合fixture，并用真实LinkRule codec及LinkMatcher回归，不改生产Java代码。

## Validation

- Java17下 frostedresearch_m2 的8项必需测试全部通过（含新增正式组合，耗时5m50s），原始日志 `build/formal-research-links-20261113-validation.log`。
- 新例覆盖三配方不能空桌面获取、黏土/沙顺序无关、缺一项不能匹配、耐火砖三项齐全以及冶铁2×3种替代组合。冶铁标签成员通过注入TagLookup测试，不能冒充本次正式客户端注册表加载验证；原正式标签文件未改。
- JSON有效性、三个正式定义与新旧fixture字节一致、行为必要前置未改、两仓diff --check通过。TWR提交6e6e5f24e只含7个自有文件，原暂存config/.gitignore核对不变。
- 确认Playtest已退出，同步3个规则JSON与4个双语内容/教程JSON，逐文件核对与源码一致；配置/按键及存档目录名称和修改时间不变。旧文件备份及清单在 `build/prism-backups/2026-11-13-formal-idea-material-links/`。既有生产JAR一致且未替换，测试类不安装到客户端；未启动游戏，未触碰Development/runClient。

## Remaining

- 使用尚未取得相应构想的队伍，在Playtest按新组合完成一次实际联络；已有构想按上一轮规则仍隐藏。11月12日M5客户端证据和自动演示开场使用旧零输入路线，不能视作此次新入口的客户端证据。
