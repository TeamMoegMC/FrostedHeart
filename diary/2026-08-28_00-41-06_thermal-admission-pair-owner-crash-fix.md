# Thermal admission and pair-owner crash fixes

- Time: `2026-08-28 00:41:06 +08:00`
- Author: `Codex; OpenAI; primary implementation and investigation`
- Status: `completed`
- Scope: `MinecraftThermalInput`, `MinecraftThermalTopologyApplier`, thermal JUnit/GameTest, and climate living documentation

## Completed

- Fixed the server crash where a Page admitted during an open 20-tick mutation batch was retroactively added to that older cut with geometry revision `0`. The cut now contains only Page identities that actually executed `beginGeometryMutation()`, and publishes their captured positive revision.
- Fixed a second incremental-air-graph failure exposed by the admission regression fixture. Every real Brick replacement now rebuilds its own Air pair fragment and the `-X`, `-Y`, and `-Z` owner fragments before old arena slots retire.
- Added `pageAdmissionDuringMutationBatchKeepsItsCapturedRevision` and `interiorMixedReplacementRebindsNegativePairOwnersBeforeOldSlotsRetire` regression coverage.
- Updated the thermal architecture, lifecycle, validation matrix, and implementation-plan outcome. No `design/` file changed.

## Decisions

- Keep `ResolvedGeometryInputRing` validation strict; revision `0` is an invalid lifecycle state and must not be hidden by weakening the guard.
- Separate shape propagation from endpoint-identity propagation. Material surface/binding work still follows exact changed faces, while Air pair rebinding is bounded to at most four owner fragments per replaced Brick.
- Preserve the existing arena/fragment architecture. Full-Page rebuilds or permanent per-position cells would simplify references but reintroduce the CPU/allocation or retained-memory costs this runtime is designed to avoid.

## Validation

- Independent Luna Max: `.\gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain` succeeded with `190/190` tests, zero failures/errors/skips; the new mixed replacement test executed and passed.
- Independent Luna Max: `.\gradlew.bat runGameTestServer --offline --console=plain` succeeded with all `13/13` required tests; the new mutation-admission batch executed and passed.
- The successful GameTest run contained none of `generation/tick must be non-negative and revision positive`, `removed air cell still owns adjacency`, or `air adjacency references invalid cells`.
- `git diff --check` passed for the implementation, tests, and living documents. The initial unified JUnit attempt only exposed a missing test import; after correcting it, the complete thermal suite passed.

## Remaining

- Crash correctness work is complete. The controlled post-change JFR and cache decision remain separate performance-plan work; the pair fix itself adds no Page/arena scan and caps additional rebinding at three negative-axis fragments plus the replaced Brick's own fragment.
