# Random-Tick and Local Phase Scheduling Review

- Time: `2026-08-22 21:54:39 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Verified that `ServerLevelMixin_TemperatureUpdate` samples `pRandomTickSpeed` positions per random-ticking section rather than scanning `4096` blocks, and that `SnowLayerBlockMixin_Melt` performs snow-layer melting from `SnowLayerBlock.randomTick`.
- Verified that `BlockStateBaseMixin_RandomTick` forces every `StateTransitionData.willTransit()` state to report random-ticking, while generated recipes include widespread terrain such as dirt, snow blocks, ice, packed ice, blue ice, and permafrost.
- Verified that `IceBlockMixin_Melt`, `ThinIceBlock`, and `LayeredThinIceBlock` currently implement no-op `randomTick` methods, so globally preserving their ticking-section activation can consume samples without doing block-local work.
- Replaced the proposed general crossing-time/`PhaseDeadline` scheduler with fixed primitive active-partition solve buckets, a deduplicated wake ring, and actual integration to the current game tick.
- Split transition candidates into native random ticks, stateless staggered chunk surface/shallow samples, and one packed local mutation request per active RC material patch.
- Added concrete primitive storage, request/ack flow, complexity, byte targets, failure modes, implementation stages, and validation gates. Local phase state reuses the material node enthalpy and never creates per-snow-block timers or state.

## Decisions

- Native random tick is reused only where a block still has real native tick behavior. It is not the sole local phase scheduler and is not treated as free when a temperature recipe forces widespread terrain sections to tick.
- The production startup path should stop globally forcing `StateTransitionData` terrain to be random-ticking. No-op ice-like random ticks may be disabled only after explicit behavior tests; snow layers and blocks with non-temperature side effects remain native.
- `AMBIENT_CHUNK_SAMPLE` uses a pure hash due check in `tickChunk` and constant surface/optional shallow reads. It has no loaded-chunk wheel, section scan, block index, or retained candidate queue.
- `LOCAL_PATCH_REQUEST` is produced only by an admitted active RC patch after aggregate energy is sufficient. Each patch has at most one packed candidate and the cross-thread rings contain primitive patch slots, so memory scales with active patches rather than snow or ice volume.
- `randomTickSpeed = 0` remains the compatibility gate for native, ambient, and local terrain mutations. RC air and machine state may still solve without changing those blocks.
- The first production version does not implement general crossing-time deadlines or persistence for uncommitted local transient enthalpy.

## Validation

- Cross-checked the plan against `ServerLevelMixin_TemperatureUpdate`, `SnowLayerBlockMixin_Melt`, `BlockStateBaseMixin_RandomTick`, `IceBlockMixin_Melt`, `ThinIceBlock`, `LayeredThinIceBlock`, and generated `state_transition` recipes.
- Documentation-only task; Java tests were not run because runtime behavior did not change.
- Markdown structure, links, fences, whitespace, and `git diff --check` are validated after this entry is written.

## Remaining

- Phase 0 must measure native versus temperature-forced random-ticking section counts and costs on snowfield, underground ice, and ordinary terrain before selecting the ambient sampling interval.
- Implement the full RC graph and shadow samplers before disabling the legacy Mixin path; production selection remains feature-flagged and benchmark-gated.
