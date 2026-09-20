# Climate Data And Lifecycle

- Status: `Transitional; material lifecycle integration is under validation`
- Last verified: `2026-09-15`
- Scope: recipe/configuration ownership, capabilities, server lifecycle, thermal runtime integration, and network boundaries
- Primary code anchors: `FHRecipeCachingReloadListener`, `WorldTemperature`, `MinecraftThermalEvents`, `MinecraftThermalInput`, `ThermalWorkerPool`, `LevelChunkSectionMixin_ThermalInput`, `FHCapabilities`, `FHNetwork`

源码、已加载数据包和服务端配置是最终权威；本文只记录当前接入顺序。新 thermal runtime 的 Page、source、solver 和 query 细节见 [thermal-runtime-architecture-and-optimization.md](thermal-runtime-architecture-and-optimization.md)。

## Data Ownership

`FHRecipeCachingReloadListener` rebuilds recipe indexes after `/reload`.
`buildRecipeLists` also rebuilds empty recipe sets and calls `WorldTemperature.clear`
after replacing temperature tables. Remote clients do the same on recipe sync;
integrated clients skip this duplicate rebuild because the server owns the shared
static tables and caches. Cached
dimension/biome values therefore cannot outlive the tables that supplied them.
`WorldTemperature` owns dimension/biome/altitude natural temperature lookup.
`StateTransitionData` declares material C/O/conductance and independent heating/cooling
edges. Its authoritative datagen input is
`src/datagen/resources/data/frostedheart/data/state_transition.xlsx`; `FHRecipeProvider`
emits `data/frostedheart/recipes/state_transition/`. `PhysicalState` slots and the phase
`heat_capacity` timing factor have been removed. The spreadsheet has been adapted to
the new schema and reconnected to `FHRecipeProvider.materialTransitions`. Its original
data sheet contains 74 material rows; a Chinese field guide explains units and defaults.
`heating_enabled` / `cooling_enabled` are authoring controls only. FALSE keeps a direction
visible in the spreadsheet but omits it from runtime JSON; with a target present, blank
means enabled. Five originally disabled cooling declarations are preserved explicitly.
The intermediate `material_transitions.json` authoring file is retired; runtime reads
the generated recipes, never Excel. Other sheets/blank rows without a `block` column
value produce no recipe.
`PlantTempData` remains separate plant gameplay data.
See [material rules](heat-production-and-network.md#material-transition-data) for the schema and energy reference.

Persistent capabilities remain separate from thermal mesh state:

| Owner | Persistence | Current responsibility |
|---|---|---|
| `WorldClimate` | NBT capability | climate clock, daily cache, white-curtain descriptors |
| `PlayerTemperatureData` | NBT capability | five body-part energy offsets, clothing stacks, and difficulty; sampled environment/HUD power are transient |
| `HeatEndpoint` / `GeneratorData` | block/entity or team data | heat-network inventory and machine power semantics |
| `MinecraftThermalInput` | runtime only | Page handles, capture queues, worker mailbox, and query publication |
| `MinecraftGameplayFields` | transient world lifetime | shared generator/boss/command index, retained across physical-runtime rebuilds |
| `DormantChunkThermalState` | chunk NBT | bounded Air residuals and exact material H/branch/BlockState/time checkpoints |
| warm stone / hot-water bag | ItemStack NBT | version-1 initialized flag plus absolute core and surface temperatures |

Source bindings, analytic fields and worker topology are not serialized.
`DormantChunkThermalState` writes exact `MaterialSectionState` records and a backend-specific
Air payload: scalar quantized residuals, or continuous `AirFieldCheckpoint` coefficients/shapes.
Both use the saved natural baseline. The opt-in `continuousAir` restart setting defaults to false;
see [Continuous Air Runtime](continuous-air-runtime.md) for ownership and current limits.
Admission rebuilds topology from current BlockState and restores matching material
energy. Transition world conditions/effects live only in the shared profile snapshot;
they add no fields to each saved material record.

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
  MinecraftThermalInput.bootstrapLoadedSources(server)
  tagged BlockState semantics and shared signature/geometry tables are frozen once

Datapack reload
  recipe listener rebuilds indexes, closes physical runtimes, invalidates profiles
  TagsUpdatedEvent(SERVER_DATA_LOAD), after tag binding: bootstrapLoadedSources(server)

Level tick START
  existing team enumeration advances town data and publishes generator analytic desired state
  completed generator-provider refresh removes fields from deleted team holders

Level tick END
  MinecraftThermalInput drains completion
  MinecraftPageManager applies worker residency, mutations, and Brick capture budgets
  advance the shared startup/load/refusal discovery queue (up to 8 chunks/tick)
  every 20 ticks: source flush and one immutable batch submit

ChunkDataEvent.Load (async)
  decode primitive dormant NBT into the new LevelChunk only

ChunkEvent.Load (level thread)
  attach saved H/time/natural baseline without a cooling sweep
  a loaded lit campfire may start the runtime; attach owners and enqueue discovery

Machine production / campfire ignition
  enabled positive-power output or first ignition may start the same runtime

ChunkDataEvent.Save / ChunkEvent.Unload / ServerStoppingEvent
  capture coherent Page temperatures and sample ticks; serialize existing anchors
  without advancing dormant energy, and write only already-loaded chunks

Level unload
  checkpoint active Pages, detach section hooks, close capture/source/radiation state,
  request mailbox processor close; MinecraftGameplayFields.unload(level)

Player logout / dimension exit
  remove the current/old dimension radiation receiver cache by UUID-derived key

ServerStoppedEvent
  MinecraftThermalInput.closeAll()
  MinecraftGameplayFields.stop()
  closeShared() joins the bounded thermal workers
```

Wearable reservoirs advance on the configured player-temperature cadence in
`PlayerTemperatureUpdate`, after the ordinary five-part body computation and
before the existing quantized body packet decision. No reservoir-specific
network packet exists; ItemStack/Curios synchronization remains authoritative
for carried and equipped items. Placed reservoirs use vanilla block entity
update packets/tags to synchronize their stored `ReservoirItem` stack whenever
exchange changes it. The block entity type is `frostedheart:thermal_reservoir`;
`ThermalReservoirBlockEntity.saveAdditional/load` preserve the complete item NBT.
Inventory stacks and exact single `ItemEntity` stacks use their own staggered
20-tick cadence. Placed reservoirs also use a position-staggered 20-tick server
block cadence, with at least 20 loaded ticks before the first update and no
unloaded-time catch-up. Dropped and placed sampling share at most 64 quarter-block positions per
level and tick, plus a separate 64-receiver radiation witness cache; overflow
still receives composed air with zero direct radiation. These caches are
transient and are cleared with the level runtime. The position cache stores raw
physical inputs only. Every item query composes the current world-owned
`MinecraftGameplayFields` at its exact receiver position, including when the
physical runtime is absent; runtime close does not discard generator floors.

`TemperatureThreadingPool.java` is intentionally retained but never initialized
or polled. It is not part of the new lifecycle. No synchronous thermal dispatch
entry remains.

## Thread Boundaries

`CampfireBlockMixin_TimeLimit.onPlace` starts physics only on the server thread
and only for a newly lit campfire. It preserves the parent callback and follows
Forge's accepted-placement lifecycle. There is no campfire-specific hook on
LevelChunk.setBlockState and no campfire cookTick poll.
Recipe reload closes physical runtimes and invalidates profiles in the recipe
listener. Loaded campfires are checked only after server tags have been bound;
active machines recover through their next production tick. Client tag-packet
events never bootstrap physical runtimes.

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
The server-side `TagsUpdatedEvent` then checks loaded lit campfires once to restart
needed dimensions. Compiling in the earlier recipe listener would freeze old
material/radiation tags. Enabled
machines can restart through their next normal output. Player environment queries
can also lazily start the runtime; analytic-field insertion
only creates a world-owned index and never starts the physical runtime.
Startup binds already-loaded sections and queues current-state
discovery without replaying dormant/radiation chunk-load events. Full reload
repeats this attachment; worker-only replacement retains owners, source indexes,
and pending discovery, using `reseedAll` without another world scan. Chunk unload
removes its pending entry by instance; close clears all pending chunk references.
Natural-temperature cache invalidation remains owned by its existing recipe
listener; changes to that contract must update `WorldTemperature` and this
document together.

`MinecraftGameplayFields` keeps the same index instance across physical close,
worker replacement, and recipe reload, including when empty. Actual level unload
and server stop clear it. Ownership uses `ServerLevel` identity, not climate
capability availability, so fixed-time dimensions also support fields.
Boss removal resets `coldApplied`; normal AI can republish after same-object
readdition. Commands are session-only and remove only their own namespaced key.

`GeneratorData.publishGameplayHeat` runs in the existing level START team loop
after optional town advancement and before morning settlement. It does not
consume fuel, tick twice, scan chunks, or depend on online members/source chunk
residency. Normal machine changes become visible by the next valid START pass.
Positive levels retain afterheat independently of active physical power.
Disassembly checks the bound master and dimension; rebind removes the old field
before changing identity. A completed provider refresh removes orphaned team
fields, while leaving boss/command fields intact. Server restart rebuilds fields
from team/entity data without persisting duplicate definitions.

Before recipe reload or worker replacement discards a publication, the main
thread captures its last coherent Page cut into the loaded chunk attachment.
The replacement worker receives an immutable `DormantAirCut`; no NBT round trip
or retained old arena is required. Slot generation checks still prevent stale
arena access.

Air and material share lazy natural cooling through `DormantThermalCooling`.
No source-support flag, source-neighbor refresh or save-time decay remains.
Infrared does not read dormant Air means. Air restoration targets actual Air only.
`DormantChunkThermalState` writes and reads format 5 exclusively; older thermal
records are not restored. `MaterialSectionState` records exact material H, active
phase branch, sampling tick and stable BlockState identity separately from Air history.
One scalar tick covers uniform-age records; partial writes lazily add per-record
ticks. Air also saves its capture's natural-temperature baseline. Pure reads and
serialization do not change these anchors. Partial checkpoints retain unrepresented
material ages; admission receives a `DormantMaterialCut` for one-time projection.
Material replacement discards the previous body's heat; confirmed thermal conversion
continues with the same H under the target law. `ThermalPhaseRequestStore` contains
request sequence/state only, and matching ACK never subtracts latent energy again.

`MinecraftPhaseController.materialChangeCause` is shared by active geometry
capture and dormant material updates. Its scoped cause matches position and both
old/new BlockStates, so an unrelated nested replacement at the same position does
not inherit phase energy. Physical submission also rechecks the current world state.
The chunk callback ignores an outer setter's outdated target when `onPlace` already
performed a nested replacement; the newer material identity and journal entry win.
The existing short-lived mutation scope also remembers supersession at the same
position, so replacing back to the outer target state cannot revive its old H.
Shape/property-only changes retain H;
ordinary block replacement initializes a new body; confirmed physical transitions
retain H under the target law. Dormant phase commits carry the projected H/tick
through the existing APPLYING scope and successful chunk mutation hook. They require
no Page and no synthetic ACK. Explicit nonphysical recipe conversions preserve the
body's projected temperature, while an explicit analytic bound can supply the energy
needed for a physical heating endpoint. Existing dry/wet effective-capacity changes use one
`MaterialThermalLaw.afterMassChange` rule; snow-layer and slab-volume subdivision
are not implemented.
Removal uses `Hnext=offsetNext+(H-offsetOld)*Cnext/Cold`, keeping the target energy
reference explicit, including after parameter changes (H/offset in J, C in J/K). Lava amount variants share
the source lava's specific offset; only source lava has the native basalt edge.

`SECTION_REPLACED` also resets material identity when the final thermal signatures
are unchanged. The reset revision prevents old phase requests and checkpoint
projection from restoring the removed body. The marker survives resync retries
until the replacement publication is observed. Raw container replacement uses
the same reason. Material checkpoint updates execute on the server thread;
off-thread callbacks are handed to Minecraft's existing server executor.

`DormantChunkThermalState` owns a `MaterialSectionState.Editor` only for populated
material Sections. Edits copy arrays once after sharing a snapshot, then reuse
the writable arrays; scalar thermometer/presence reads do not publish snapshots.
Removal marks a slot immediately and compacts it when `snapshot()` is requested.
Lazy palette reference counts reuse unreferenced slots without rescanning all bodies
for each edit. Published `MaterialSectionState` objects remain immutable; encoding
emits only referenced parameters and remaps their indices. The former per-edit
immutable `update/remove` API and runtime change adapter are removed.

The Section and Chunk mutation hooks remain distinct: the former observes active
geometry, while the latter also maintains stored bodies without an active runtime.
Both use `ModifyReturnValue` and return the original result unchanged, avoiding
`CallbackInfoReturnable` allocation. The existing LDLib supplies MixinExtras at
runtime; Gradle declares only its compile-time/API processor dependency. Unchanged
writes, client Chunk callbacks, unowned Sections and untracked material positions
exit before expensive work. The checkpoint journal stores position/next-state/cause
plus revision; its old-state field was unused and is removed. Pruning occurs at
most once per touched Section per game tick, with a constant-time clear when the
whole journal has retired. Natural temperature remains necessary only for tracked
replacement/mass changes whose target still has material; removal does not query it.

## Network And Consumers

`SoilThermometer`的方块测量读取`WorldTemperature.material`，不可用时显示明确提示。
`SoilThermometerRequestPacket`仍读取`WorldTemperature.block`，因为这条网络路径服务于
作物生长环境HUD；它不是材料温度计读数。红外仍使用材料基础值与既有解析场显示合成，
本轮未修改shader、色标、0.43融合或蓝色缺值占位。

红外也可读取同一个休眠容器中的`MaterialSectionState`；活动材料优先，保存记录只补充
未驻留Brick。请求和响应增加`storedEpoch`，用于变化、删除和Chunk替换后的增量刷新；
并携带`storedSampleTick`。客户端在最后一片响应提交时一起记录它们。服务端比较两个时刻的
量化材料温度，使纯时间冷却也能刷新；自然温度改变通过原Section显示编号失效。
这些基准不表示新休眠Page，也不持久化，不增加每玩家的服务端材料缓存。

`tryMaterialPhaseAtRandomTick` 接入 `ServerLevelMixin_TemperatureUpdate` 的通用随机抽样，
雪层回调也调用同一入口以保护受跟踪物体的潜热。水恢复原Chunk地表抽样：默认每20tick
选一列，以MOTION_BLOCKING高度图定位，只让命中的表面水调用同一入口；冻结不要求降水。
`StateTransitionData.hasRandomTransitions`使水不增加方块随机资格，也不在其他方块
开启的Section中进入通用相变抽样；重载资格变化比较使用同一判断。独立水温/目标算法没有恢复。
地表积雪/降水与岩浆原生点火继续执行。无记录相变使用编译边做环境平衡近似。
`DEFERRED`只拦截旧温度转换，保留非相变随机行为；`CHANGED`才终止旧状态的后续回调。
不另建轮询、候选位图或定时任务。`StateTransitionData.updateCache`在随机资格改变时，
只于重载边界重算包含相关状态的已加载Section计数。单纯加载、但不参与原随机更新的区域
没有转换时限；该路径不加载Chunk或启动worker。

`FHBodyDataSyncPacket` carries only the quantized player-facing environment and
absolute core temperature. It is sent on the player-temperature cadence only
when either value changes, plus one forced state on login, respawn, and
dimension change. Net body power remains server-side diagnostic state and is
not a client HUD input. World air and Page cell state remain query-on-demand
and are never placed in the body packet.

`FHTemperatureDisplayPacket` remains the registered `temperature_display` message
for localized temperature reports. Its floating-point constructors carry the
one-decimal display flag and its integer constructors retain integer formatting;
`CreativeThermometerItem` sends its unquantized raw body value as a localized
server component instead of using this packet.

`FHRequestInfraredViewDataSyncPacket` carries requestId, forceFull, committed
display generation/center, material epoch, twelve material-presence words,
known material readability, and an optional twelve-word previous field footprint.
Full omits the old field footprint. Opening/movement forces full; stable clients
poll every 40 ticks with an entity-ID offset. Awaiting full uses 41..59-tick
retries; an accepted multipart response finishes before a periodic retry.
Movement supersedes an old requestId.

`FHResponseInfraredViewDataSyncPacket` carries server center, dimension generation,
material epoch and FULL/FIRST/LAST/READABLE flags, optional changed/full material
presence, the complete current field footprint, and final display Brick records.
Empty field footprint explicitly clears its baseline. Each part carries the same
footprint. READABLE describes physical input availability; a field-only or empty
full is a valid display transaction. There is no background key/grid or separate
material-update/control transaction. Matching packet implementations are required
on both endpoints.

Server composition restores analytic fields over raw material, with naturalAir
read only at actual field hits needing natural/base. The previous/current field
Page union is rebuilt each poll, independent of material epoch. A removed field
restores material or INVALID. Non-field Pages retain material epoch increments.
Local geometry mutation keeps unchanged bodies readable from the last Page publication;
the existing material journal excludes replacements and sends affected Brick deltas.
Concurrent publication/slot mismatches retry the entire snapshot twice, then send
nothing rather than a false removal; the client retains its committed texture and
requests again on its normal schedule. A stably invalid or over-age physical input
transitions to a field-only full. An unchanged
unreadable state can continue delta. Readability recovery and generation changes
force full. No field can be hidden solely because physical presence is empty.

`InfraredBrickCodec` uses 0.25°C final quantization and INVALID/UNIFORM/INDEXED/RAW.
Full can omit INVALID after clearing the mirror; all deltas use explicit INVALID
for removal, including Page replacement. The client never erases field display
based on material-presence changes. Parts split at Page boundaries within 960 KiB
including 2,048 reserved header bytes. TCP supplies order, requestId identity.
FIRST validates/starts CPU staging; LAST commits origin, both masks, material
epoch/readability and generation, then uploads. Previous GPU data remains at its
old origin until commit. A field-only full establishes the display baseline;
materialReadable=false does not trigger perpetual full retries or disable shader
sampling. There is still only one 144³ mirror and one temperature texture, with
an 8 KiB scratch for partial Page uploads.

Recipe sync calls `InfraredViewRenderer.invalidateDisplay` to request a fresh
full, while retaining the visible GPU image until replacement. World reset clears
request/field state and detaches existing GPU handles before deletion. No background
resources exist. Missing data uses the original blue placeholder and keeps the
scan complete. Closed clients stop polling; material comparisons stop after the
last request's 80-tick activity window. No-field unchanged windows send no S2C;
field-window capture/traffic scales with its refreshed Page area. Shader scope
and approximations are in [world temperature](world-climate-and-temperature.md).
The player NBT schema preserves each existing clothing `ItemStackHandler`, its
complete item NBT, and temperature difficulty. New saves store per-part
`energy_j`; old Celsius body/feel/environment and dormant
`blockTemp`/`windStrengh` values are ignored on load, so an old player begins at
normal body energy. Analytic fields remain server-side control fields composed
after Page air or natural fallback. Player/town/crop/passive/infrared consumers
never admit a Page merely because a query missed. Dormant checkpoints remain
server-owned; infrared transmits quantized material/analytic display temperatures, never
checkpoint metadata or component vectors.

Changes to this integration must update the relevant consumer document and add
one dated diary entry. Performance evidence comes from external JFR/heap runs;
production code must not gain counters or test-only observation hooks.
