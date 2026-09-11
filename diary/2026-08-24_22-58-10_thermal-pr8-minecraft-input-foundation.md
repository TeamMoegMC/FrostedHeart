# PR8 Minecraft thermal input foundation

- Time: `2026-08-24 22:58:10 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `partial`
- Scope: `thermal runtime Minecraft mutation capture, interest ownership, primitive input transport, frame sealing, lifecycle documentation`

## Completed

- Added an always-applied `LevelChunkSection` mutation route with an unowned null-owner fast path while preserving the property-gated Phase 0a evidence probe.
- Added explicit loaded-section interest ownership, all-air Page admission, main-thread loaded-only dependency capture, frozen shared signature IDs, a fixed primitive resolved-input ring, and tick-end five-stream frame sealing.
- Added chunk unload, section replacement, and raw block-container invalidation handling. Moving Create structures remain air while moving; unsupported dynamic states remain unresolved.
- Kept the runtime dormant: no gameplay query replacement, no second `H/C` store, and no non-source ACK before topology is rebuilt.
- Cached enum decode tables in `ResolvedGeometryInputRing.poll()` so its caller-owned result path does not allocate cloned `values()` arrays.

## Decisions

- Resolver output crosses the worker boundary only as primitive signature IDs from one immutable shared registry; IDs are not interned in per-dimension mutation order.
- Ring overflow preserves a Page-owned sticky full-resync requirement and does not advance a false watermark.
- Production activation remains gated by Phase 0b evidence and approved FarField profiles.

## Validation

- `gradlew test runGameTestServer --no-daemon --console=plain`: passed; JUnit `736/736`, thermal JUnit `208/208`, Forge required GameTest `16/16`.
- The two PR8 GameTests exercise the real section Mixin, self and neighbor dependency invalidation, primitive signature transport, frame sealing, `INPUTS_PENDING`, and chunk detach.
- `git diff --check`: passed after the final code and documentation edits.

## Remaining

- Implement one topology applier that consumes primitive geometry/chunk inputs, rebuilds `ThermalCellArena` spans and pair/boundary operations, then advances the explicit non-source ACK.
- Add arena/source release for page and chunk unload, and activate coordinator dispatch only in shadow mode after the existing gates pass.
- Phase 0b production-like multiplayer/server evidence and production-approved FarField profiles are still missing; legacy temperature remains the only gameplay authority.
