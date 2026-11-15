# Real-client infrared E2E follows the natural base

- Time: `2026-11-15 13:21:34 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, task from the owner's infrared session`
- Status: `default physical scene passes on a real dedicated server and two real clients; --cooling-only and
  --analytic-stress-only not rerun`
- Scope: `InfraredPhysicalE2eClient`, `InfraredE2eClient`, `InfraredPhysicalE2e`, `InfraredViewRenderer.naturalAt`,
  `Scripts/run-infrared-network-e2e.py`, `docs/climate/infrared-view.md`,
  `docs/climate/validation/infrared-physical-e2e-2026-11-15.json`
- Follows: [translucent surfaces](2026-11-15_12-37-24_infrared-translucent-surfaces.md),
  [infrared natural base](2026-11-15_10-48-51_infrared-natural-base.md)

## Why

The real-client E2E (`Scripts/run-infrared-network-e2e.py`: a dedicated server and two rendering clients through an
ordered, rate-limited TCP proxy) was last validated on 2026-11-08. It reads the GPU texture and required it to equal the
server's wire value, and to become INVALID after a material retires or is removed. Since the natural base, the texture
shows the client's natural temperature wherever the wire has no value, so those expectations failed with nothing wrong:
in the 2026-11-08 record the cold block `4,67,0` was already INVALID on the wire, and the natural base now fills it.

## Completed

- `InfraredViewRenderer.naturalAt(x, y, z)` (package-private): the natural value uploads merge at a block of the current
  window, INVALID outside it, for air, or before it is computed. `InfraredE2eClient.natural()` reads it at the two
  sampled blocks.
- `InfraredPhysicalE2eClient`: the GPU value equals the wire value where the committed display has one, otherwise the
  client's natural value; INVALID only where both are missing. The client's natural value must agree with the server's
  `WorldTemperature.naturalAir` of the same block within one 0.25 °C step: `InfraredPhysicalE2e` writes
  `naturalWarmQ`/`naturalColdQ` into `physical-state.json`. The E2E server turns the daylight cycle off, so the climate
  clock does not advance and the comparison holds for the whole round.
- Phases: `heated` requires a wire value. `removed` still expects INVALID, since air has no natural value. `restored`
  accepts the natural value, or a fresh wire value no warmer than baseline + 0.5 °C (the old rule). `natural-cleared`
  (`--cooling-only`) expects the wire INVALID and the GPU at the iron block's natural value, and its 12-second staleness
  check now looks at the committed wire value instead of the GPU.
- `InfraredE2eClient` (analytic scene) counts cold pixels as blue to violet only (`b > r + 32 && b > g + 64`). The
  natural base near −11 °C is cyan-blue, `b − g` ≈ 40 on grey stone, and on its own passed the old `b > g + 32` test,
  which is meant to show the −40 °C reference (`b − g` ≈ 110).
- Script: `verify_observations` applies the same rules to the saved records. The first launch found two wire-parser bugs,
  both fixed:
  - The infrared reply's message ID was hardcoded as 61. `CBaseNetwork` numbers `FHNetwork` messages from 1 in
    registration order; five town messages added on 11-11 and 11-13 moved `infrared_view_s2c` to 66, so the proxy meter
    decoded another message as an infrared reply ("index out of range"). The script now derives the ID from
    `FHNetwork.registerMessages` (61 at `ad030056e`, 66 now). The E2E had been broken by this since 11-11, independently
    of the natural base.
  - Since `2f18c0265`, FIRST parts carry an optional `InfraredNaturalClimate` after the epoch; the meter skips it and
    records its size as `naturalBytes`.
  - `--java` defaulted to `$JAVA_HOME/bin/java.exe` on every platform; it is `java` outside Windows.

## Validation

- `compileJava compileGameTestJava` passes.
- The wire meter was checked offline against replies laid out like `FHResponseInfraredViewDataSyncPacket.encode`: full
  and delta parts, INVALID records, the natural block with one and two tables, and the message now numbered 61 ignored.
- Real-client E2E, default scene `physical-campfires-3s-rtt-16KiBps`, one run from a fresh world, 13:16:16–13:18:59
  including launch export: passed, both clients. The owner's client was checked not to be running first, and the run
  was scheduled around the infrared session's 100-player load tests. Warm iron `-4,67,0`: baseline −10.01 °C; wire
  and GPU −9.50 °C when heated, −9.25 °C after the fires went out; removed: wire, client natural and GPU INVALID;
  restored: wire still empty, GPU back at the natural −10.00 °C. Cold iron `4,67,0`: no wire value all round, GPU at
  the natural −10.00 °C, the point the old expectations failed on. Client natural equals the server's `naturalAir`
  (q = −40) at both points. Frame colour error 0.45 (heated) and 0.62 (restored) against the 60 limit, 113 and 115
  away from untinted. Slow link: one TCP session, 15 infrared frames, largest body 921 B, largest frame 543 B, 4752 B in
  total; no server timeout. macOS 15.0.1 arm64, Apple M3 Max (OpenGL 4.1 Metal), Java 17.0.20, source-default configs.
  Details in `docs/climate/validation/infrared-physical-e2e-2026-11-15.json`; logs and frames stay in the worktree's
  `build/infrared-network-e2e`, and the failed first attempt in `build/infrared-network-e2e-attempt1`.

## Documentation

- `docs/climate/infrared-view.md` "验证边界": new subsection for the E2E after the natural base (expectations, script
  fixes, this run, the redundant natural climate); the `--java` default; the 2026-11-08 records stay as written.
- New `docs/climate/validation/infrared-physical-e2e-2026-11-15.json`.

## Remaining

- Not rerun: `--cooling-only` (its `natural-cleared` expectations are new) and `--analytic-stress-only` (the new
  cold-pixel rule has not run on a real client).
- Observation, production code not changed: every non-empty reply's first part carries the 165 B natural climate, even
  when the request's `knownNaturalRevision` already matches; the revision only keeps an otherwise empty reply empty
  (`InfraredCapture.capture`). On the slow link that was 2475 B of 8405 B of reply bodies before compression (29 %).
- A climate change during a round is not covered: the E2E world's climate clock stands still.
