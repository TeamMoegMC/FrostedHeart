# Transport station naming revision

- Time: `2026-08-19 13:00:32 +08:00`
- Author: `Codex; OpenAI GPT-5; primary design agent /root`
- Status: `completed`
- Scope: `docs/transport_station_design.md; transport-station naming and scope`

## Completed

- Replaced the retired `TownLogistics*` class family with `TransportStation*` throughout the design, including block, block entity, building, scanner, menu, screen, daily model, tests, config section, and translation keys.
- Changed the English display name from `TransportStation` to `Transport Station` while retaining the registry ID `transport_station` and persistent Codec discriminator `transportStation`.
- Removed the robotics-logistics bridge milestone, provider discussion, cross-system risks, and related acceptance work.

## Decisions

- The design now covers only the town building and town `TRANSPORT_CAPACITY` production.
- KHJ logistics is explicitly out of scope while that work is shelved.
- Existing lower camel-case Codec naming remains appropriate and is separate from Java class and display naming.

## Validation

- Searched the design for obsolete `TownLogistics` names and removed all occurrences.
- Confirmed the remaining `TransportStation` occurrences are Java identifiers or the Codec discriminator; player-facing English uses `Transport Station`.
- `git diff --check -- docs/transport_station_design.md` passed with only the repository's LF-to-CRLF checkout warning.

## Remaining

- Before town-capacity production is implemented, decide resident attribute weights, output per standard worker-day, and stockpiling rules.
