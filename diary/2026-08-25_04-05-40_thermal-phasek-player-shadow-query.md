# Thermal Phase K player shadow query

- Time: `2026-08-25 04:05:40 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `thermal production query composition, mixed-Brick point lookup, legacy player shadow observation, climate documentation and thermal implementation plan`

## Completed

- Added a caller-owned player environment sample that composes admitted Air Mesh publication data and optional Phase J radiation while exposing the unavailable surface channel explicitly.
- Added exact quarter-block mixed-component lookup through the existing compiled topology; a mixed support ref is never treated as the queried air cell.
- Added runtime and Page envelope checks around the seqlock read, publication age enforcement, and explicit no-Page/no-air/stale/miss flags; the Air Mesh branch performs no worker waits, world reads, chunk loads, or mesh admission, while optional radiation remains an independent bounded loaded-only DDA.
- Connected `TemperatureUpdate` at its existing environment cadence to record bounded legacy/new absolute-temperature comparisons without modifying `PlayerTemperatureData` or any gameplay output.
- Added JUnit and Forge GameTest coverage for topology advance, mixed air, solid microcells, passive misses, and stale publications; updated living climate documentation and the Phase K implementation snapshot.

## Decisions

- Player migration remains observational until production-like Phase 0b, FarField approval, and gameplay/reference calibration gates pass.
- Regular Brick queries use the O(1) Page coverage path; only mixed Brick queries consult the compiled component index.
- Missing radiation and the unimplemented contact-surface compositor are separate flags, not fabricated heat contributions.

## Validation

- `\.\gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` passed.
- Repository JUnit: `809/809`; thermal JUnit: `236/236`; Forge GameTest: `19/19` required.
- `git diff --check` passed.

## Remaining

- Continue Phase K in the frozen order: machine, crop, town, then HUD consumers.
- Keep legacy player authority until calibration gates pass; implement the local surface compositor before interpreting `surfaceFluxW` as available.
