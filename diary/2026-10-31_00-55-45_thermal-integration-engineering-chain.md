# 11项热模拟接入的完整工程链路

- Time: `2026-10-31 00:55:45 +08:00`
- Author: `Codex; OpenAI GPT-6; engineering design and closure review`
- Status: `completed scoped implementation design; production changes not implemented`
- Scope: `热模拟11个Mixin的接入、所有权、拒绝/取消/释放及删除验收`

## Completed

- 原地重写[既有研究计划](../plans/2026-10-30_23-51-15_thermal-mixin-integration-review.md)，为11项分别补齐入口、处理、后续交接、失败分支、实施步骤、删除项、代价和验收。
- 篝火方案包括最终Chunk通知、直接源登记、容量拒绝复用原发现队列、显式补查重建旧快照，以及对现有启动等待记录扩展一个位置以在额度版本变化后重试；不新增第二套源权威或队列。
- 保存方案包括原版helper内实际save包装、干净区块快路径、prepared标记finally恢复、封存返回false延期和卸载原future身份保持。
- 更正此前保存成本表述：当前维护保存的captureRegionalCheckpoint本身同步等待接管/finish；移动钩子只减少重复筛选，不表示自动保存零等待。
- 更正雪检查的实施范围：内层也保护直接randomTick调用。本批保留两条入口保护，不增加上下文缓存强行去重；随机循环只集中实际业务，不冒称已减少整段接管。
- 给出岩浆通知删除需要的逆向Brick依赖和派生mask成本，明确本批保留；禁冻规则仍覆盖SnowAndFreezeFeature。

## Decisions

- 以可实施删除项和明确保留范围交接，不把候选全部标为“需要核对”，也不把未知结果当作通过。
- 实质目标包括专用篝火onPlace退出、保存冷却字段依赖退出、Ice.playerDestroy副本退出、BE判空继承适配退出和共享业务归位。

## Validation

- 核对源码调用者、生成数据和原版相关字节码。计划含11个独立工程章节，全部相对文件链接存在。
- 本轮没有修改生产代码、编译或运行JUnit/GameTest。工作区原有源发现改动保持，没有新性能实测或正式服务器资格。
- Documentation impact: 更新原工程计划和热系统待办。现行行为文档未改变，历史日记不重写。

## Remaining

- 方案对应代码与联合重点验收尚未执行；通知中间态、公开API复制成本和保存同步等待均已在计划明示。
