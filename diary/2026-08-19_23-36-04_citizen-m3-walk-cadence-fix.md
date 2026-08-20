# Citizen M3 walk cadence fix

- Time: `2026-08-19 23:36:04 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `FlywheelCitizenBackend, citizen.vert, regression tests, and town rendering documentation`

## Completed

- Investigated the reported visual mismatch where detailed fake entities walked while M3 instance Bodies appeared to run.
- Replaced the fixed GPU cadence and amplitude with snapshot-distance-driven animation at vanilla humanoid scale, without changing the 58-byte instance layout.
- Added regression coverage for cumulative walk phase, phase encoding, shader speed/amplitude formulas, removal of the fixed run cadence, and removal of global body bob.
- Updated the town rendering living documentation with the root cause, corrected behavior, and pending retest.

## Decisions

- Use the existing instance `phase` byte as a quantized cumulative phase. `FlywheelCitizenBackend` advances it by the previous snapshot's planar distance at `0.6662 rad/block`, matching the vanilla humanoid phase scale.
- During snapshot interpolation, derive cadence and amplitude from `length(pos1.xz - pos0.xz) / duration`; during extrapolation, use `length(velocity)`. This lets collision slowdown and the deterministic benchmark match the detailed entity path.
- Scale arm amplitude by per-tick walk speed and leg amplitude by `1.4 * speed`, matching vanilla `HumanoidModel`; remove the old `0.05`-block whole-body bob because the detailed fake entity has no equivalent transform.
- Preserve the M3 stride, program id, network protocol, LOD ownership, and dirty-byte accounting.

## Validation

- The new shader regression assertion failed against the old `uTime * 0.6`, fixed `0.55` amplitude, and global bob implementation before the production change.
- Focused `FlywheelCitizenBackendTest`: 8 tests passed after the fix; `BUILD SUCCESSFUL`.

## Remaining

- Run F3+T and repeat `/citizen_debug benchmark load 1024 moving`; visually compare detailed and instanced residents for matching walk cadence and stride.
- Confirm start, stop, turn, and moving-to-sleeping transitions remain smooth, then complete 68/72-block Billboard and Embeddium/Oculus validation.
