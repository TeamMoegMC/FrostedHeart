# 红外复查：恢复实际shader绑定并保持缓存一致

- Time: `2026-09-13 21:49:19 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `本次红外实现的代码复查与局部渲染状态修复`

## Completed

- 复查真实owner、两种vertex format、GPU arena、program选择、帧状态、纹理/FBO生命周期和旧逻辑残留。Embeddium源码中四类原格式引用使用点均已有对应处理；未恢复旧渲染分支或引入新的兼容层。
- 修正[InfraredViewRenderer](../src/main/java/com/teammoeg/frostedheart/content/climate/render/InfraredViewRenderer.java)最终混合把GL program解绑为0却不恢复的问题。`ShaderInstance`缓存仍记着原program时，再次apply同一shader会跳过重新绑定。
- 增加一个局部`previousProgram`，退出时raw GL恢复原绑定，替代`program.release()`。LDLib绑定使用raw GL；经Oculus缓存的`GlStateManager._glUseProgram`可能跳过恢复，不能混用。未新增生产类、Mixin、持久字段、图像或draw，四个生产桥接不变。
- 在现有隐藏客户端测试加入`verifyBlendState`，检查原版shader重复apply及viewport/scissor恢复。
- Documentation impact: 更新世界温度、runtime文档与IR plan，明确原版/Oculus program缓存及成对恢复合同。没有改design、服务端热模型或既有diary。

## Decisions

- 用实际客户端失败→修复后通过确认问题，避免只看未变换的Minecraft字节码推断Oculus运行行为。
- 仅修复有证据的状态遗漏；不为减少行数改动必要接口，不增加通用GL状态管理框架。

## Validation

- Java17，`gradlew.bat runClient -PinfraredValidation --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`。
- 原实现的新增测试在`build/infrared-review-before.log`失败：`Infrared blend lost the active vanilla shader`。
- 最终`build/infrared-review-after.log`为BUILD SUCCESSFUL，`build/infrared-client-result.txt`为PASS；新增shader缓存/viewport/scissor检查通过。
- 同次运行两种格式、resize、资源重载、关闭释放/重开通过；37,246实际表面像素RGB误差≤1且alpha精确保留。8,640 GPU场景、353,123,074分类像素错误0，GL error 0。
- 定向旧符号检索无猜块/中转复制残留；`git diff --check`通过。未重复无关服务端测试或扩大负载实验。

## Remaining

- 本次复查发现的问题已修复；IR plan中既有完整整合包性能、扩展场景验收未覆盖项保持原状态。本次结果不是全场景无缺陷或全局最优保证。
