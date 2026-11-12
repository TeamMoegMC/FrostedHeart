# Research Playtest Environment Preparation

- Time: `2026-11-12 13:05:54 +08:00`
- Authors: `Codex / OpenAI GPT-6 Luna medium；只读验收环境调查`
- Status: `completed`；本准备已落实为独立Playtest/Peer实例，下文调查数据保留其原时点。
- Scope: M3/M7 正式整合包新世界验收环境；PrismLauncher 本地实例、FrostedHeart 开发运行和 TheWinterRescue 数据。
- Related: [完整开发计划 M7](2026-11-11_23-38-07_research-system-playable-development-plan.md)、[M0 内容基线](2026-11-12_12-36-00_research-m0-content-baseline.md)、[M0 交接](../diary/2026-11-12_12-40-00_research-m0-baseline-handoff.md)。

## 目标

给后续 M3/M7 提供一个来源明确、不会使用玩家原存档的正式包测试入口。本文只记录准备调查与拟执行步骤，不表示新客户端已经启动、正式包已经验收或任何生存流程已经通过。

## 当前核实状态

### Local instances

PrismLauncher 实例根目录为 `D:\games\minecraft\prismlauncher\instances`。可用的较新基线是：

`D:\games\minecraft\prismlauncher\instances\The-Winter-Rescue-Development`

该实例有 `.minecraft\mods`、`config`、`defaultconfigs`、`kubejs`、`resourcepacks` 等整合包运行目录。其 `mmc-pack.json` 标记 Minecraft `1.20.1`、Forge `47.4.0`；`instance.cfg` 的 Java 为 Microsoft OpenJDK `17.0.15`，路径是 `D:/games/minecraft/prismlauncher/java/java-runtime-gamma/bin/javaw.exe`。`mods` 中实际可见 FH `frostedheart-1.20.1-0.7.10.jar`、Charcoal Pit `3.12.0`、Immersive Engineering `10.1.0-171`、Immersive Industry `0.2.4`、KubeJS `2001.6.5-build.16`、Rhino `2001.2.3-build.6`、Stone Age `1.6.8`、Create `0.5.1.j` 和 JEI `15.20.0.111`，以及 FTB Library/Teams/Chunks/Quests。调查时它有 81 个 `config/fhresearches/*.json`，这个数量是当时基线。

本地开发实例的 `kubejs/server_scripts` 有 56 个文件；TWR 工作仓库有 57 个。唯一在工作仓库而不在该实例中的脚本是 `src/recipes/warm_stone.js`。所以要测当前 TWR 工作树，测试副本应使用工作仓库完整 `kubejs/`，不能只依赖实例里现有脚本。

此开发实例含有两个 `saves` 世界目录。推荐使用独立副本运行验收，以免影响玩家世界；若之后在原实例开展其他操作，也必须保留原有存档内容不变。本交接只给出独立副本流程。

另有 `冬季救援公众版`，也是 MC `1.20.1` / Forge `47.4.0`，但安装 FH `0.6.14`、79 个研究配置和 52 个 KubeJS 服务端脚本，版本/内容均较旧，不作为这轮验收基线。

### Repository and development-run sources

- FH 当前源码声明 `minecraft_version=1.20.1`、`forge_version=47.3.0`、`mod_version=0.7.10`，见 `gradle.properties`。Gradle `minecraft.runs.client` 工作目录为 FH `run/`，见 `build.gradle`。
- FH 的 `run/mods` 当前为空；`run/kubejs/server_scripts` 只有 `example.js`；`run/config/fhresearches` 有 87 项，其中 6 项是开发侧额外定义。调查时 TWR 正式仓 `config/fhresearches/` 有 81 项，KubeJS 服务端脚本有 57 个文件。数字与差异来自 M0 交接记录及本次目录复核，仅作为调查时基线。
- `runClient` 的模组来自 Gradle 依赖配置，不是 `run/mods` 目录。`build.gradle` 用 `implementation`/`runtimeOnly` 等提供 IE、Create、Flywheel、Registrate、Steam Powered、Caupona、LDLib、Embeddium、Curios、Architectury 等运行依赖；`clientRuntimeOnly` 另提供 Immersive Industry 文件 `7530803`、Stone Age `1.6.8`、Charcoal Pit 文件 `7748097`、KubeJS 文件 `5853326`、Rhino `2001.2.3-build.6` 和 Oculus。该开发类路径不会自动加载 TWR 工作仓完整 KubeJS、正式 81 项研究定义或整合包其余配置。
- FH 仓 `build/libs/` 当前可见的是 `frostedheart-1.20.1-0.7.7.jar`、对应 `-deobf.jar` 和 `-sources.jar`；该目录中没有当前 `0.7.10` 生产 JAR。Prism 开发实例中的 `0.7.10` JAR 是实例已有文件，不证明它由当前工作树构建。
- FH `mods.toml` 的 Forge 范围是 `[37,)`，Minecraft 范围是 `[1.20.1,1.20.4)`；源码编译目标 Forge `47.3.0` 与本地整合包实例 Forge `47.4.0` 仍是两个不同运行基线，应在正式包副本里记录并验证实际启动结果。
- TWR 仓 README 将项目定义为 Minecraft 整合包；`kubejs/README.md` 和 `kubejs/CONTRIBUTING.md` 说明 KubeJS 资源/脚本组织方式，并记录原开发实例的 `.minecraft/kubejs/` 布局。当前 TWR 工作仓中正式候选数据位置为 `kubejs/` 与 `config/fhresearches/`；M0 记录的新知识示例数据包不会自动随 FH 模组加载。

