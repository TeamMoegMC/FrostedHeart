# 风力涡轮耐久

状态：当前实现。最后核验：2026-11-13。范围：VAWT 寿命与物品显示。
代码锚点：`VAWTItem.getWear/setWear`、`VAWTType.getDurability`、`VAWTBlockEntity.damage`。

寿命以 long 毫秒计量，基础分钟数乘以 60000 和配置 `VAWT.vawtDurability`（默认 1.0）。布料、金属、合金基础寿命分别为 120、240、480 分钟；DSP 为 Integer.MAX_VALUE 分钟。保留天气损耗倍率及环境检测优化。

物品使用独立 NBT `VAWTWear` 保存累计损耗，自定义耐久条显示剩余比例，不参与原版耐久系统。旧物品的 `Damage` 仍可读取，下次掉落时写入新格式。方块实体 `damage` 改为 long，兼容旧整数。放置、掉落与克隆只传递损耗及损坏状态，不携带机械动力网络数据。

恢复物品损耗时立即更新实体损坏标志；每次慢速 tick 先更新倍率并扣除寿命，最后发布最终转速。`getGeneratedSpeed` 直接检查剩余寿命，避免损坏状态尚未刷新时接入网络。
