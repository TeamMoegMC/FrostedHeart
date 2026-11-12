# M7 研究入门任务书引导

- Time: 2026-11-12 18:05:27 +08:00
- Author: Codex / OpenAI GPT-6.1 Sol medium；独立玩家教程
- Status: partial：静态结构、引用和文案核对完成；未在客户端加载。
- Scope: TWR 新增独立 FTB Quests 说明章节及中英文语言；FH 仅新增本日记。

## Completed

TWR 精确变更3文件：

- 新增 config/ftbquests/quests/chapters/research_practical_guide.snbt
- 追加 kubejs/assets/twr_quests/lang/zh_cn.json 的32个新键
- 追加 kubejs/assets/twr_quests/lang/en_us.json 的32个新键

已先检索现有 a_day_of_researcher.snbt、t1研究入口和tip研究提示。旧“研究者的一天”讲旧科技点/图谱及其他研究设备；保留其全文与ID，另用独立“研究入门：观察与实物试验”说明当前知识/实物流程。新章节归入既有 rebuild 组121B12860A7D323A，order_index=1，无需新增组或修改索引，章节目录由FTB自动发现。

章节ID为4A8E1F92C73B605D。五卡ID依次为507C594CBE4EDD30、56B3257EE1F12A5B、564A135A9DC0E980、594A74A3D9A1A554、5A6E8872E26A30E3；内容为观察入档、构想/联络/开始条件、土制冶炼炉、冶铁炉、普通耐火砖。三条路线各三段，带失败/丢失恢复。未写保温杯改版或城镇两建筑为已实现内容。

## Decisions

遵循现有checkmark任务惯例，每卡仅一项“已读说明（不推进研究）”。所有卡独立可读，无dependencies、rewards、item任务、研究完成命令或权限奖励；勾选不会完成实际研究。图标只用已注册的放大镜、绘图台、均衡小炉、正常砂质风口及普通耐火砖。

文案区分观察保留后学习、构想/成果通常自动入档、可见不等于可开始；联络10秒是基准，跟随界面实际计时。M3说明绘图台抄录台物件槽提交；M4说明风道/临时原型/同队现场观察；M5说明准备→烧制、1100°C建议、只观察不消耗及批量工艺。没有run、CODEC或Java包等玩家不需要的实现信息。

## Validation

- 新SNBT采用合法JSON子集的compound/list语法，JSON.parse及字段类型检查通过；未冒称调用了FTB原生解析器。
- 11个新章节/任务/勾选ID全部16位HEX且互异，与现有全部SNBT无碰撞；group引用存在，filename与文件名一致。
- 所有title/subtitle/description/task文本键在中英文资源中存在；新增键没有覆盖旧键。写入后逐键比较，两个原有语言文件各1009个旧值全部保留，分别新增32键。
- 五卡均检查无依赖、无奖励、单checkmark、可见及坐标有效；五种图标由FH注册源码与当前Charcoal Pit 7748097 JAR blockstate核对。路线数量依据本轮正式M3/M4/M5数据，不引用被TWR替代的原生配方。
- 相关TWR树/父目录此前与本次检索未找到AGENTS；遵循FH约定。没有改config/.gitignore、.workbuddy、旧章节/旧文字或其他工作者文件，没有Git/Gradle/客户端/Prism操作。

## Remaining

主协调最终包中加载章节，检查SNBT/语言资源生效、同组排序、GUI缩放与英文换行、checkmark只标已读且不推进研究。config树的默认rg检索会忽略这些章节，本次使用--no-ignore找到原文件；未为本任务调整既有.gitignore，提交时须确保新SNBT被纳入。
