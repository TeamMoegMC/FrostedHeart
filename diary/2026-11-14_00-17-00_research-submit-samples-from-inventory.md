# 研究实物试样支持背包直接提交

- Time: `2026-11-14 00:17:00 +08:00`
- Author: `Codex / OpenAI；实现`
- Status: `completed`（实现、定向回归、正式打包和Playtest同步完成）
- Scope: KnowledgeService样品提交、绘图台SUBMIT_SAMPLE、双语提示与TWR土制炉教程。

## Completed

- 保留单堆栈submitSample接口，新增桌面+候选集合重载。绘图台优先尝试EXAMINE_SLOT，再扫描玩家Inventory.items的36格主背包/快捷栏；桌上错误样品不阻挡有效背包样，第一件正确样提交成功后停止。
- 每件候选继续使用原run/team/task/experiment/item检查与任务回执；完成或重复提交直接返回，不继续扣下一件。没有自动提交副手、装备、鼠标携带物或嵌套背包物品。
- 成功后通知玩家库存/菜单同步；没有增加网络字段、存档版本或新的知识对象。原物件槽仍可使用。
- 中英文操作提示、土制炉正文和任务书同步为“留在背包/快捷栏直接提交”；更新操作、API、小炉和玩家路线文档。

## Validation

- 新增真实玩家Inventory引用回归：快捷栏0、主背包9/35，错误桌面/旧run样不消耗；只扣第一件，重复不扣，桌面正确样优先，缺样时保留所有物品。M3共4项必需GameTest全部通过，日志 `build/research-inventory-submit-20261114-validation.log`。
- 同次Gradle运行在测试通过后遇到Local Dev Mods仓库描述不可再修改、reobfJar.toolJar未配置的ForgeGradle问题；没有修改构建配置，另起独立jar/reobfJar后成功（1m59s），日志 `build/research-inventory-submit-20261114-final-build.log`。未将前一次非混淆JAR部署。
- 最终生产JAR14,913,426字节，已核对中英文提交提示确实在包中且不含GameTest类；确认Playtest未运行后同步JAR和4个双语研究/教程JSON，逐文件核对一致，配置/按键和存档目录元数据不变。旧包/正文及清单在 `build/prism-backups/2026-11-14-inventory-sample-submission/`。TWR正文提交90200588b，原暂存config/.gitignore保留。

## Remaining

- 本轮背包提交的实际客户端点击未验收，不以服务端测试冒充。前一轮札记一级退出和普通炉子GUI的客户端证据及回退点为5eadec97b，已随当前包保留。
