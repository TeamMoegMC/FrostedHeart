# Citizen merge persistence and dimension fixes

- Time: `2026-08-14 21:47:33 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `hybrid citizen simulation persistence, legacy NBT migration, cross-dimension runtime rebinding`

## Completed

- Made `CitizenSim.load` tolerate the pre-`syaw` save format by using current yaw as the canonical fallback, and bounded decoding by the available core arrays while defaulting optional fields.
- Corrected `TownSimData` to write its intended nested `sim` compound while continuing to read the accidentally released flat layout used by the current `fh_ai_towns.dat` file.
- Reworked town level changes to notify the old scheduler, bind the new scheduler/level, clear old runtime caches, and immediately rebuild all authoritative residents in the destination level.
- Added `CitizenSimPersistenceTest` coverage for missing `syaw`, nested round trips, and flat-layout migration.

## Decisions

- Kept the flat reader indefinitely as a schema migration path so worlds already saved after the hybrid-simulation merge remain usable.
- Reallocated simulation ids during a dimension move because ids belong to the per-level unmanaged allocator; resident UUIDs remain the durable identity.
- Left the separately reviewed empty-collision heat-source cache issue unchanged at the user's request.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimPersistenceTest` passed.
- `./gradlew test` passed.
- `./gradlew build` passed; it reported the repository's existing duplicate-resource and license warnings.
- `git diff --check` passed for the changes.

## Remaining

- Perform an in-game smoke test that moves a player town's generator dimension while observers remain in both source and destination dimensions.
- Revisit the deferred empty-collision heat-source cache regression separately.
