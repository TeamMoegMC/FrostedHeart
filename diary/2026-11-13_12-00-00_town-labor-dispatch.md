# Town Labor Dispatch Agency

- Author: Codex
- Date: 2026-11-13
- Status: implementation complete; gameplay validation pending

Added `frostedheart:labor_dispatch` and persisted `laborDispatch` building type, room scanner, menu and information screen. Uses existing resident staffing and eligibility; valid rooms need a door, area >= 9 and volume >= 18. Capacity is floor(area / 4). Each eligible assigned resident grants one `LaborTypes.WORKER`; `TeamTownData.tick` recomputes the team's extra worker provider so removal, invalidation and roster changes do not accumulate stale daily output. Physical Labor providers remain additive. The town owns the extra WORKER provider channel.

Added block models, loot, town/axe tags and Chinese/English labels; reused transport station texture as a placeholder. No companion pack recipe changes.

Documentation: added `docs/town/labor-dispatch.md` and linked it from the town index. `diary/README.md` was absent when inspected.

Validation: offline `compileJava` succeeded (21 pre-existing deprecation/mixin warnings); `git diff --check` passed; all eight new JSON resources parsed. Compilation required approved access to the Gradle cache outside the workspace. No client/server gameplay, save/reload or allocation GameTest was run; these remain manual validation items.
