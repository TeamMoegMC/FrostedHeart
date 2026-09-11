# P2P H01 Status, Connection, and Route Card Follow-up

- Time: `2026-08-24 19:48:36 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P terminal container face, connection rows, runtime status, route card feedback, tests, plans, and docs`

## Completed

- Replaced the shipping/receiving inventory connection face with `logistics_terminal_port`, a 16x16 recessed opening derived from the
  freight-station frame without its envelope mark. The front remains role-specific and other non-functional faces remain freight-station sides.
- Replaced ambiguous `< 20` / `> 20` connection labels with explicit localized sender-to-receiver descriptions and `items/s`; bidirectional
  links list both directed rates.
- Split the former generic unavailable status into `RECEIVER_CONTAINER_UNAVAILABLE` and `PEER_UNLOADED`. A missing shipping source
  container is treated as no supply and does not show a receiving fault.
- Made the receiving terminal own a cached adjacent-container fact. Neighbor changes refresh it immediately, a bounded periodic probe detects
  capability recovery, and loaded senders read the same fact so both endpoints show the same receiving-container error without tick flapping.
- Kept `TRANSFERRING` active for continuous valid demand before the fractional budget reaches one item, preventing low-rate links from flashing
  between transferring and idle.
- Added a selected route-card model using `CustomModelData = 1`, a cyan selection mark, and a tooltip containing the first terminal's role,
  coordinates, and next action. Successful binding or clearing removes the model tag.

## Decisions

- Kept redstone pause and town-wide capacity shortage above container/peer errors in visual-state priority.
- Retained the legacy `UNAVAILABLE` enum value for blockstate compatibility, but no current path uses its former vague player-facing wording.
- Used a 20-tick receiver capability recheck with immediate neighbor invalidation to avoid a per-sender capability scan on every idle tick.

## Validation

- Focused `P2PTerminalScreenTest` and `P2PTerminalResourcesTest`: passed.
- Java 17 full regression: 534 tests in 139 suites passed with zero failures, errors, or skips.
- `compileJava` passed as part of the focused run; only existing Mixin, deprecated API, and duplicate-resource warnings were reported.
- Both language fragments and all changed model JSON files parsed successfully; resource tests verified the two new textures are 16x16.
- `git diff --check` passed.

## Remaining

- Recheck the opening face and selected-card model/tooltip in the running client.
- Verify the longer direction labels at common GUI scales and in both languages.
- Verify missing/restored receiving containers and continuous rates below 20 items/s no longer produce status flicker in-game.
