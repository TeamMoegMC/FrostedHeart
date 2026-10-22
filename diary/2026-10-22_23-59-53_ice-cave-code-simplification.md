# 冰洞性能与代码整理，移除温泉定位

- Time: `2026-10-22 23:59:53 +08:00`
- Author: `OpenAI Codex；主代理整合与边界修复，子代理 ice_features_v3 / crystal_translucency_models 局部实现和复核`
- Status: `completed`（源码及定向验证完成，未提交或部署）
- Scope: FH 冰洞生成、验证脚本与当前文档；TWR 无改动

## Completed

- 删除 `HotSpringLocateCommand`、专用 GameTest 与命令说明；不再注册定位、tick搜索或区块票据。温泉本体、频率与接触回暖不变。
- 小冰晶在 FEATURES 中按 `WorldGenRegion.getCenter()` 的紧邻一圈提前结束搜索；保留可读外侧支撑与合法邻区放置。无需 accessor、额外 Mixin 或日志过滤。
- 条带按256列缓存二维材质；软锰矿四分位随机门控仅在 roll=1 时查询宽厅；去除团块重复体积集合、统一冰材质判断。
- 模型序列化去掉重复写死字段；采样脚本统一标签投影与资源读取，两个 init 脚本运行结束自动调用已有 cleanup。未扩大脚本职责。
- 文档影响：更新 [冰洞当前实现](../docs/worldgen/ice-caves.md)、[冰洞方块](../docs/climate/ice-cave-blocks.md)、[温泉](../docs/climate/hot-spring.md)及世界生成入口。方块文档纠正大小晶体模型、独立纹理、亮度1/3/12；用户确认自然温泉和回暖可用，不再作为本轮待验收项。

## Decisions

- 用户要求处理性能及代码复杂度；不增加温泉逐格群系限制，不继续追查小冰刺密度、长期冻融或多人性能。
- 保留有视觉修复依据的透明拆面、两阶段地形扫描、完整冰体/陷阱预检；不引入通用框架。
- 幂2随机调用等价对当前 Legacy/Xoroshiro 及 WorldgenRandom 包装成立；优化不改变这些随机源的软锰矿门控结果和后续状态。小冰晶仍以实际装饰区域而非候选所属区块确定范围。
- 保留预存荧石亮度12改动以及其它任务文件。没有git add/commit/push，也未生成或替换Prism JAR。

## Validation

- `runGameTestServer -PgameTestNamespaces=frostedheart_ice_cave,frostedheart_ice_cave_body,frostedheart_ice_caves --offline --no-daemon --max-workers=2 --console=plain -Dnet.minecraftforge.gradle.check.certs=false`：**24/24**通过，编译成功，4m56s。日志 `build/ice-cave-simplification-tests.log`。
- 4种随机源、宽窄两类与多种子共4096次旧/新判定及后续状态一致，空间探测934次；这验证调用次数减少，不是整合包TPS或耗时基准。
- 4096格高墙的材质噪声调用为256/512次（每列区域掩码，必要时再查材质）；33300个球体边界候选等价。荧石团块仍为252/288=87.5%，直径5～10。
- 新增真实 `WorldGenRegion` 边界夹具覆盖正负区块、四个水平搜索方向、可读不可写邻区及外侧支撑；空搜索不进入越界诊断，合法邻区末端仍生成晶体。
- 子代理逐字节对照6种资源准备模式，分别345/609/428/692/692/345份资源一致；两个模型文本及PNG不变，2048个纹素朝向检查通过。
- 两个 init 脚本一并 `--dry-run` 成功（21s），确认两个 cleanup finalizer 均入任务图；未为验证脚本启动额外客户端或重做自然采样。日志 `build/ice-cave-simplification-script-check.log`。
- 源码/脚本无遗留 `HotSpringLocate` 引用，编译输出不再包含命令class。差异检查通过；TWR既有暂存项保留。

## Remaining

- 当前改动未提交、打包或同步Prism；实例仍使用上一部署JAR（包括旧定位命令及荧石亮度7）。
- 自动cleanup已检查Gradle任务图及复用实现；本轮未重新执行完整自然采样/客户端任务。历史日记和旧采样结果保留，不冒充本轮结果。
