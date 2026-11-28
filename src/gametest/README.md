# Forge GameTest sources

This directory contains reusable Forge GameTest cases, test-only observers, catalogue fixtures and resources. They use the `gameTest` source set and are excluded from the ordinary production JAR.

Production source and data under `src/main/` and `src/generated/` define implemented behaviour. These tests describe the checks they perform; successful cases do not prove untested loads.

Start with the relevant system in [docs/README.md](../../docs/README.md). The `gameTest` source set and `runGameTestServer` task are configured in [build.gradle](../../build.gradle); temperature behaviour and configuration are described in [the thermal runtime contract](../../docs/climate/thermal-runtime-control.md).

Thermal tests retain input/source ownership, query and save lifecycle, quality recovery, gameplay integration, and current two-base/scattered/overload fixtures. One-off recording clients, retired capacity variants and duplicate entry wrappers were removed; their history is recorded in [the thermal consolidation](../../diary/2026-10-29_23-38-00_thermal-development-history.md#retired-validation-tools). Other systems' test code and fixture data are separate from that cleanup.

Use Java 17 and compile the test source set with `./gradlew compileGameTestJava` (`.\gradlew.bat compileGameTestJava` on Windows). For example, select the compact-storage checks with `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_compact_storage`. Runtime worlds, logs and profiling recordings are generated outputs and stay outside Git; test source and deliberate fixtures stay versioned.

For town staffing and housing queues, use `-I Scripts/town-scheduling-validation.init.gradle` and namespace `frostedheart_town_scheduling`. The same init file provides `runTownSchedulingClient`, which creates a disposable world and checks the rendered Mayor's Seal with actual management packets and mouse events; its JSON result must pass for the task to succeed. The [town synchronization reference](../../docs/town/persistence-and-runtime.md#验证入口) owns the scenarios, evidence and controlled temperature.

That client entry also calls `TownEditorClientChecks` for controlled component reply sequences. The returned `editorReplyChecks` list records completed checks; earlier result files without that field do not cover them. These added checks are prepared but unrun as of 2026-11-09, following the user's instruction not to build this change.

Player heating equipment checks use namespace `frostedheart_equipment_heating`:
real item capabilities, sustained body curves, stacked devices, fuel cadence and
reload, vest demand, reservoir energy conservation and the actual player tick.
Their gameplay contract and validation scope belong to
[player temperature](../../docs/climate/player-temperature.md).

Knowledge observation checks use namespace `frostedresearch_observation`:
`./gradlew runGameTestServer -PgameTestNamespaces=frostedresearch_observation`.
They cover shared item sampling, 30/60-tick sessions, non-consuming inventory
inspection and interruption when targets, tools or menus change.
`./gradlew runObservationUiClient` creates a fresh local world and uses Minecraft's
keyboard/mouse callbacks with a test-only held-input device to exercise world and inventory observation, including
Shift-right-click and creative inventory. It focuses its own client window,
writes screenshots and `result.json` to `build/observation-ui-validation/`, then
exits and restores the options it changed. These client checks need a desktop
session; they do not verify physical OS input delivery. Player controls are described in
[observations and notes](../../docs/knowledge/observations-and-notes.md).

For the shared thermal comparison baseline, run [`scripts/run-thermal-baseline.ps1`](../../scripts/run-thermal-baseline.ps1). The [baseline reference](../../docs/climate/thermal-validation-baseline.md) owns its scene list, recorded configuration, clocks and result interpretation.

Project access checks use namespace `frostedresearch_m1` (`ProjectAccessGameTests`), with deliberate fixtures under
`resources/m1-fixtures/`. They exercise disclosure, learned-only prerequisites, author exceptions, result/observation
entry, legacy projects, observation uses, strict decoding and v2/v3 state round trips. Keep runtime directories under build.

M2 behavior and contact checks use `frostedresearch_m2` (`IdeaAcquisitionGameTests`), including real item pickup,
crafting/furnace/container extraction, use/interaction bridges and repeated observation sessions; fixtures live under resources/m2-fixtures/.

M3-A trial kiln and physical submission checks use `frostedresearch_m3` (`TrialKilnGameTests`), a separate `m3_trial_kiln` batch
and trial_kiln_empty structure. They cover real loaded ticks, three ratios, lifecycle/settlement, sample binding and schema4 progress. The formal project now compares three fired-item observations: current-run binding, separate cracked-mixture identities, no consumption/archive, progress persistence, and upgrading the former single-sample run are checked alongside the retained sample-submission example.

`LegacyResearchBridgeGameTests` (`frostedresearch_m3b`) uses separate synchronous batches for legacy import/retry,
new-run prerequisite and permission projection, replaced-entry guards/removal recovery, knowledge/receipt merge,
formal companion JSON codecs/references, and zero-online final recipe reload/start lifecycle.
It uses actual server teams, RecipeManager and IE multiblock registration. Codec round trips are persistence checks,
not an independent-process restart claim.

`src/gametest/resources/m3b-formal-fixtures/` contains the five earthen-smelter knowledge JSON files copied verbatim
from the companion pack on 2026-11-12, with link/project fixtures refreshed on 2026-11-14 for codec/reference checks. Their stable recipe ID is asserted; the test does
not claim that a full KubeJS pack runtime registered that recipe. Keep fixtures aligned when the formal contract changes.

M4 stage checks use `frostedresearch_m4`. Real bellows integration additionally requires `-Pm4CharcoalPitTests`
and namespace `frostedresearch_m4_cp`; its sources and formal fixtures are isolated under `src/m4/`.
The two-process `frostedresearch_m4_restart` check uses `-Pm4RestartPhase=write` then `read` with the same
`-PgameTestDirectory`. Exact commands, verified results and startup-failure pitfalls are recorded in the
[M4 handoff](../../diary/2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md).

M5 controlled-furnace checks use `-Pm5Tests` and namespace `frostedresearch_m5`;
This includes the real Charcoal Pit runtime because formal project access now requires a sandy brick observation; see the [M5 handoff](../../diary/2026-11-12_18-10-45_research-m5-controlled-furnace-handoff.md) for the minimum run command and separate-process checks.

M6 merged-run checks use `frostedresearch_m6`; device selection checks use `frostedresearch_m6_devices` from
`src/m5/`, enabled by `-Pm5Tests`. This switch also loads Charcoal Pit for the formal sandy-brick observation prerequisite. With Java 17, the minimum Windows command is:

```powershell
.\gradlew.bat runGameTestServer -Pm5Tests '-PgameTestNamespaces=frostedresearch_m6,frostedresearch_m6_devices' '-PgameTestDirectory=build/m6-gametest'
```

The [merged-run contract](../../docs/knowledge/state-and-api.md#m6-合并记录排序与备用进度) owns the rules;
exact validation scope and evidence belong in the [M6 handoff](../../diary/2026-11-12_19-32-38_research-m6-merged-runs-final-handoff.md).

Client checks use the opt-in `runM3cClient` task or the separate `m3cValidationJar` helper for an isolated full-pack
instance. Normal `jar` excludes the helper. See the [M3-C handoff](../../diary/2026-11-12_16-58-51_research-m3c-client-prism-handoff.md)
for setup, evidence paths and the distinction between callback tests, full-pack loading and physical input.

The opt-in M4 client helper also supports `-Dfrostedresearch.m4Validation=true -Dfrostedresearch.m4DeskOnly=true`.
It creates a fresh full-pack fixture world, opens the formal project, excavates airflow through the actual screen's press/drag/release handlers,
checks Esc/button return and retained board state, then exits. It covers the airflow stage only, not the later
prototype trial or survival materials. Output is `<frostedresearch.m4Root>/build/m4-client/`.

For a running Prism launcher, the helper also accepts an instance-local `research-client-m4-desk-check.txt` in the game directory.
Its UTF-8 content is the absolute output root; it enables the M4 desk-only check without changing JVM arguments.
Remove this temporary opt-in and the helper JAR after checking. Ordinary production JARs include neither the helper nor its opt-in loader.
`AirflowExcavationGameTests` covers diggable side bricks, the unmarked protected inlet and starter pocket,
cold chimney/outer-wall leaks, whole-batch failure priority, hot-only vents, partial/failed/completed saves,
format-2 migration and particle collision/warming against bricks. `AirflowParticleGameTests` additionally checks enlarged
colliders, pair momentum/energy and separation, volume-scaled births in disconnected excavations, and bounded long-running
clouds with nearby-pair filtering. It also checks quarter-cell density, fixed-speed inlet/charcoal/outlet flow after completion,
heating, recycling without interpolation streaks, completed-board reopening, reset to random motion and the empty preset furnace before ventilation.
Its `AIRFLOW_PARTICLE_PROFILE` log records computational timing without asserting machine-specific speed.
The M4 helper checks real client population growth, contact counters and completed fixed-path flow as well as both failures, resets and completion.
Before synthetic Screen press events, it updates the drawing desk's hover cache at the event coordinates, matching normal render preparation independently of the physical cursor.
Formal Charcoal Pit tests continue through prototype access and blowing; those integration tests were not rerun for this visual-only change.

Matching puzzle checks use `frostedresearch_matching` (`MatchingDifficultyGameTests`):
`./gradlew runGameTestServer -PgameTestNamespaces=frostedresearch_matching`.
They play 256 seeds for each of the eight difficulties (2048 boards) through ordinary legal moves after NBT restoration, including the final tile; no `forceWin` is used. Separate cases force the bounded fallback, check the full difficulty comparison matrix and legacy tool tiers, load the shipped opt-in debug project to verify discovery, per-task credits and persistence, and exercise actual desk starts, charcoal pencil durability, paper consumption and ordinary win credit. The player route is documented under [debug research](../../docs/research/gameplay-and-integrations.md#debug-research).

Idea-link puzzle checks use the existing `frostedresearch_m2` namespace. They now verify route-specific auxiliary groups, auxiliary tray selections (including inherited/overridden groups, tags and overlapping predicates), mandatory-only session witnesses, 0–7 number boards, ordinary win settlement, fixed difficulty and reset recalculation, no timer payload, cancellation/close/logout, required-knowledge changes and already-received outputs. Old timing claims in historical diaries describe the previous implementation.
`LinkClientValidation` is opt-in through an instance-local `research-client-link-check.txt` output-root marker and the temporary `m3cValidationJar`. It creates a fresh flat fixture world and uses actual Screen events/C2S for a raw-copper + sand-auxiliary tray candidate, original 387×203 drawing desk and shared MainGamePanel, Esc cancellation, reset after auxiliary knowledge, normal final click, return to the journal and preservation of an already-running legacy board. Production JARs exclude this driver.

The focused generator heating checks use `-PgameTestNamespaces=frostedheart_generator_heat`.
They cover the smooth center boost, unchanged boundary, overlap, player/crop/town/phase queries,
infrared updates, item caches, real T1/T2 production, four horizontal outlets across Chunk boundaries,
blocked/reopened outlets, physical enclosure heating, phase acknowledgements and tower lifecycle.
The real T1/T2 cases also check per-target power at temperature levels `10` and `1`,
fixed `2,000 W` radiation and discovery of both generator profile IDs. The formed
T2 warm-up check compares an exact steam dose with a short dose.
The [temperature composition reference](../../docs/climate/world-climate-and-temperature.md#7-analytic-control-fields)
owns the formula and units.

The heat-network check uses `-PgameTestNamespaces=frostedheart_heat_pipe_revalidation,frostedheart_t2_pressure`.
It covers fallback visits on real pipes, merge/removal/reconnect callbacks,
an actual absent/reloaded adjacent chunk, and the independent T2 pressure ledger.

For infrared transport and actual GL display, run [`Scripts/run-infrared-network-e2e.py`](../../Scripts/run-infrared-network-e2e.py) with Java 17. It starts an isolated dedicated server, two rendering clients and an ordered TCP delay/rate proxy; the [infrared reference](../../docs/climate/infrared-view.md) owns its scenarios, invocation and verification limits.

The memory-lifecycle regressions use namespaces `frostedheart_preparation_retention`,
`frostedheart_geometry_input_order`, `frostedheart_checkpoint_boundary`, and
`frostedheart_persistence_overload`; `frostedheart_concurrent_save` covers accepted
and unaccepted input during a running solve. Their separate GameTest batches permit a
combined run without closing another test's live thermal runtime. They check
collection of completed builders before closing a heated world, actual adjacent
Page publication order when fair selection first visits the later input, incoming
geometry holds during unload-job cleanup, connecting Pages that are announced but
not yet installed, and a real flushed chunk save with every
thermal worker paused. The last test also reads disk NBT to check pending heat and
the changed block's material identity.

These focused checks complement the ordinary dedicated-server source/chunk
workload in [`Scripts/run-thermal-oom-repro.py`](../../Scripts/run-thermal-oom-repro.py).
The [memory evidence reference](../../docs/climate/thermal-server-memory-evidence.md)
records the confirmed reference defect, heap analysis, configuration differences,
and limits on attributing the original remote OOM.


Town connectivity and citizen road navigation checks use namespaces `frostedheart_town_connectivity,frostedheart_citizen_roads`:
`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_town_connectivity,frostedheart_citizen_roads -PgameTestDirectory=build/citizen-road-gametest`.
The 28 cases cover component policy, entrance radii, building-port transfers, server-side vertical movement and dynamic obstacles,
local support/headroom checks, ladders, stairs and packet metadata. With the opt-in agent client bridge, create a disposable empty
singleplayer town and use `roads setup`, `roads populate`, `roads status` for a rendered fixture and observations.
The [citizen rendering contract](../../docs/town/citizen-rendering-at-scale.md#客户端道路站位与攀爬2026-11-13) owns the behavior and validation limits.


Section snapshot TTL checks use `frostedheart_section_cache`. Run it with the existing road suites:
`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_town_connectivity,frostedheart_citizen_roads,frostedheart_section_cache -PgameTestDirectory=build/town-section-cache-gametest`.
The deterministic clock cases cover fresh/grace/hard-expiry boundaries, dirty deadlines, FIFO deduplication, coalesced cold readers,
capacity eviction, invalidation, failure and rollback. Two real-level cases exercise the state-write mixin and section-edge dirty propagation.

温度传感器验证使用命名空间 `frostedheart_temperature_sensor`，入口：
`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_temperature_sensor -PgameTestDirectory=build/temperature-sensor-gametest`。
覆盖真实世界中的阈值等号、反转、六面强/弱输出、负小数配置 NBT 恢复和定时重算；GUI 需客户端验收。

作物肥料验证使用命名空间 `frostedheart_fertilizer`：
`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_fertilizer -PgameTestDirectory=build/fertilizer-gametest`。
覆盖生长增产记账、成熟后施肥、预览、NBT 恢复、重置清理、多层根部及 Create 公共破坏掉落路径。行为与验证边界见[作物肥料](../../docs/climate/crop-fertilization.md)。

独立栽培橡树原型验证使用 `frostedheart_cultivated_oak` 命名空间；配置、试用方式和验证范围见[栽培橡树](../../docs/climate/cultivated-oak.md)。
