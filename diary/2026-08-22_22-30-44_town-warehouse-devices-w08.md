# Town Warehouse Devices W08 Delivery

- Time: `2026-08-22 22:30:44 +08:00`
- Author: `Codex; documentation and delivery role`
- Status: `completed`
- Scope: `warehouse-device plan outcome, living documentation, P2P prerequisite contract, and Git staging`

## Completed

- Recorded the user's completion of H02 game validation and closed the town-owned warehouse-device plan through W08.
- Verified that the living town documentation describes the implemented weighted-distance formula, town ownership, topology refresh, reservation persistence/snapshot split, device permissions, emitter watcher/redstone lifecycle, synchronization, and UI behavior.
- Updated the P2P draft to the implemented generic DTO/result/state surface: `TransportEndpointRequest` carries only endpoint ID, kind, and rate; `TransportReservation.CODEC` persists the server-derived metric but omits derived capacity; `SNAPSHOT_CODEC` includes capacity; removal is by stable endpoint ID.
- Documented that current `TeamTown` metric derivation and topology batch refresh remain warehouse-interface-specific. P2P must add a server-owned kind-specific fact resolver before registering a new endpoint kind.
- Staged the source, tests, resources, living documentation, plans, and diary entries belonging to W01-W08 while leaving unrelated local tools and artifacts unstaged.

## Decisions

- H02 is recorded as user-accepted without inventing per-scenario evidence that was not supplied in the conversation.
- Packet-type observation, disk-write observation, and multiplayer testing remain `未测` under the previously recorded environment constraints.
- The P2P plan remains `draft`; completing this prerequisite does not authorize direct implementation against the warehouse-specific metric path.

## Validation

- Existing final automated evidence remains: directed `97`, town package `367`, and full `463` tests, all with zero failures/errors; `compileJava` succeeded.
- Documentation and staging integrity were checked with `git diff --check`, staged-name review, and explicit exclusion review.

## Remaining

- P2P work starts at `P00`; its open topology, fairness, filtering, and failure-semantics decisions remain in the draft plan.
- Packet-type observation, disk-write observation, and multiplayer testing remain `未测`.
