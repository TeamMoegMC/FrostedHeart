# 控温炉空槽边界绘制修复

- Time: `2026-11-12 20:15:07 +08:00`
- Author: `Codex / OpenAI；FH客户端绘制补丁`
- Status: `completed`（编译/生产打包；真实画面由客户端接续简短复核）
- Scope: `src/main/java/com/teammoeg/frostedresearch/knowledge/client/experiment/ControlledFurnaceScreen.java`

## Completed

主协调实际截图确认36个空背包槽因18像素同色背景、17像素步长重叠而形成整块深棕色。仅添加本Screen私有drawSlot绘制：原18×18外框使用KnowledgeUiTheme.MUTED，16×16内底使用PAPER_DARK，相邻槽共享清楚的一像素边线；三设备槽使用同样绘制。布局坐标、步长、容器、逻辑、协议与其它源文件不变。

## Decisions

复用现有主题颜色，不加贴图/框架。无行为、配置、生命周期或数据契约变化，因此living docs无需更新；已有功能验证不因纯背景绘制变化重跑。未新增测试、未操作Git/Prism。

## Validation

[构建日志](../build/m6-furnace-slot-borders-build.log)：compileJava、jar、reobfJar通过，**BUILD SUCCESSFUL in 3m25s**。进程JAVA_HOME为C:/Program Files/Java/jdk-17.0.2。

```powershell
.\gradlew.bat compileJava jar --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false' '-Dorg.gradle.jvmargs=-Xmx4G -XX:MaxMetaspaceSize=1G -Dfile.encoding=UTF-8 -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=10000'
```

产物build/libs/frostedheart-1.20.1-0.7.10.jar，**14907400 bytes**。只读ZIP核对Screen含新drawSlot方法，未混入GameTests。本次JVM采用已有的10000ms连接/读取超时，未更改全局设置。

## Remaining

客户端简短复核36个空槽边线和三设备槽实际画面；本节点未宣称已完成截图验收。Gradle已空闲，生产源码再次冻结。

## Exact manifest

- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/experiment/ControlledFurnaceScreen.java`
- `diary/2026-11-12_20-15-07_controlled-furnace-slot-borders.md`
