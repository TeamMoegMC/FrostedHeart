# Climate Data And Lifecycle

- Status: `Current`
- Last verified: `2026-09-01`
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
| `DormantChunkThermalState` | chunk NBT | bounded Air-temperature residual checkpoints for retired Pages |

Arena enthalpy, source bindings, analytic fields, material/phase state, and
worker topology are never serialized. `DormantChunkThermalState` writes only
quantized Air residuals relative to section-center `WorldTemperature.naturalAir`.
Regular Bricks store one value; bounded exact mixed Bricks store one
capacity-weighted mean plus deterministic component values. Admission rebuilds
topology from current BlockState and uses the checkpoint only for initial Air
and material-pole temperatures.

Thermal gameplay coefficients are instance-wide COMMON config under
`FHConfig.COMMON.THERMAL_RUNTIME`, stored in
`config/frostedheart-common.toml`. `MinecraftThermalProfiles.prepare()` reads
them once into an immutable snapshot at server start. Editing the file requires
a client or dedicated-server restart; active workers never poll config and no
per-save `serverconfig` copy is involved.

## Server Lifecycle

```text
ServerStartingEvent
  ThermalWorkerPool.startShared()

ServerStartedEvent
  MinecraftThermalInput.prepareGameplayProfiles()
  tagged BlockState semantics and shared signature/geometry tables are frozen once

Level tick END
  MinecraftThermalInput drains completion
  MinecraftPageManager applies worker residency, mutations, and Brick capture budgets
  every 20 ticks: source flush and one immutable batch submit

ChunkDataEvent.Load (async)
  decode primitive dormant NBT into the new LevelChunk only

ChunkEvent.Load (level thread)
  consume one-shot source support, rebase residuals, then expose fallback

ChunkDataEvent.Save / ChunkEvent.Unload / ServerStoppingEvent
  capture coherent Page temperatures, refresh disk-only source support,
  and write only already-loaded chunks

Level unload
  checkpoint active Pages, detach section hooks, close capture/source/radiation state,
  request mailbox processor close

Player logout / dimension exit
  remove the current/old dimension radiation receiver cache by UUID-derived key

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

Static fire/lava radiation uses a separate primitive Brick dirty map and never
enters `ThermalInputBatch`. `LiquidBlock.updateShape` only marks lava's own
Brick. Receiver queries never admit a Page: palette-positive coverage alone may
reuse the existing loaded-section mutation owner. Unknown Bricks enter one
dimension pending mask and become known under a fixed per-tick capture budget;
chunk unload removes known, emitter, pending, and dirty state. Static rays retain
no receiver cache or section witness.

`ThermalDimensionMailbox` serializes one batch per dimension. The worker calls
only `ThermalDimensionEngine.process(ThermalInputBatch)`. A normal completion
is held until the main thread applies absolute Brick residency/resync and phase request
payloads and explicitly ACKs the matching sequence. A terminal failure does not
close publications in the worker catch path. The main thread first checkpoints the
quiescent engine's last coherent cut, then its terminal ACK closes that engine
before a replacement generation reuses the Page handles. Query preparation keeps
the previous envelope readable until its final exchange; if Page references were
already exchanged when preparation fails, they alone are restored to that prior
cut. A processor-close exception is logged after mailbox ownership is released,
so `inFlight` is cleared and replacement still proceeds.

## Reload And Restart

Recipe reload invalidates gameplay profiles and closes active thermal levels.
The next gameplay query creates a new profile snapshot and worker generation.
Natural-temperature cache invalidation remains owned by its existing recipe
listener; changes to that contract must update `WorldTemperature` and this
document together.

Before recipe reload or worker replacement discards a publication, the main
thread captures its last coherent Page cut into the loaded chunk attachment.
The replacement worker receives an immutable `DormantAirCut`; no NBT round trip
or retained old arena is required. Slot generation checks still prevent stale
arena access.

Every successful Page capture immediately derives its one-shot disk support bit
from the current physical-source target index. Source target/power/enabled
changes update only the target section and six face neighbors that already own
a loaded dormant entry. This avoids an active-Page-only stop scan and adds no
loaded-world traversal or retained chunk index.
Chunk activation also retains the consumed disk-support decision in one lazy
primitive bitset until live source discovery updates it, so an initial infrared
full response cannot race campfire/block-entity registration.

## Network And Consumers

`FHBodyDataSyncPacket` carries only the quantized player-facing environment and
absolute core temperature. It is sent on the player-temperature cadence only
when either value changes, plus one forced state on login, respawn, and
dimension change. Net body power remains server-side diagnostic state and is
not a client HUD input. World air and Page cell state remain query-on-demand
and are never placed in the body packet.

`FHRequestInfraredViewDataSyncPacket` is a separate client-carried-state poll:
opening or moving forces a full request; stable clients poll every 40 ticks with
an entity-ID phase offset, the last infrared epoch, and twelve exact presence
words. A center/full request remains full across a missing or superseded response
until one matching response installs the new texture origin. While waiting, the
normal 40-tick poll is replaced by one entity-ID-spread retry after 41-59 ticks;
none of those delays is a multiple of the thermal runtime's 20-tick cut, so a
transient publication exchange cannot become a permanent cadence collision. A delta is accepted
only when its server-selected center matches the installed texture center;
otherwise it is discarded and the next client tick requests a full snapshot.
A full response may install any server-selected center, which then becomes the
client's movement-comparison baseline.
`FHResponseInfraredViewDataSyncPacket` is omitted when the view has no
changed Brick or Page presence, including when only an out-of-view Page advanced
the dimension epoch. Otherwise it carries optional current presence plus one
flat changed-Brick payload using `INVALID`, `UNIFORM`, `INDEXED`, or `RAW` mode.
`INDEXED` uses `SimpleBitStorage` only when its complete record is smaller than
RAW. Full and added-Page responses omit all-invalid Bricks because the client
first clears their target regions; ordinary deltas send explicit INVALID when a
previous value must be removed. Known invalid and regular-uniform Bricks write
their wire modes directly; only mixed Bricks build and scan a 64-value dictionary.

The server keeps no per-player infrared observer, payload copy, history ring, or
temperature duplicate. Requests extend one dimension-level tracking window to
80 ticks; the window affects query publication only and never retains or admits
a Page. The dimension's fixed epoch storage is budgeted at runtime creation but
its arrays are allocated only by the first infrared request. The client's
`144^3` direct mirror is created by the first actual infrared render, while the
Page upload scratch is created by the first Page delta; both are then retained
as bounded reusable allocations. Before the first accepted full response, an
all-INVALID texture anchored at the player's current section keeps visual
initialization independent from temporary publication unavailability. World reset
immediately detaches GPU handles and queues deletion of those captured old
resources. While a moved client waits for a replacement full response, the old
texture remains renderable at its old origin; `deltaBaselineValid` controls only
delta/full protocol eligibility. A temporarily invalid
or over-age `QueryPublication` produces no
response, so the client retains its existing temperature mirror until a valid
rebuild response is available. A valid presence mismatch sends added/removed
Page delta; full rebuild remains limited to first open, center change,
reactivation, generation/reset, and explicit invalidation boundaries.
One full response may additionally encode source-supported dormant Brick means
through existing `UNIFORM` records. This temporary Brick-resolution bootstrap
does not enter client presence or normal deltas, admit a Page, or load a chunk;
real Page admission clears and replaces it with block-position exact data.

The player NBT schema preserves each existing clothing `ItemStackHandler`, its
complete item NBT, and temperature difficulty. New saves store per-part
`energy_j`; old Celsius body/feel/environment and dormant
`blockTemp`/`windStrengh` values are ignored on load, so an old player begins at
normal body energy. Analytic fields remain server-side control fields composed
after Page air or natural fallback. Player/town/crop/passive/infrared consumers
never admit a Page merely because a query missed. Dormant state is server-only
and adds no packet.

Changes to this integration must update the relevant consumer document and add
one dated diary entry. Performance evidence comes from external JFR/heap runs;
production code must not gain counters or test-only observation hooks.
