# Agent client bridge: its worlds open in a plain dev client

- Time: `2026-11-09 21:15:09 -0600`
- Author: `Claude; Anthropic/claude-opus-5-5; dev tooling`
- Status: `completed`
- Scope: `Scripts/dev_client_bridge.init.gradle`, `Scripts/agent_client.py`

## Completed

- Worlds created or opened by the bridge client from [the bridge entry](2026-11-09_02-34-24_agent-client-bridge.md) could not be loaded by a plain `./gradlew runClient`: `Failed to load level data or datapacks` / `Failed to get element ResourceKey[minecraft:worldgen/biome / frostedheart_deposits:old]`. Two such saves (`institute-fresh`, `institute-test`) had already been repaired by hand by deleting the `frostedheart_deposits:*` entries from `level.dat`.
  - Cause: the init script added the whole `gameTest` source set to the client's `frostedheart` mod. ForgeGradle puts each mod source's resources directory into `MOD_CLASSES`, so `src/gametest/resources` became part of the mod's data: the five test dimensions `frostedheart_deposits:{ordinary,new,core,control,old}`, the `frostedheart_deposits_legacy` and `the_winter_rescue` test worldgen, and the `minecraft` `flat` world preset override. Minecraft stores the loaded dimensions in `level.dat` (`Data.WorldGenSettings.dimensions`).
- `Scripts/dev_client_bridge.init.gradle` now creates its own `agentBridge` source set (`src/gametest/java` filtered to `com/teammoeg/frostedheart/devtools/**`, no resources, compiled against `main` the same way as `gameTest`) and adds only that set to the client run. This replaces the earlier entry's "adds the `gameTest` source set". `build.gradle` and the `gameTest` source set are unchanged. `AgentClientBridge.java` stays where it is and is still compiled into `gameTest` too, where it stays inactive (client-only, behind `-Dfrostedheart.agentBridge=true`).
- The `Scripts/agent_client.py` docstring now says that the bridge client has the same mods and data as a plain `runClient`.

## Decisions

- A filtered source set defined in the init script, not a move of the bridge to a new `src/<set>/java` directory. This needs no `build.gradle` change, the IDE still sees the file as `gameTest` code, and existing links stay valid. ForgeGradle 6.0.6 `ModConfig` accepts only whole source sets, so "only the gameTest classes" would also have needed a source set.
- The script has no explicit `dependsOn`. ForgeGradle makes each run task depend on the `classes` task of every mod source (`RunConfigGenerator.createRunTask`). `runClient -I Scripts/dev_client_bridge.init.gradle -m` lists `compileAgentBridgeJava` and `agentBridgeClasses` and no `gameTest` task; the old script's graph had `processGameTestResources`.
- If a later `devtools` class needs other `gameTest` classes, `compileAgentBridgeJava` fails. Then widen the include or move the shared code; do not put the `gameTest` set back on this run.

## Validation

Everything ran in worktree `strange-galileo` (HEAD `ad030056e` plus this change), using its own `run/` with `options.txt` copied from the main checkout. The main checkout's `run/saves` was only read.

- `./gradlew compileGameTestJava agentBridgeClasses -I Scripts/dev_client_bridge.init.gradle` built in 50 s. `build/classes/java/agentBridge` holds only the four `AgentClientBridge` classes, and there is no `build/resources/agentBridge`.
- Baseline with the old script: `newworld bridge-old`, then `quit`. `/forge dimensions` listed the five `frostedheart_deposits:*` levels, and `level.dat` stored 8 dimensions. Opening is enough on its own: a copy of a clean three-dimension world, opened with `world` and then `quit` under the old script, also ended with 8.
- New script: the client's `MOD_CLASSES` for `frostedheart` contained `noDatagen`, `main`, `client` and `agentBridge` only. Commands run: `newworld bridge-new`, `newworld bridge-new-normal normal 42`, `world bridge-new` (reopened), `quit`. `/forge dimensions` listed only `minecraft:overworld`, `the_nether` and `the_end`, and both `level.dat` files store exactly those three. The old session's log had 11 lines mentioning `frostedheart_deposits` and the new one had none. Both sessions logged the same 19 distinct ERROR messages, all present before this change: recipes for optional mods missing in dev (`supplementaries`, `patchouli`), FTB Teams' dev player id, and `Scenario init not found`.
- Plain client with no init script: `./gradlew runClient --args="--quickPlaySingleplayer bridge-new"`. Gradle's `--args` stay ahead of the arguments ForgeGradle adds at launch, so vanilla Quick Play opens a save without an init script. The integrated server started, `Dev` logged in at the bridge's spawn `(0.5, 64, 0.5)` and the thermal runtime started. After SIGTERM, the client's shutdown hook saved all three dimensions, and `level.dat` still stores three.
- Negative control, the same plain launch on `bridge-old`: `Failed to load level data or datapacks, can't proceed with server load` and `Failed to get element ResourceKey[minecraft:worldgen/biome / frostedheart_deposits:old]`, the reported error.
- `-Dforge.enableGameTest=true` appears in both plain and bridge client processes. It is a Forge userdev default, not a sign that the `gameTest` set is loaded.
- A read-only scan of the main checkout's `run/saves` (91 saves with a `level.dat`) found no remaining non-vanilla dimension.
- `./gradlew runGameTestServer` with the default namespaces ran 90 tests (whole task 3 min 37 s): 86 passed and 4 required thermal tests failed. The four are `waterinteriorwaitsforanedge`, `sensibleairandmaterialusethesameclockwithoutsavedrift` (`only v5 is written`), `warmedmaterialremainsvisibleaftertheruntimecloses` and `campfiresurfaceusesmaterialandlocalavailability`. The second was already recorded as failing in [2026-09-23](2026-09-23_18-50-58_legacy-clothing-exchange-production.md) and [2026-10-09](2026-10-09_18-13-27_reservoir-registrate-datagen-fix.md), and the fourth on 2026-09-23 with a different message. This change cannot affect the run: it touches only `Scripts/` and `diary/`, the init script applies only with `-I`, and there is no `~/.gradle/init.d`.

## Documentation

- Updated: the header comment of `Scripts/dev_client_bridge.init.gradle` and the `Scripts/agent_client.py` docstring.
- Unchanged because they are still accurate: the bridge sentence in `README.md`, and the interactive-client bullet in `.claude/memory/architecture.md` (ignored by Git). The earlier bridge entry is left as written.

## Remaining

- `researchUiClient` in `build.gradle` still adds the whole `gameTest` set to a client that works in `run/`. Each run creates a new `run/saves/research-ui-validation-<run>`. With the same mod sources as the old bridge, that world would also store the test dimensions (not run here). These are throwaway validation worlds and are not changed here.
- `Scripts/ice_cave_client_validation.init.gradle`, `Scripts/infrared-network-e2e.init.gradle` and `Scripts/town-scheduling-validation.init.gradle` also give client runs `gameTest` code, but they use their own working directories under `build/`, so their worlds do not reach `run/saves`.
- Two of the four GameTest failures at `ad030056e`, `waterinteriorwaitsforanedge` and `warmedmaterialremainsvisibleaftertheruntimecloses`, are not recorded in earlier diaries. They were not investigated here.
