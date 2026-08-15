# Citizen position save integrity and data audit

- Time: `2026-08-14 22:30:04 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `hybrid citizen simulation dirty tracking, deletion consistency, id/UUID recovery, persistence documentation`

## Completed

- Fixed the restart position rollback: persisted movement, height, yaw, behavior state, target, and home-anchor changes now mark their backing `SavedData` dirty. Normal server stop also marks every adopted container before the runtime scheduler is cleared, so the stop-save captures the final authoritative snapshot.
- Routed all removals through a data-level operation that removes the SoA entry, clears runtime caches, and marks persistence dirty together. New entries are registered immediately, removing the previous 20-tick window in which a just-created resident could not be found or removed; `/fhcitizen clear` now persists after an earlier autosave.
- Calibrated each level's persisted id allocator against all existing player-town, AI-town, and unmanaged ids before first adoption, preventing id reuse after a partial cross-`SavedData` save. `TownSimData` now also persists its last active dimension, so a dimension change spanning a restart rebuilds per-level session ids from durable resident UUIDs.
- Hardened load recovery: duplicate stable ids and duplicate managed UUIDs within one SoA container are discarded; cross-container id collisions are reassigned without losing position/state; invalid states fall back to `IDLE`; half-written UUIDs become unmanaged sentinels; malformed `lastAIId` no longer rejects the global file; and AI resident map keys are normalized to the resident payload UUID.
- Replaced lossy `uuidHi ^ uuidLo` reconciliation keys with full `UUID` equality, preventing rare ghost retention on XOR collisions. Updated `docs/hybrid-simulation-architecture.md` to document the actual persistence contract.

## Decisions

- Dirty marking remains an in-memory boolean operation; disk writes still use Minecraft's built-in 6000-tick autosave and stop-save. This preserves final positions without adding per-tick disk I/O or a custom save scheduler.
- `tradeData` and `nameCache` remain runtime-only. Names are reconstructed from authoritative `Resident` data, and entity-less trade currently exposes only an empty-policy interface, so persisting those maps would add an unneeded second identity store.
- Stable ids remain per-level session/wire identities; resident UUIDs remain the durable town identity across dimension rebuilds.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimPersistenceTest` passed with new corruption and dirty-removal coverage.
- `./gradlew test` passed.
- `./gradlew build` passed; only the repository's existing duplicate-resource and license warnings were reported.
- `git diff --check` passed before the diary entry.

## Remaining

- Perform an in-game smoke test: let a managed resident walk well away from its house, save and quit, reopen the same world, and verify exact position/state continuity; repeat once for an unmanaged command resident.
- `int` fixed-point coordinates inherently cover only about ±2,097,151 blocks. Supporting citizens beyond that radius would require a deliberate save/network format migration to wider or chunk-relative authoritative coordinates.
