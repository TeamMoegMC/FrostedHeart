# Citizen Body clock and delta batching fix

- Time: `2026-08-20 03:19:18 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `FlywheelCitizenBackend` M3 timing, `SyncEngine` delta batching, focused regression tests, and town Citizen documentation

## Root causes

- M3 instance snapshots used Create's `AnimationTickHolder`, while
  `citizen.vert` received Flywheel's `uTime`. The independent clocks could
  differ by one tick, making every four-tick position/yaw segment start with a
  visible pre-blend. The client benchmark is local-only and does not involve
  the server or network.
- `SyncEngine.flushDeltas` stopped packing after 240 entries but advanced the
  shared canonical state for every due ID. With the configured 1024-resident
  visibility budget, entries after the first 240 could be skipped until a
  later heartbeat or state change.

## Implemented

- Switched `FlywheelCitizenBackend` to
  `com.jozufozu.flywheel.util.AnimationTickHolder`, keeping the shader and
  interpolation formulas unchanged.
- Added package-private `CitizenDeltaPacketBatcher`, which partitions
  chunk-grouped entries into packets of at most 240 entries and can split a
  single chunk group without changing coordinates or entry data.
- Updated `SyncEngine` to send every partition and advance canonical state only
  for IDs present in packets successfully handed to
  `FHNetwork.INSTANCE.sendPlayer`. The existing 240-entry wire limit and no
  application-level ACK contract remain unchanged.
- Updated `docs/town/citizen-rendering-at-scale.md`,
  `docs/town/hybrid-simulation-architecture.md`, and the implementation plan
  to document the clock-domain and multi-packet contracts.

## Validation

- Focused command:
  `./gradlew.bat test --tests com.teammoeg.frostedheart.content.town.citizen.client.FlywheelCitizenBackendTest --tests com.teammoeg.frostedheart.content.town.citizen.sync.CitizenDeltaPacketBatcherTest --console=plain`
  completed successfully: 11 Flywheel tests and 4 batcher tests passed.
- Full command:
  `./gradlew.bat test --console=plain` completed successfully: 72 suites,
  251 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` reported no whitespace errors; only existing
  LF-to-CRLF working-copy warnings.

## Remaining

- Run the real client `1024 moving` M3-vs-CPU matrix across direction reversal,
  F3+T, Flywheel origin crossing, dimension round trip, and renderer reload.
- Run a server-backed town with more than 240 tracked residents and capture a
  single flush's packet sizes, expecting `240,240,240,240,64`, plus client
  updates for residents after entry 240.

