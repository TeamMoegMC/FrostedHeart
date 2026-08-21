# Citizen sync and render hot-path optimization

- Time: `2026-08-21 14:59:16 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation`
- Status: `completed`
- Scope: `content/town/citizen/client/**`, `content/town/citizen/sync/**`, citizen tests, and town living documentation

## Completed

- Removed the second client-cache ID lookup from spawn/batch backend notifications while preserving the existing two-phase batch ordering.
- Hoisted client game-time sampling to render/tick/batch entry points and skipped unchanged batch-owner map writes.
- Reworked `SyncEngine.flushDeltas` to resolve each tracked due citizen once, share one immutable delta Entry across players, reuse grouping scratch, and write back only successfully handed-off records.
- Switched detailed proxy removal to the fastutil primitive fast iterator and removed split-fragment entry-list copies.
- Updated `docs/town/hybrid-simulation-architecture.md` and `docs/town/citizen-rendering-at-scale.md` with the implemented lifecycle and performance contracts.

## Decisions

- Preserve “apply the entire client batch, then notify backends” rather than invoking backend callbacks inline during cache mutation.
- Recycle chunk lists only after Forge 47.3.0 `SimpleChannel.send` has synchronously encoded the packet; do not pool packet outer lists or `FriendlyByteBuf`.
- Keep canonical writeback at the end of the flush and gate it on successful handoff to at least one player.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.citizen.client.ClientCitizenTimeSamplingTest --tests com.teammoeg.frostedheart.content.town.citizen.client.CitizenRenderCoordinatorTest --tests com.teammoeg.frostedheart.content.town.citizen.sync.CitizenDeltaPacketBatcherTest` — passed.
- `./gradlew test` — passed (`BUILD SUCCESSFUL`).
- `git diff --check` and targeted hot-path searches — passed; only the repository's existing LF/CRLF conversion warnings were reported.

## Remaining

- Measure actual client and dedicated-server gains with JFR/`spark`; this change removes confirmed redundant work but does not claim an unmeasured millisecond or allocation reduction.
