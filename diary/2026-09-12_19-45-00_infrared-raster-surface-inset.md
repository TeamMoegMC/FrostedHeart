# 红外同平面条纹的实际GPU复现与修复

- Time: `2026-09-12 19:45:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial`
- Scope: `红外深度表面归属；GPU回归通过，用户世界重载后的视觉验收待完成`

## Completed

- 根据用户截图调查黄蓝条纹，实际GPU仅画一个平面就复现，不存在重叠几何。原depth量化后移没有覆盖光栅化/重建误差，部分采样落到空气/缺值侧。
- shader保留原一张温度纹理、色标、0.43混合、场合成数据和扫描范围，改为沿重建表面内侧法线偏移。法线导数在sky/radius early return之前计算，避免分支下导数未定义。
- 取样余量`1/1024 + 4*length(depthNudged-surface)` blocks，保留0.5 block位移界限。余量是显示误差容限，不是热容、材料厚度或实测物理值。
- 新增独立客户端GPU验证入口`InfraredRasterValidation`，使用现有LWJGL/JOML；不注册到没有GL上下文的专用服务器，不新增生产渲染器或依赖。
- Documentation impact: 同步世界温度/runtime说明及当前plan追加结果；既有日记保留。未恢复自然背景或改变解析场逻辑。

## Decisions

- 先复现再修正；不把截图直接定性为传统双面Z-fighting，也不通过改色标掩盖错误。
- 理想点投影的144组数值测试不足以验证实际光栅化；补充真实24-bit/32F depth raster测试，明确两种证据不同。
- 不继续用户已停止的界面输入。隐藏GL上下文用于独立合成平面测试，没有激活游戏或操作用户世界。

## Validation

- RTX 4070 Laptop：原方法在120个平面场景出现3,943,910个错误侧像素。
- 修正后的生产shader：6朝向、8相机高度（0.1..48.5 blocks）、5俯角（5..89°）、3yaw、24-bit/32F depth共1,440场景；130,955,206个被分类像素，错误侧0、GL error=0。日志`build/infrared-raster-validation.log`。
- 运行方式：Java17 source-file模式运行`src/gametest/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/InfraredRasterValidation.java`；classpath使用现有org.lwjgl的lwjgl/glfw/opengl Java及Windows native jars与JOML。工作目录为仓库根目录，shader直接读生产资源文件。
- `processResources compileGameTestJava`通过（15s），已更新运行资源并编译GPU验证入口；服务器代码未在此修正中改变，不为shader表达式重复整套58个服务端测试。

## Remaining

- 用户世界F3+T重载后的实际画面、复杂几何深度边缘和其他驱动验证；不把单平面GPU回归称为全渲染兼容性证明。
