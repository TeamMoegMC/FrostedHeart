# 调查并停用旧树苗事件扣肥

- 作者：OpenAI Codex
- 状态：旧扣费块已按用户要求注释，未改新树苗扣费策略。

ClimateCommonEvents.saplingGrow(SaplingGrowTreeEvent) 原逻辑在allow为true时直接扣4格FertilizedDirt.STORAGE，耗尽转普通泥土，然后才放行树木生成事件；并不等待配置树形实际放置成功。现将该完整扣费块逐行注释，保留DEFAULT/DENY结果及温度、光照、保温和加速许可逻辑。CropFertilization的普通作物扣费不变。

栽培树苗通过CultivatedOakBlockEntity.advance按实际推进（或满进度后等待产量的有效推进）扣1格，通过Feature.TREE.place直接生成，不经过旧SaplingGrowTreeEvent。因此源码调查未发现旧事件与新账本对新树苗重复扣费；当前每步一格本身也会较快消耗，此次没有擅改这项策略。同步crop-fertilization与cultivated-oak活文档。

验证仅源码路径检查与git diff --check；本轮只注释生产代码，未重新编译或运行游戏测试。之前温度计展示修改保持原样。未提交、未推送。