## 正式包隔离测试环境准备

以下步骤供 M3/M7 执行。构建和启动均未在本次调查中进行。`build.gradle` 中的 `jar` 任务配置了 `finalizedBy('reobfJar')`，因此运行 `jar` 会在普通 JAR 任务后执行 ForgeGradle 的重映射任务；仓内历史构建记录也将该产物记为生产 JAR。实现阶段先在 FH 仓用进程级 Java 17 构建；按当前版本字段，预期生产映射 JAR 路径为 `FrostedHeart\build\libs\frostedheart-1.20.1-0.7.10.jar`：

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat jar --no-daemon --console=plain
```

1. 在 PowerShell 中设定路径并建立新的 Prism 实例目录。`robocopy` 只从现有实例读取整合包运行文件，并排除所有实例存档及玩家个人状态目录：

```powershell
$sourceInstance = 'D:\games\minecraft\prismlauncher\instances\The-Winter-Rescue-Development'
$testInstance = 'D:\games\minecraft\prismlauncher\instances\The-Winter-Rescue-Research-Playtest'
$expectedTestInstance = 'D:\games\minecraft\prismlauncher\instances\The-Winter-Rescue-Research-Playtest'
if ($testInstance -ne $expectedTestInstance -or (Test-Path -LiteralPath $testInstance)) {
    throw "Choose a new, unused dedicated test-instance path: $testInstance"
}
$sourceGame = Join-Path $sourceInstance '.minecraft'
$testGame = Join-Path $testInstance '.minecraft'

