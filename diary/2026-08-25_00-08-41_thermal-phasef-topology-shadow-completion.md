# Phase F Minecraft topology and shadow completion

- Time: `2026-08-25 00:08:41 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `thermal Minecraft full resnapshot, topology application, source-span retention, and dormant shadow dispatch`

## Completed

- Completed the dormant Phase F frame-to-topology path: incremental events and frozen `int[4096]` full-Page snapshots rebuild regular, mixed, or no-air arena coverage and install the replacement sweep before acknowledging geometry input.
- Added independently bounded full-snapshot ownership and exact section identity, lifecycle generation, revision, and reason matching for full-resync installation; excess Pages retain their sticky requirement and retry later.
- Narrowed topology replacement blocking to source ports that actually reference the replaced arena span. Unrelated sources no longer prevent Page rebuild, while affected ports still require a Phase G rebind.
- Added latest-only shadow dispatch through a caller-provided bounded executor. Main-thread work stops after capture, sealing, and mailbox replacement; topology compilation and solving run through `ThermalRuntimeCoordinator` on the worker.
- Kept normal gameplay dormant and legacy-authoritative. No gameplay query, physical source profile, source producer, or FarField transport was enabled.

## Decisions

- Full resync carries a frozen signature cut instead of asking a worker to read Minecraft world state.
- Source rebind remains explicit. Existing generator and endpoint values are not assumed to be SI watts, and nearest-cell rebinding is not invented without Phase G port geometry.
- Moving Create structures remain air while moving; static `hasDynamicShape=false` states remain automatically trusted; unsupported dynamic geometry remains conservatively unresolved.

## Validation

- `gradlew.bat test runGameTestServer --offline --no-daemon --console=plain`: passed.
- JUnit XML: thermal `216/216`, repository `744/744`, with zero failures, errors, or skips.
- Forge GameTest: `16/16` required tests passed, including both dormant Minecraft thermal input tests.

## Remaining

- Phase G: define gameplay-calibrated Campfire and Generator power profiles and concrete emission-port geometry, then implement affected-source rebind and unload settle.
- Approve FarField profiles from production-like evidence, collect production workload CPU/heap evidence, and only then connect the gameplay query compositor.
