# 红外全窗口上传与节点展开

- Time: `2026-09-09 20:34:17 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `InfraredViewRenderer / MinecraftThermalInput.InfraredCapture`

## Completed

- 全窗口变脏时复用整纹理上传，省去逐页复制；普通物理混合Brick按节点读取并展开，删除线性去重、槽位数组及预填循环。
- 更新[现有计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)与[runtime文档](../docs/climate/thermal-runtime-architecture-and-optimization.md)。

## Decisions

- 无新增持久状态或缓存；保留部分Page上传、尾包提交与原物理查询一致性检查。减少一个int[64]数组，载荷256字节。

## Validation

- Java17 runGameTestServer及原临时范围脚本：27项全部通过，BUILD SUCCESSFUL；未运行JUnit，仍排除含未接通掉落物接口的旧测试类。
- 扩展真实篝火测试验证无解析场的混合Brick热空气、石块INVALID及协议往返。日志：run-gametest/infrared-upload-node-verification.log。
- 已删符号无残留调用；git diff --check通过。

## Remaining

- 客户端OpenGL上传与FPS尚未实机验证。
