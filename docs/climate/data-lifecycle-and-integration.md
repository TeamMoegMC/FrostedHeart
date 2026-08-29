# Climate Data And Lifecycle

- Status: `Current`
- Last verified: `2026-08-30`
- Scope: recipe/configuration ownership, capabilities, server lifecycle, thermal runtime integration, and network boundaries
- Primary code anchors: `FHRecipeCachingReloadListener`, `WorldTemperature`, `MinecraftThermalEvents`, `MinecraftThermalInput`, `ThermalWorkerPool`, `LevelChunkSectionMixin_ThermalInput`, `FHCapabilities`, `FHNetwork`

源码、已加载数据包和服务端配置是最终权威；本文只记录当前接入顺序。新 thermal runtime 的 Page、source、solver 和 query 细节见 [thermal-runtime-architecture-and-optimization.md](thermal-runtime-architecture-and-optimization.md)。

## Data Ownership

`FHRecipeCachingReloadListener` rebuilds recipe indexes after `/reload`.
`WorldTemperature` owns dimension/biome/altitude natural temperature lookup.
`StateTransitionData` and `PlantTempData` remain gameplay data; their
`heat_capacity` values are random-tick timing factors, not SI heat capacity.

Persistent capabilities remain separate from thermal mesh state:

| Owner | Persistence | Current responsibility |
|---|---|---|
| `WorldClimate` | NBT capability | climate clock, daily cache, white-curtain descriptors |
| `PlayerTemperatureData` | NBT capability | five body-part energy offsets, clothing stacks, and difficulty; sampled environment/HUD power are transient |
| `HeatEndpoint` / `GeneratorData` | block/entity or team data | heat-network inventory and machine power semantics |
| `MinecraftThermalInput` | runtime only | Page handles, capture queues, worker mailbox, and query publication |

Page cell enthalpy, source bindings, analytic fields, and worker topology are
not written into chunk NBT. They are rebuilt from loaded sections and active
source reports after level load or a worker-generation restart.

## Server Lifecycle

```text
ServerStartingEvent
  ThermalWorkerPool.startShared()

ServerStartedEvent
  MinecraftThermalInput.prepareGameplayProfiles()
  static BlockState -> signature index is frozen once

Level tick END
  MinecraftThermalInput drains completion
  MinecraftPageManager processes leases/mutations/capture budgets
  every 20 ticks: source flush and one immutable batch submit

Level unload
  detach section hooks, close capture/source/radiation state,
  request mailbox processor close

ServerStoppedEvent
  MinecraftThermalInput.closeAll()
  closeShared() joins the bounded thermal workers
```

`TemperatureThreadingPool.java` is intentionally retained but never initialized
or polled. It is not part of the new lifecycle. No synchronous thermal dispatch
entry remains.

## Thread Boundaries

The section mixin records only primitive local-position bits. An off-thread
mutation cannot read a `ServerLevel`, `LevelChunk`, source index, or heightmap.
The level thread drains the inbox, reads each final state once, updates physical
source/sky/radiation state, and submits immutable arrays through
`DimensionInputAccumulator`.

`ThermalDimensionMailbox` serializes one batch per dimension. The worker calls
only `ThermalDimensionEngine.process(ThermalInputBatch)`. A normal completion
is held until the main thread applies Page continuation/resync and phase request
payloads and explicitly ACKs the matching sequence. A terminal failure closes
the engine and starts a new generation from complete current captures.

## Reload And Restart

Recipe reload invalidates gameplay profiles and closes active thermal levels.
The next gameplay query creates a new profile snapshot and worker generation.
Natural-temperature cache invalidation remains owned by its existing recipe
listener; changes to that contract must update `WorldTemperature` and this
document together.

After an unexpected worker exception, the main thread logs the failure, replaces
the accumulator, reseeds every active Page and physical source, and submits a
complete cut. Old Page publications are cleared by the closing worker store,
so no stale arena slot can be observed during restart.

## Network And Consumers

`FHBodyDataSyncPacket` carries only the quantized player-facing environment,
absolute core temperature, net body power, and status flags. It is sent on the
player-temperature cadence only when those values change, plus one forced state
on login, respawn, and dimension change. World air and Page cell state remain
query-on-demand and are never placed in the body packet.

The player NBT schema preserves each existing clothing `ItemStackHandler`, its
complete item NBT, and temperature difficulty. New saves store per-part
`energy_j`; old Celsius body/feel/environment and dormant
`blockTemp`/`windStrengh` values are ignored on load, so an old player begins at
normal body energy. Analytic fields remain server-side control fields composed
after Page air or natural fallback. Town/crop/passive consumers never admit a
Page merely because a low-frequency query missed.

Changes to this integration must update the relevant consumer document and add
one dated diary entry. Performance evidence comes from external JFR/heap runs;
production code must not gain counters or test-only observation hooks.
