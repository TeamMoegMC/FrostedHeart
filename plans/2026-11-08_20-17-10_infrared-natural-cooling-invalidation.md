# 红外自然冷却与自动退出刷新

- Time: `2026-11-08 20:17:10 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary implementation and validation agent`
- Status: `completed for local investigation; reported persistent freeze not reproduced`
- Scope: 真实物理材料降温、休眠投影、自然退出、持续开启的红外镜像与最终画面
- Related: [红外当前合同](../docs/climate/infrared-view.md)、[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)

## Goal

复现并定位“服务器自然冷却后旧热色仍保留，只有重新进入服务器才刷新”。前一轮熄火后观察Air下降并拆除材料，不能证明材料自然降温与自动退出时的增量失效正确。

## Steps

1. 保留真实煤燃料篝火、零解析场、真实专用服与两个客户端；红外保持打开。
2. 实际点火、材料升温后用铁铲熄灭。此后不拆材料、不重开红外、不重同步或重连，持续观察降温及自然记录退出。
3. 记录服务端LIVE/STORED/MISSING材料状态、实际网络温度、已提交display、GPU值与目标像素，覆盖普通降温和退出清除。
4. 并行核对live发布dirty epoch、休眠温度投影、维护revision及客户端上传；若出现差异保留失败证据、修复对应层并复测。

## Outcome

- 用户确认双端0.7.11；重进归零只观察了刚进入的状态。首包前的空缓存也呈蓝色，因此这条观察不能独立证明服务器已冷。
- 封闭房间长测在约十分钟后因测试JSON替换/读取竞争中断；当时服务端材料仍暖，实际回包与GPU量化一致。修复测试文件通信，未把中断当作同步bug。
- 露天单篝火的自然冷却两轮均通过：开发图形库，以及安装包的Embeddium0.3.31/Oculus1.8.0。目标材料不拆除、不重建，I始终开启，TCP会话数为1。约53游戏秒后目标自然退出，正常增量在只读full探测之前4秒发出，GPU及像素自动清除。
- 本轮没有定位新的生产同步缺陷，也没有更改生产逻辑或增加定时强制全量。现场长期冻结仍未确认，未宣称其已经修好。
- [验证数据](../docs/climate/validation/infrared-natural-cooling-2026-11-08.json)、[对照图](../docs/climate/validation/infrared-natural-cooling-2026-11-08.png)及当前红外文档已记录范围。测试入口新增`--cooling-only`。
