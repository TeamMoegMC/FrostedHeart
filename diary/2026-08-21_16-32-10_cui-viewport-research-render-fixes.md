# CUI viewport and research rendering fixes

- Time: `2026-08-21 16:32:10 +08:00`
- Author: `Codex (OpenAI; production implementation) with Terra medium (tests and validation)`
- Status: `completed`
- Scope: `PanZoomViewport`, `CUIScreenWrapper`, research archive graph/modal state, camera persistence, hot-path caches, focused tests, research UI documentation

## Completed

- Suspended graph rendering and disabled background archive controls while the centered project dialog is open, then restored them without rebuilding the graph.
- Preserved saved per-field cameras across definition refresh and field switching; first-time fields alone receive the automatic `15%` fit.
- Reused graph node render records and cached node hit testing until pointer, camera, projection, or viewport invalidation.
- Consumed non-zero wheel input at zoom limits and corrected standalone `CUIScreenWrapper` drag-delta forwarding.
- Added regression coverage for modal graph suspension, camera existence and refresh retention, field switching, and wheel consumption at both zoom limits.

## Decisions

- Kept graph nodes virtual and batched instead of creating one CUI child per research.
- Hid only the graph viewport during the modal while disabling all background controls; the archive chrome remains visible under the existing dimming mask.
- Used `ResearchWorkspaceState#hasCamera` as the authority for distinguishing first fit from a saved default-valued camera.

## Validation

- Terra medium ran the four focused test classes: `BUILD SUCCESSFUL`.
- Terra medium ran `.\gradlew.bat test`: `BUILD SUCCESSFUL in 9s`.
- `git diff --check` passed with only existing CRLF conversion warnings.

## Remaining

- `CUIScreenWrapper#mouseDragged` has full compile coverage but no isolated unit test because the wrapper currently requires a Minecraft client screen environment.
- In-game visual and frame-allocation profiling remains useful for very large research graphs.
