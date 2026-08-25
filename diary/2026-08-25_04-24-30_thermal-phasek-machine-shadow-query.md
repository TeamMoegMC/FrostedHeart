# Thermal Phase K registered-machine shadow query

- Time: `2026-08-25 04:24:30 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `registered-machine passive query, shared publication lookup, shadow metrics, climate documentation and thermal implementation plan`

## Completed

- Extracted the existing player Air Mesh lookup into one private published-air path and renamed its caller-owned result to `MutableEnvironmentSample`; player composition and legacy authority are unchanged.
- Added `sampleMachineEnvironment` for an explicitly declared receiver point and `observeRegisteredMachineEnvironment` for a future real `QUERY_ONLY` machine, with bounded hit/miss and legacy/new comparison metrics in `MachineShadowSnapshot`.
- Kept machine lookup passive: it does not request player radiation, scan BlockEntities, load chunks, admit Page interest, mutate arena energy, advance runtime state, or touch the legacy heat network.
- Extended the existing Minecraft input GameTest with admitted publication reads, stale fallback, 64 passive misses without Page growth, comparison accounting, and thermal state immutability assertions.
- Updated living climate documentation and the Phase K implementation snapshot.

## Decisions

- No current ordinary machine reads `WorldTemperature.air/block`, so no unrelated gameplay consumer was fabricated. The new observation entry point remains infrastructure-ready until a real environment-sensitive machine calls it at its existing cadence.
- Ordinary machines remain `NONE`; Campfire and Generator remain on their existing `POWER_SOURCE` path and must not also use the machine observation path.
- Machine queries reuse exact regular/mixed Air Mesh lookup but do not inherit player radiation semantics automatically.

## Validation

- `.\gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` passed.
- Repository JUnit: `809/809`; thermal JUnit: `236/236`; Forge GameTest: `19/19` required.
- `git diff --check` passed.

## Remaining

- Continue Phase K in the frozen order: crop, town, then HUD consumers.
- Keep all player and machine legacy gameplay authoritative until production-like Phase 0b, FarField approval, and gameplay/reference calibration gates pass.
