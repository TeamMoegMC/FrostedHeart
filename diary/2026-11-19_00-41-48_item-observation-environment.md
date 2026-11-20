# 物品观察的现场环境与研究判重

- Time: `2026-11-19 00:41:48 +08:00`
- Author: Codex / OpenAI，功能实现与服务端验证
- Status: completed
- Scope: Frosted Research 实物观察、来件、研究条件与记录预览

## Completed

- 实物观察与绘图台检索复用方块和实体的环境采样。预览展示地点、日期、气候和温度，研究条件可读取这些字段。旧物品观察缺少采样值时继续按未知值读取。
- 来件区先比较物品自身字段与允许的 NBT，再复用研究用途等价判定。更新了[观察说明](../docs/knowledge/observations-and-notes.md)和[字段参考](../docs/knowledge/definitions.md)。

## Decisions

- 掉落物取实体当前位置，手持和容器槽取玩家位置，绘图台检索取台子位置。使用和获得物品的行为事件维持原有的轻量实物快照。
- 只有研究使用的环境差异影响同一实物的来件判重。跨记录距离条件可以使用物品检视位置。

## Validation

- Java 17 运行 `gradlew runGameTestServer -PgameTestDirectory=build/item-environment-gametest -PgameTestNamespaces=frostedresearch_observation,frostedresearch_m2`，14 项必需测试全部通过。日志位于 `build/item-environment-tests.log`。
- 已验证手持、槽位、掉落物的完成回包带有现场环境，以及严寒和常温记录可同时收录、学习并按条件匹配。相同严寒条件下仅游戏刻不同的记录会合并。

## Remaining

- 客户端记录窗口尚未进行游戏内目视验收。
