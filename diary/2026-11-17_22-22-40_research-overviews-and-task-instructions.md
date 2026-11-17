# 三个正式研究的简介与操作步骤

- Time: `2026-11-17 22:22:40 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: FH研究任务页与土制炉旧进度兼容；TWR三个研究的中英文说明及土制炉阶段数据。

## Completed

- 按用户两张标注截图，三个项目的`body`改为简洁目标、实际实验物品与成果用途，具体制作、设备操作和观察放到阶段任务页。使用JEI可搜索名称，如【未烧小炉】【鼓风口原型】【砂质砖鼓风口】【控温实验炉】；同时核对英文名称。
- 土制炉采用显式`main`单阶段，通用试烧方法写入阶段`method`，三种配比写入各自`tasks[].method`。三个原观察需求、任务ID、开始条件、成果和旧研究映射完全相同，制作提示不增加任务或检查。
- `KnowledgeLayer`直接展示各条任务的`method`，观察任务取消重复的“无需入档”正文与灰色操作提示。全为观察任务时不再显示切换栏，正文获得原栏位的空间。
- `TrialKilnSamples.reconcile`同时接受旧平铺定义与显式`main`单阶段，保留旧完好样提交回执并补齐比较任务。正式fixture同步实际项目，既有测试改用`allRequirements`及保留显式阶段的正式定义。
- 冶铁炉说明改为规划风道、制作原型、风箱试吹和原地观察；耐火砖分开原料/设备准备、控温试烧与观察，参考路径继续提供更准确的温区。删除重复的知识入档和记账解释；联络说明中的旧等待秒数改为当前连连看难度。
- 更新[任务页说明](../docs/knowledge/workbench.md)、[作者指南](../docs/knowledge/authoring-guide.md)、[内容模板](../docs/knowledge/content-template.md)与[土制炉](../docs/knowledge/earthen-smelter.md)。无build.gradle、gradle.properties或design/改动。

## Decisions

- 使用现有阶段/任务说明字段承载无需验证的制作步骤，不新增任务类型或制作监听。
- 文案本身检查JSON、语言键及实际物品名；本轮另涉及任务页显示与土制炉阶段定义，因此复用已有进度兼容测试，没有为文案另写逻辑测试。

## Validation

- TWR中英文各63个语言键一致，三个项目所有语言引用有效；土制炉旧`requirements`与新阶段中三项`requirement`逐项完全相等，其余开始条件、成果和旧映射一致。正式fixture与TWR项目字节一致。
- 首次构建因JVM无法分配native memory中断；诊断保存在`build/research-guidance-first-jvm-crash.log`。只用命令行将Gradle和GameTest堆降到2G、限制worker/处理器后重试，无构建配置文件改动。
- Java17 `runGameTestServer jar reobfJar validateResearchCatalog -PgameTestNamespaces=frostedresearch_m3,frostedresearch_m3b,frostedresearch_m4 -PgameTestDirectory=build/research-guidance-20261117 -PthermalTestHeap=2G -PthermalTestProcessors=4 --max-workers=1 -Dorg.gradle.jvmargs="-Xmx2G -XX:MaxMetaspaceSize=512m -XX:ActiveProcessorCount=4 -Dfile.encoding=UTF-8" -PresearchCatalogDir=../TheWinterRescue/config/fhresearches --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，4m22s；24项required tests全部通过，81个旧研究定义有效。包含三配比正常观察、旧单样品提交/v4旧进度接续、已完成历史和阶段流程。日志`build/research-guidance-retry-20261117.log`。
- 用户确认正常退出后，同步正式JAR、TWR中英文语言文件和土制炉项目四个文件到Research Playtest，逐字节核对；备份`build/prism-backups/2026-11-17_22-22-14-research-guidance/`。本轮未额外启动客户端做视觉或生存验收。
- TWR选择性本地提交`e20238251`；保留原暂存`config/.gitignore`。FH只提交本轮文件，保留原`.gitignore`及无关未跟踪文件。两个研究分支均未推送。

## Remaining

- 更新后的排版由玩家下次启动Playtest查看；代码与数据校验不代表新的实机视觉验收。
