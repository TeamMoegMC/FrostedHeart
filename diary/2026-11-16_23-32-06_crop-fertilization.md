# 作物肥料、多层根部与生长增产

- Time: `2026-11-16 23:32:06 UTC`
- Author: `OpenAI Codex`
- Status: 实现完成，最终专用 GameTest 2/2 通过。
- Scope: FrostedHeart 作物肥料；未修改配套整合包、未提交或部署。

## 决策与行为

新增 `frostedheart:vertical_fertilizable_crops` tag，默认甘蔗、IE 大麻。连续同种的竖直作物共用根部土壤，增速与保温均支持施肥耕地和泥土。肥料施用也识别这个 tag。沿用原有倍率、温度阈值和平均储量消耗，只将作物扣费从生长前概率通过改为确认实际年龄推进或长出节段之后。树苗规则不变。

`CropFertilization` 在生长期间积累增产贡献，以维度 SavedData `frostedheart_crop_fertilization` 保存，成熟后补施不追溯收益。普通作物按年龄比例计账，甘蔗按 16 次增长转移到新节段，大麻上层独立计账。作物重置和替换清理记录，无掉落移除延后到 tick 末清理，以兼容玩家先移除再生成掉落的顺序。

六参数公共掉落复用项目既有 Chorda `BlockDropsEvent`，保留原有事件链；自然掉落四参数入口增加 Mixin。已检查本地 Create 0.5.1.j-55 的 `HarvesterMovementBehaviour` / `BlockHelper.destroyBlockAs` 确实使用此路径，不取消其收割或补种。掉落查询只读取记录，不让预览消耗收益，收获导致的移除/重置负责清理。增产结果按最大堆叠拆分。

## 验证

- `./gradlew compileJava --offline`：通过。
- 最终命令：`./gradlew runGameTestServer --offline -PgameTestNamespaces=frostedheart_fertilizer -PgameTestDirectory=build/fertilizer-gametest`。
- 最终日志：`build/fertilizer-verification-final.log`，`All 2 required tests passed`，`BUILD SUCCESSFUL in 3m 28s`。
- 覆盖：成熟后施肥不增产、全程施肥倍增、未变化不扣肥、预览不耗记录、NBT 序列化恢复、下一轮不继承、多层甘蔗/大麻根部、甘蔗在施肥泥土存活、Create 实际破坏工具收到增产掉落。
- 首轮失败：大麻场景未使用正确的成熟底部和 TOP 状态；连续年龄推进场景照明未稳定，土壤更新导致掉株。修正夹具后通过。中间一次在编译期间补入接口变更造成新测试读取旧主类，报 peek 方法不存在；源码写入结束后完整串行复跑通过。
- `git diff --check`：通过。
- 未执行渲染客户端整机收割验收、自然随机生长全周期统计、独立进程存档重启验证；NBT 测试证明序列化恢复，不等同于重启验收。

## 文档影响

新增 `docs/climate/crop-fertilization.md`，在气候文档 README 和 GameTest README 增加入口。未修改设计文档。