New-Item -ItemType Directory -Path $testInstance | Out-Null
$testInstanceCreatedByThisRun = $true
Copy-Item (Join-Path $sourceInstance 'instance.cfg'), (Join-Path $sourceInstance 'mmc-pack.json'), (Join-Path $sourceInstance 'twricon.png') -Destination $testInstance
New-Item -ItemType Directory -Path $testGame | Out-Null
foreach ($name in @('mods', 'config', 'defaultconfigs', 'kubejs', 'resourcepacks', 'shaderpacks')) {
    robocopy (Join-Path $sourceGame $name) (Join-Path $testGame $name) /E
}
```

2. 在 PrismLauncher 实例列表刷新并选择 `The-Winter-Rescue-Research-Playtest`，需要时只重命名副本显示名，并确认组件仍是 MC `1.20.1` / Forge `47.4.0`。该副本没有 `saves` 目录。保持 Java 17；若 Prism 没保留副本的 Java 设置，只为此测试实例选择已安装的 Microsoft Java 17。

3. 将当前整合包内容同步到隔离副本。下面每次镜像或删除前都确认路径是本次刚创建的专用副本；若分步执行时变量不存在，先从步骤 1 重新创建一个未占用的新目录，不要对已有目录执行：

```powershell
$twrRoot = 'D:\games\minecraft\Project TWR\TheWinterRescue'
$fhRoot = 'D:\games\minecraft\Project TWR\FrostedHeart'
if (-not $testInstanceCreatedByThisRun -or $testInstance -ne $expectedTestInstance -or -not (Test-Path -LiteralPath $testGame)) {
    throw 'Recreate and verify the dedicated test instance before mirroring or replacing files.'
}
robocopy (Join-Path $twrRoot 'kubejs') (Join-Path $testGame 'kubejs') /MIR
robocopy (Join-Path $twrRoot 'config\fhresearches') (Join-Path $testGame 'config\fhresearches') /MIR
$newJar = Join-Path $fhRoot 'build\libs\frostedheart-1.20.1-0.7.10.jar'
if (-not (Test-Path -LiteralPath $newJar)) { throw "Build the current FH jar first: $newJar" }
if (-not $testInstanceCreatedByThisRun -or $testInstance -ne $expectedTestInstance -or -not (Test-Path -LiteralPath $testGame)) {
    throw 'Recreate and verify the dedicated test instance before replacing its FH jar.'
}
Get-ChildItem -LiteralPath (Join-Path $testGame 'mods') -Filter 'frostedheart*.jar' | Remove-Item
Copy-Item -LiteralPath $newJar -Destination (Join-Path $testGame 'mods')
```

`/MIR` 移除或替换的只是已核实的本次新建测试副本中的目标文件，以便与当前 TWR 源码一致；不改源实例。保留从实例副本复制来的其他模组和配置。

4. 只从 PrismLauncher 启动 `The-Winter-Rescue-Research-Playtest`，创建一个使用新名称的单人世界。保留两个原实例及其中的玩家存档不变。检查启动日志确认载入新构建的 FH JAR、预期第三方模组和当前 TWR 正式数据。每个 M3/M6 阶段都重新核对当时正式仓 `config/fhresearches/`、`kubejs/` 中定义和脚本的实际内容，再据此检查运行时加载结果；81 项研究、57 个脚本仅为调查时基线，不是后续固定验收数。每个 M3/M7 检查点记录 FH commit/JAR 版本、TWR commit、Prism 实例名、新世界名、Forge/Java 版本、日志路径和实际观察结果。

5. 在新世界执行里程碑检查：通过正常生存流程获取材料；不使用管理员完成命令或创造模式发放关键物品，完成三个研究项目及至少一段下游生产；检查 TWR 配方与解锁，并在关键研究阶段验证退出重进后的保存状态。多人协作和服务器重启按 M7 使用独立测试服务器与两个隔离客户端验证。客户端启动或进入菜单本身不代表生存流程或持久化验收通过。

## 需记录的差异

| 项目 | `runClient` 开发运行 | 隔离的 TWR 整合包运行 |
|---|---|---|
| Minecraft / Forge | 按 FH `gradle.properties` 为 1.20.1 / 47.3.0 | 按所选 Prism 实例元数据为 1.20.1 / 47.4.0 |
| FH 代码 | 直接载入 Gradle 源集 | 将构建出的重映射 FH JAR 复制到测试实例 `mods/` |
| 第三方模组 | Gradle runtime 依赖，包括仅开发运行的 `clientRuntimeOnly` 模组 | 从 Prism 实例复制已安装的 TWR `mods/` |
| 研究定义 | `run/config/fhresearches` 调查时有 87 项开发定义 | 以 M3/M6 阶段 TWR 正式仓当前定义为准；81 项是调查时基线 |
| KubeJS | `run/kubejs/server_scripts` 只有 `example.js` | 以 M3/M6 阶段 TWR 正式仓当前脚本为准；57 个脚本是调查时基线 |
| 世界 | FH `run/` 工作目录，只保证开发数据 | 在隔离 Prism 副本中创建的新名称世界 |

## 缺少条件与交接

- FH `build/libs/` 当前没有 `0.7.10` JAR；主协调需先构建当前 M3/M7 代码，再在测试副本中替换旧 FH JAR。
- 未找到 Forge `47.3.0` 的本地 Prism 实例。当前发现的 TWR 开发候选为 Forge `47.4.0`；玩家验收应保留整合包实际版本，并在结果中明确记录版本差异。
- 所选实例是开发实例，不能证明已导出的发布包状态。调查时实例中的 TWR 数据比工作仓少一个脚本（`warm_stone.js`）；准备流程会将当前仓库的 `kubejs/` 和 `config/fhresearches/` 覆盖到隔离副本。M3/M6 每次交接时都要重新核对这两个目录，不沿用调查时的数量。
- 本次没有启动客户端或世界，也没有构建；材料路线、新内容加载和持久化尚未验证。当前交付仅为环境准备调查。
- 后续 M3/M7 步骤：使用进程级 Java 17 构建当前 FH JAR；创建并填充隔离 Prism 副本；创建新世界；检查启动日志及研究定义的实际加载；验证配方与材料可达性、项目推进；保存退出并重新进入同一隔离世界确认进度保留；随后完成 M7 双客户端、服务器重启和下游生产检查。

## Outcome

隔离实例已用于M3/M4/M5/M6正式客户端和M7双客户端验证；用户Development、旧存档与Prism网络保持原样。后续实际范围见[最终客户端记录](../diary/2026-11-12_20-12-00_research-m5-m6-client-validation.md)；完整生存仍是独立待办，不重做本环境调查。
