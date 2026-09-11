# Thermal semantic and payload closure

- Time: `2026-08-30 19:21:08 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal mutation capture, fragment payloads, production physics modes, physical-source capacity recovery, resolver/source models, runtime API surface, tests, and living documentation`

## Completed

- Replaced the obsolete 27-center mutation halo with exact changed-position
  capture and one Page handle per section owner. Cross-Brick/Page fragment
  recompilation remains local in `TopologyPlan.markFragmentNeighborhood`.
- Delayed Brick signature scratch allocation until a captured value differs
  from the installed value, so coalesced mutations that return to the original
  state do not rebuild signature payloads.
- Removed seven unread endpoint-generation arrays. Air pairs now store only the
  production buoyancy inputs; FarField stores one Page slot and one wind
  generation per fragment.
- Removed test-only buoyancy/FarField switches, the unused primitive scratch
  flag channel, `ThermalResolution`, and `SourceChannel`. Radiation share is
  precomputed once by each physical-source profile.
- Added bounded hard-cap recovery over the existing loaded source-chunk set.
  Recovery runs only after a refused observation and a later slot release, at
  no more than 64 chunks per 20-tick cut, without overflow-source retention.
- Narrowed same-package transport/configuration members and replaced public
  array records with indexed operation payload classes.

## Decisions

- Slot ownership is proven by topology preflight/commit and reference-counted
  old-span release; solver operations do not repeat generation validation.
- Tests execute production buoyancy and FarField behavior instead of selecting
  a simpler production branch.
- `ThermalSourceMode.IMPULSE` remains the explicit zero-gameplay-caller
  exception required by the user.

## Validation

- Java 17 `compileJava`, `compileTestJava`, and `compileGameTestJava`: passed.
- Thermal JUnit: `96/96` passed.
- Forge GameTest: all `14/14` required tests passed.
- Removed-symbol search, package-surface inspection, and `git diff --check`:
  passed.

## Remaining

- Controlled door/block/source/player/crop JFR and long-running heap evidence
  remain the existing performance-validation task.
