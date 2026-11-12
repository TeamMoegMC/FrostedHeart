# Forge GameTest sources

This directory contains reusable Forge GameTest cases, test-only observers, catalogue fixtures and resources. They use the `gameTest` source set and are excluded from the ordinary production JAR.

Production source and data under `src/main/` and `src/generated/` define implemented behaviour. These tests describe the checks they perform; successful cases do not prove untested loads.

Start with the relevant system in [docs/README.md](../../docs/README.md). The `gameTest` source set and `runGameTestServer` task are configured in [build.gradle](../../build.gradle); temperature behaviour and configuration are described in [the thermal runtime contract](../../docs/climate/thermal-runtime-control.md).

Thermal tests retain input/source ownership, query and save lifecycle, quality recovery, gameplay integration, and current two-base/scattered/overload fixtures. One-off recording clients, retired capacity variants and duplicate entry wrappers were removed; their history is recorded in [the thermal consolidation](../../diary/2026-10-29_23-38-00_thermal-development-history.md#retired-validation-tools). Other systems' test code and fixture data are separate from that cleanup.

Use Java 17 and compile the test source set with `./gradlew compileGameTestJava` (`.\gradlew.bat compileGameTestJava` on Windows). For example, select the compact-storage checks with `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_compact_storage`. Runtime worlds, logs and profiling recordings are generated outputs and stay outside Git; test source and deliberate fixtures stay versioned.

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
and trial_kiln_empty structure. They cover real loaded ticks, three ratios, lifecycle/settlement, sample binding and schema4 progress.

`LegacyResearchBridgeGameTests` (`frostedresearch_m3b`) uses separate synchronous batches for legacy import/retry,
new-run prerequisite and permission projection, replaced-entry guards/removal recovery, knowledge/receipt merge,
formal companion JSON codecs/references, and zero-online final recipe reload/start lifecycle.
It uses actual server teams, RecipeManager and IE multiblock registration. Codec round trips are persistence checks,
not an independent-process restart claim.

`src/gametest/resources/m3b-formal-fixtures/` contains the five earthen-smelter knowledge JSON files copied verbatim
from the companion pack on 2026-11-12 for codec/reference checks. Their stable recipe ID is asserted; the test does
not claim that a full KubeJS pack runtime registered that recipe. Keep fixtures aligned when the formal contract changes.

M4 stage checks use `frostedresearch_m4`. Real bellows integration additionally requires `-Pm4CharcoalPitTests`
and namespace `frostedresearch_m4_cp`; its sources and formal fixtures are isolated under `src/m4/`.
The two-process `frostedresearch_m4_restart` check uses `-Pm4RestartPhase=write` then `read` with the same
`-PgameTestDirectory`. Exact commands, verified results and startup-failure pitfalls are recorded in the
[M4 handoff](../../diary/2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md).

Client checks use the opt-in `runM3cClient` task or the separate `m3cValidationJar` helper for an isolated full-pack
instance. Normal `jar` excludes the helper. See the [M3-C handoff](../../diary/2026-11-12_16-58-51_research-m3c-client-prism-handoff.md)
for setup, evidence paths and the distinction between callback tests, full-pack loading and physical input.
