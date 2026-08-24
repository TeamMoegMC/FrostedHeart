# P2P H01 First Frame, Header, and Port Follow-up

- Time: `2026-08-24 21:45:05 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P menu synchronization, header layout, connection labels, endpoint textures, sided capability contract, tests, plans, and docs`

## Completed

- Changed the complex menu slot's pre-sync default from a false `UNBOUND` view to `null`. Until the first authoritative menu packet arrives,
  `P2PTerminalMenu` now derives its temporary view from the client terminal BlockState, removing the initial unbound flash.
- Reworked the header so status text keeps its natural font size and is right-aligned; the title receives the remaining width instead of forcing
  long statuses such as `收货端无可用容器` into a fixed 46-pixel scaled region.
- Connection rows now use complete terminal names and replace the local endpoint with `本终端`, preserving actual sender-to-receiver order
  from either endpoint's view.
- Replaced the shared large port with `shipping_terminal_port` and `receiving_terminal_port`. Each port, including its colored bevel border,
  occupies exactly the centered 6x6 pixel region; shipping is blue and receiving is orange. The obsolete shared texture was removed.
- Made the existing bidirectional all-face item capability rule explicit through `exposesExternalInventoryOn`: all six directions and unsided
  queries expose the same restricted pending/received buffer view, while shipping and receiving terminals expose none of that external buffer.

## Decisions

- The requested exact 6x6 sprite geometry was implemented as a deterministic edit of the existing freight-station pixels. Generative image
  tooling was intentionally not used because it cannot guarantee exact pixel coordinates and the built-in image tool was unavailable.
- Bidirectional terminals keep ordinary freight-station side textures and no dedicated port face because no face has exclusive interaction
  semantics. All-face capability exposure still does not make the terminal actively scan adjacent inventories.
- The status keeps priority over the title only when a translation is too wide for both; neither string is allowed to overlap the other.

## Validation

- Focused `P2PTerminalScreenTest` and `P2PTerminalResourcesTest`: passed.
- Java 17 full regression: 536 tests in 139 suites passed with zero failures, errors, or skips.
- `compileJava` passed as part of the focused and full runs; only existing Mixin, deprecated API, and duplicate-resource warnings were reported.
- Resource tests verify that the two textures are 16x16, differ only on the 20-pixel perimeter of the centered 6x6 port, and use blue/orange
  role colors. Capability tests cover all six directions plus unsided access.
- Changed model and language JSON files parsed successfully; `git diff --check` passed.

## Remaining

- Reopen bound terminals and confirm no unbound frame appears before idle or another authoritative state.
- Inspect both 6x6 port colors on horizontal and vertical placement in the running client.
- Recheck the full-name connection rows and natural-size long status at common Chinese and English GUI scales.
