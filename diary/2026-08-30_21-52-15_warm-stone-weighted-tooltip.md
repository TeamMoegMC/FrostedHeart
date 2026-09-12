# Warm stone weighted Tooltip

- Time: `2026-08-30 21:52:15 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `warm-stone branch simplified and advanced item Tooltip presentation`

## Completed

- Changed the normal reservoir Tooltip from surface temperature to the core/surface capacity-weighted average.
- Kept advanced detail explicit by showing average, core, surface, and relative player capacity.
- Added English and Chinese average-temperature translations and updated living documentation.

## Decisions

- The formula is `T_average=(1-a)*T_core+a*T_surface`, where `a` comes from the current profile rather than a hardcoded `0.20`.
- Tooltip rendering remains read-only and does not initialize or write reservoir NBT.
- The deferred client-config debug switch remains out of scope.

## Validation

- `WarmStoneItemTest` covers uninitialized normal display, a non-default `a=0.40` weighted average, advanced average/core/surface ordering, capacity, and unchanged NBT.
- English and Chinese JSON resources parse successfully; `git diff --check` passes.

## Remaining

- Port this presentation behavior together with the rest of the warm-stone feature after the remote temperature architecture stabilizes.
