# Knowledge Laboratory organize feedback and layout repair

- Time: `2026-08-26 18:14:26 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation`
- Status: `partial`
- Scope: `DrawingDeskTileEntity V2 inspiration start, KnowledgeLabLayer transition/feedback/layout, research UI documentation`

## Completed

- Inspected the active development save after a player pinned one copper-ore record and four adjacent stone records. The persisted observations carry the expected `copper_outcrop`, `rock_sample`, and geology public facets; this was not an observation-ingress failure.
- Fixed the successful organize transition. Once the server starts `V2_INSPIRATION`, the full-window Knowledge Laboratory now refreshes the game panel's derived widgets and returns to the drawing-desk surface so the existing card game is visible instead of running behind the overlay.
- Added synchronized, topic-neutral `InspirationStatus` feedback for no candidate, missing compatible paper, missing ink, a started game, and completed candidates. Rejected organize attempts now explain what happened without consuming resources or revealing a hidden topic.
- Removed inactive header actions from the visible layout, positioned the current primary action beside the return action, increased inbox/header spacing, and word-wrapped relationship and failure text inside the worksheet.
- After the first client retest reported that both drawn header buttons had no hover or click response, removed the remaining render-time geometry and visibility mutation. Header hit boxes are now positioned only during resize and stable visibility is reconciled during tick; the render pass no longer clears hover by hiding every button before showing the active pair again.
- Updated the research UI and gameplay living documents for the implemented transition and feedback contract.

## Decisions

- Paper and ink costs remain part of V2 inspiration. The usability defect was silent rejection and a hidden successful game, not the resource rule itself.
- Failure text describes the immediate operation or materials only. A no-candidate result does not disclose a topic, desired evidence recipe, Finding, or protocol checklist.

## Validation

- `./gradlew --no-daemon compileJava compileTestJava test --tests com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskLayoutTest --tests com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacketTest` completed successfully.
- `./gradlew --no-daemon test` completed successfully.
- `./gradlew --no-daemon runGameTestServer` reported `All 18 required tests passed`.
- After the hit-region lifecycle correction, `./gradlew --no-daemon compileJava test --tests com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskLayoutTest` completed successfully.
- The active save's compressed team NBT was inspected read-only and confirmed the relevant geology record kinds/facets. No world or team data was modified.

## Remaining

- Restart the development client and verify the successful auto-return/card-game reveal plus the missing-paper and missing-ink messages at the player's GUI scale. This visual/runtime acceptance cannot be replaced by headless tests.
