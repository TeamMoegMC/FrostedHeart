# Thermal runtime discovery plan review

- Time: `2026-09-08 01:15:22 +08:00`
- Author: `Codex; OpenAI GPT-6; primary design/review agent`
- Status: `completed`
- Scope: `review and revision of the loaded-world reconciliation proposal`

## Completed

- Revised the [existing plan](../plans/2026-09-08_00-38-47_thermal-runtime-loaded-world-reconciliation.md) after comparing earlier startup, persistent input ownership, BE callbacks, synchronous scans, and palette scans.
- Found that current campfire observation allocates disabled sources and refreshes target seeds without checking enabled. Broad discovery therefore needs a shared emitting-state rule and dormant-support cleanup on removal.
- Replaced the proposed bootstrap list plus completed-chunk recovery set with one pending chunk work map for startup, load, and exact capacity refusal.
- Specified immediate owner binding, shared scan budgeting, one-time nearby discovery priority, loaded-only Forge resolution, and existing cut timing.

## Decisions

- Keep current lazy runtime creation and machine production callbacks. Use existing owners and world indexes without a second long-lived input authority.
- Preserve metadata coverage for source-free sections because the section mutation hook needs an owner to receive future sources.
- Propagate capacity refusal to the owning chunk instead of rescanning all completed chunks.
- Only the plan changed. Current-behavior documentation remains unchanged; implementation will need the documented source residency and lifecycle updates.

## Validation

- Inspected production source registration, target release, owner attachment, mutation drain, capacity recovery, runtime cut/restart, configuration ranges, and mapped Forge palette/chunk behavior.
- Confirmed that GlobalPalette.maybeHas returns true and getChunkNow includes Forge currentlyLoading handling; the alternatives have different semantics and costs.
- No Java build, GameTest, or performance measurement was run for this design revision.

## Remaining

- Implement the revised draft and validate its behavior and initial scan budget in the existing workspace.
