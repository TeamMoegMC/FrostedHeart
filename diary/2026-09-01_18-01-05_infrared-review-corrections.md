# Infrared review corrections

- Time: `2026-09-01 18:01:05 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `infrared request lifecycle, far-coordinate shader sampling, and known Brick wire modes`

## Completed

- Kept center/full requests forced until a matching response installs the new
  texture origin, preventing a delayed or absent full response from falling
  back to a delta based on the previous center.
- Replaced subtraction of two absolute float world coordinates with a CPU
  double-precision camera-to-texture-origin offset and one small float shader
  uniform.
- Added direct INVALID and UNIFORM codec entry points so known invalid/regular
  Bricks do not fill and rescan the 64-value mixed scratch.
- Corrected the sparse full/add path: omitting an all-invalid record is a
  successful operation, not a reason to cancel the complete response.
- Updated the climate living documents and active implementation plan.

## Decisions

- Reused `snapshotAvailable`; no pending-request field or client state object was
  added.
- Preserved the existing packet bytes, four modes, Page upload policy, texture,
  polling cadence, and server ownership model.
- Kept the generic dictionary path only for mixed Bricks.

## Validation

- `compileJava`, `compileTestJava`, and `compileGameTestJava`: passed.
- Selected thermal and infrared JUnit suite: `117/117` passed.
- Direct INVALID/UNIFORM records are byte-identical to generic encoding;
  omitted INVALID leaves an empty payload.
- Forge GameTest server: `14/14` required tests passed.
- Old absolute-coordinate uniforms were removed; `git diff --check` passed
  before the final documentation update.

## Remaining

- Run live Vanilla and Oculus/Embeddium checks for center movement, crouching,
  far coordinates, sparse full responses, and Page deltas.
- Run the planned 100-client JFR/heap/network performance gate.
