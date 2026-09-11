# Warm stone T25 gameplay matrix

- Time: `2026-08-30 15:37:40 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent, with user manual gameplay verification`
- Status: `completed`
- Scope: `T25 real-world three-node curves, lifecycle, UI, synchronization, physical-source, and multi-entity smoke validation`

## Completed

- Parsed the user-run empty, hot/cold worn, split-node reversal, cold-field buffer, heating-device, inventory/dropped, Campfire/occlusion, chest, Curios, invulnerable-mode, persistence, restart, packet, and multi-entity sequences.
- Compared measured surface-player conductance and normalized heat residual against the frozen warm-stone and hot-water-bag profiles without changing parameters.
- Accepted the user's manual UI, tooltip, container, threshold-effect, and subjective performance observations together with server/client log evidence.

## Decisions

- T25 validates rather than tunes the frozen profiles. No balance, runtime, receiver, synchronization, recipe, or lifecycle contract changed.
- The heating-pad segment remained net-cooling because of ambient conditions; it still showed a warmer reservoir surface and no ordering anomaly. It is qualitative evidence only and is not used for a parameter conclusion.
- No living document changed because implemented behavior and contracts did not change; T26 remains the planned consolidation step.

## Validation

- Full-run inferred `g_sp`: hot warm stone `1.19892e-4 /s`, hot-water bag `7.9992e-5 /s`, cold hot-water bag `7.9990e-5 /s`, cold warm stone `1.20052e-4 /s`; each full-run error was below `0.1%` relative to the frozen rate.
- Split-node hot-water bag: player minimum near `55 s`, surface/player crossing near `57 s`, inferred `g_sp=7.9878e-5 /s`, maximum normalized heat residual `0.00075`.
- Cold field at 180 seconds: empty `-0.178 degC`, warm stone `-0.091 degC`, hot-water bag `-0.073 degC` player change.
- Restart continuity: `37.183/48.185/46.922 degC` before save and `37.165/48.130/46.988 degC` after restart for player/core/surface.
- Multi-entity smoke: `31` distinct thermal slot IDs, 9 samples at `20.000 TPS`, total mean tick time `11.935..19.080 ms` with `16.404 ms` average, no catch-up warning.
- Final Gate B: `364.032 s`, `1817` thermal slot packets, `0` content packets, `0` Curios packets, `probe_errors=0`; user reported no visible anomaly.

## Remaining

- Stop before T26. T26-T28 own living documentation consolidation, final per-repository validation, and plan closure.
- Aggregate-temperature Tooltip/client-config work remains deferred until explicitly restored by the user.
