# 红外分包重试与编码扫描修复

- Time: `2026-09-09 20:23:18 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `InfraredViewRenderer / InfraredBrickCodec`

## Completed

- 接收分包时暂停定时重试；36个不同温度后停止调色板扫描，复用RAW编码。
- 更新[现有计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)与[runtime文档](../docs/climate/thermal-runtime-architecture-and-optimization.md)。

## Decisions

- 复用现有布尔状态、分支和协议，不增加缓存；等待首包仍重试，窗口移动仍立即发full。

## Validation

- Java17 `runGameTestServer --offline --no-daemon --console=plain -I run-gametest/infrared-test-scope.gradle`：27项真实Forge测试通过，BUILD SUCCESSFUL。临时脚本排除含未接通掉落物接口的旧测试类，未运行JUnit。
- 日志：`run-gametest/infrared-retry-codec-verification.log`；r128载荷1104489字节、2包，完整解码与协议往返通过。

## Remaining

- 客户端慢网接收时序尚未实机验证；生产编译及静态路径核对不替代这一验证。
