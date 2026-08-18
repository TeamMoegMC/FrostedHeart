# Citizen home, sleep, and presence lifecycle

- Time: `2026-08-18 17:57:08 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `town house layout; citizen behavior, movement, presence, synchronization, and transient home slots`

## Completed

- Persisted canonical bed-head positions and the scanned house entrance in `HouseBuilding`.
- Sent residents home through the entrance, hid sleeping residents from movement, spatial queries, synchronization, rendering, and interaction, and placed valid sleepers at deterministic beds.
- Assigned one UUID-sorted `homeSlot` per resident for deterministic beds and distinct spawn/wake exits; morning wake-up is spread across 200 ticks.
- Hardened exit placement with bounded terrain, liquid, headroom, drop, and same-house occupancy checks.
- Fixed exact-overlap separation without allocating temporary arrays in the movement hot path.
- Removed the redundant transient `bedSlot[]`; bed eligibility is now `homeSlot < bedCount`.

## Decisions

- Kept `Resident.housePos` as the persistent resident-to-house key. It is no longer a visible spawn, return, or sleep coordinate; deleting it would require a larger reverse index or repeated house-roster scans.
- Kept `homePos` and `homeSlot` transient and out of the citizen save format. The packed home key provides constant-time house, entrance, and bed lookup.
- Did not narrow `homeSlot` to `short`, because saving another two bytes per capacity slot is not worth imposing a hidden 32,767-resident-per-house limit.

## Validation

- Existing focused citizen layout, presence, wake, exit, separation, bed assignment, and persistence tests passed.
- Full `gradlew test --console=plain` passed before the user requested that no further tests be run; compilation reported only the project's 20 existing JEI removal warnings.

## Remaining

- Perform an in-game smoke test for daytime spawn, evening return, hidden sleep, staggered wake-up, invalid/missing beds, and crowded exits.
- A larger future storage refactor could make `Resident.housePos` the sole housing relation and remove the duplicated `HouseBuilding.residentsUUID` roster, but it should be handled as a separate save migration and daily-settlement redesign.
