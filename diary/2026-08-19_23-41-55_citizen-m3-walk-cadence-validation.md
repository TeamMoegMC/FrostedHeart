# Citizen M3 walk cadence validation

- Time: `2026-08-19 23:41:55 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `M3 detailed-entity versus instance Body walk cadence acceptance and town living documentation`

## Completed

- Recorded the post-fix client confirmation that detailed fake citizens and M3 instance Body citizens now walk with matching cadence.
- Updated `citizen-rendering-at-scale.md` and `hybrid-simulation-architecture.md` so walk cadence is no longer listed as an open M3 validation item.

## Decisions

- Close the M3 walk-cadence acceptance gate for the snapshot-distance-driven `0.6662 rad/block` implementation.
- Keep M3 explicitly selected rather than default until the separate Billboard, compatibility, and measured GPU budget gates close.

## Validation

- Manual client result reported by the user after rebuilding/restarting: detailed fake-entity walking and non-entity M3 Body walking are consistent.

## Remaining

- Validate Body/Billboard transitions across 68/72 blocks without ghosting or an empty frame.
- Test Embeddium and Oculus without/with a shader pack, then collect JFR/RenderDoc measurements before enabling M3 by default.
