# Knowledge Laboratory open crash and drawing-desk overlap

- Time: `2026-08-26 15:21:20 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Knowledge Laboratory client lifecycle and drawing-desk entry layout`

## Problem

- The first in-client Phase 2 pass crashed immediately after the Knowledge Laboratory entry was clicked.
- `run/logs/latest.log` identified a null dereference in `KnowledgeLabLayer.resizeLab`: `DrawDeskScreen.showKnowledgeLab` requested a primary-layer refresh but resized the new layer before that deferred refresh had populated `actionButtons`.
- The entry occupied `(16, 91, 111, 18)`, while drawing-desk player slots begin at `y = 93`, so the clickable area also covered the first inventory rows.

## Changes

- Constructed Knowledge Laboratory action and observation buttons with the layer itself. `addUIElements` now only attaches those stable objects, so immediate full-window resizing is valid before the next primary-layer refresh.
- Moved the entry to `(77, 68, 50, 19)`, after the existing tech-tree and pause controls and completely above the player inventory. The compact label is `Lab` / `实验室`; hovering retains the full `Knowledge Lab` / `知识实验室` tooltip.
- Centralized the inventory top and entry bounds and added a regression test requiring the entry bottom to remain above the player inventory.

## Validation

- Frosted Research language JSON parsed successfully.
- `./gradlew test --tests com.teammoeg.frostedresearch.gui.drawdesk.DrawDeskLayoutTest --tests com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacketTest` passed, including a fresh client-source compilation.
- Living research documentation required no change because the Knowledge Laboratory behavior and contracts did not change; this was a lifecycle and placement correction.

## Remaining

- Reopen a drawing desk in the development client and confirm the compact entry, full tooltip, full-window transition, return action, and restored inventory interactions visually.
