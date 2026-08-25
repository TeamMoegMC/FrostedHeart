# Freight Route Card Pair Toggle

- Time: `2026-08-25 16:46:48 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed`
- Scope: `freight route card state, exact-pair lookup, binding interaction, localization, tests, and P2P living documentation`

## Completed

- Removed completed connection IDs from `P2PRouteCardState`; a route card now persists only the first
  pending endpoint selection.
- Cleared the card immediately after a successful bind. Right-clicking with an empty card no longer
  has enough state to disconnect any existing link.
- Added exact current-pair lookup to `P2PBindingState`. Selecting the two endpoints of an existing
  connection now disconnects that connection; selecting any other compatible pair binds or rebinds
  through the existing admission transaction.
- Treated legacy cards containing only `connectionId` as empty cards and removed the obsolete stale
  connection message.
- Updated `docs/town/p2p-logistics.md`. No companion repository data or recipe changed.

## Decisions

- Pair toggling is derived from the town's current authoritative binding state, never from a
  connection ID retained on the item.
- Both successful binding and successful exact-pair disconnection clear the card. Failed operations
  keep the first endpoint selected so the player can correct or retry the second choice.
- Right-clicking air or an unrelated block only clears a pending first endpoint; it cannot disconnect
  a completed link.

## Validation

- Focused route-card state, P2P binding, and resource tests: passed.
- P2P/Transport matrix: 134 tests in 31 suites passed with zero failures, errors, or skips.
- Full test run reached 803 tests; the unrelated `TeamTownActualSaveCodecProbeTest` failed because its
  required external actual-save file was absent. All tests related to this change passed.
- `git diff --check`: passed.

## Remaining

- Confirm in game that a successful bind clears the held card, ordinary right-click does not unlink,
  and deliberately selecting the same two connected terminals unlinks them.
