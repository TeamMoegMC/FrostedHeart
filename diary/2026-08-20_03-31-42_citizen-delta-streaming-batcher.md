# Citizen delta streaming batcher refinement

- Time: `2026-08-20 03:31:42 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `CitizenDeltaPacketBatcher`, `SyncEngine` network handoff naming, tests, and town synchronization documentation

## Completed

- Replaced the eager `partition` result with
  `CitizenDeltaPacketBatcher.forEachPacket`, so `SyncEngine` sends each packet
  as soon as the packet is produced instead of retaining every packet for the
  player until partitioning finishes.
- Reused complete `S2CCitizenBatchPacket.Group` objects and their entry lists.
  Only a group fragment that crosses the 240-entry boundary now allocates a
  copied slice list.
- Renamed `sentToAnyPlayer` to `handedOffToAnyPlayer`. IDs enter this set only
  after `FHNetwork.INSTANCE.sendPlayer` returns normally; this remains a local
  API handoff contract and does not claim a client ACK.

## Decisions

- Keep the full per-player `byChunk` map because the wire format benefits from
  chunk headers and the tracked set has no useful chunk order. Optimize the
  duplicate packet representation rather than replacing chunk grouping.
- Preserve callback exception propagation. If a send throws, batching stops and
  later packets are not falsely recorded as handed off.
- Do not change `CBaseNetwork.sendPlayer` to return a boolean: its underlying
  Forge `SimpleChannel.send` is void, so a boolean wrapper would manufacture a
  guarantee the transport does not provide.

## Validation

- Focused `CitizenDeltaPacketBatcherTest` passed after incremental Java and test
  compilation.
- Coverage retains the 0/1/239/240/241/480/481/1024 boundaries, single- and
  multi-chunk preservation, exact ID coverage, and now asserts that whole groups
  are reused while split fragments are copied.
- Full `./gradlew.bat test --console=plain` completed successfully: 72 suites,
  252 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check` reported no whitespace errors; only existing LF-to-CRLF
  working-copy warnings.

## Remaining

- The separate P1 recommendation remains: add a production-path
  `SyncEngine.flushDeltas` integration test with an injectable network sink.
