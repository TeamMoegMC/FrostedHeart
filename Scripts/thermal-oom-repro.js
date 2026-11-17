// External KubeJS scenario for an existing dedicated-server runtime; no mod build.
var FakePlayers = Java.loadClass('net.minecraftforge.common.util.FakePlayerFactory')
var GameProfile = Java.loadClass('com.mojang.authlib.GameProfile')
var UUID = Java.loadClass('java.util.UUID')
var BlockPos = Java.loadClass('net.minecraft.core.BlockPos')
var Blocks = Java.loadClass('net.minecraft.world.level.block.Blocks')
var GameType = Java.loadClass('net.minecraft.world.level.GameType')
var HeightmapTypes = Java.loadClass('net.minecraft.world.level.levelgen.Heightmap$Types')
var ChunkStatus = Java.loadClass('net.minecraft.world.level.chunk.ChunkStatus')
var Items = Java.loadClass('net.minecraft.world.item.Items')
var ItemStack = Java.loadClass('net.minecraft.world.item.ItemStack')
var ForgeCapabilities = Java.loadClass('net.minecraftforge.common.capabilities.ForgeCapabilities')
var LongSet = Java.loadClass('it.unimi.dsi.fastutil.longs.LongOpenHashSet')
var ThermalInput = Java.loadClass('com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput')
var Temperature = Java.loadClass('com.teammoeg.frostedheart.content.climate.WorldTemperature')
var EnvironmentSample = Java.loadClass('com.teammoeg.frostedheart.content.climate.thermal.query.ThermalEnvironmentSample')
var PublicationSample = Java.loadClass('com.teammoeg.frostedheart.content.climate.thermal.query.QueryPublication$MutableSample')
var publishedSample = new PublicationSample()
var players = [], bases = [], setup = [], ticks = 0, started = false, failed = false
var environments = []
var seen = new LongSet(), planted = 0, saveCount = 0
var fires = []
var heatCount = 64
var minutes = 20
var sourceGeometry = 'open'
var residentPlayer = false
var warmupTicks = 400
var mutationsPerCycle = 0
var mutationInterval = 20
var mutationStopTick = 0
var mutationStartsAt = 0, mutationCycles = 0, geometryMutations = 0
var exploreAtTick = 400
var thermalType = null

function write(name, value, append) {
  console.info('FH_REPRO_EVENT ' + name.replace('.jsonl', '.json') + ' ' + JSON.stringify(value))
}
function field(object, name) {
  var type
  try { type = object.getClass() } catch (error) { return null }
  while (type !== null) {
    try { var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(object) }
    catch (error) { type = type.getSuperclass() }
  }
  return null
}
function size(object) { try { return object === null ? -1 : Number(object.size()) } catch (error) { return -1 } }
function fieldSize(object, name) {
  try {
    var type = object.getClass()
    while (type !== null) {
      try {
        var f = type.getDeclaredField(name); f.setAccessible(true)
        var value = f.get(object)
        if (value === null) return 0
        // Fastutil long-key maps cannot receive JavaScript string property lookups such as .size.
        return Number(f.getType().getMethod('size', []).invoke(value, []))
      } catch (error) { type = type.getSuperclass() }
    }
  } catch (error) { }
  return -1
}
function staticField(type, name) {
  var f = type.getDeclaredField(name); f.setAccessible(true); return f.get(null)
}
function publishedFields(level, input) {
  var query = field(input, 'queryPublication'), result = []
  for (var lane = 0; lane < 3; lane++) {
    var fire = fires[lane * heatCount]
    if (fire === undefined) { result.push({ placed: false }); continue }
    var loaded = level.getChunkSource().getChunkNow(fire.getX() >> 4, fire.getZ() >> 4) !== null
    // Read the published field directly; this does not request capture or retain a chunk ticket.
    var available = loaded && query.tryReadAir(fire.getX() + 0.5, fire.getY() + 1.5, fire.getZ() + 0.5, publishedSample)
    result.push({ placed: true, loaded: loaded, available: Boolean(available),
      sampleTick: available ? Number(publishedSample.sampleTick()) : -1,
      minimumQuality: available ? Boolean(publishedSample.minimumQuality()) : false,
      temperatureC: available ? Number(publishedSample.temperatureC()) : null })
  }
  return result
}
function thermalSample(level) {
  try {
    if (thermalType === null) thermalType = environments[0].getClass().getClassLoader().loadClass(
      'com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalInput')
    var budget = staticField(thermalType, 'MEMORY'), input = staticField(thermalType, 'ACTIVE').get(level)
    if (input === null) return { active: false }
    var physicalSources = field(input, 'physicalSources')
    var result = { active: true, usedBytes: Number(budget.usedBytes()), peakBytes: Number(budget.peakUsedBytes()),
      refusals: Number(budget.admissionRefusals()), nativeCheckpoint: Boolean(field(input, 'nativeCheckpoint')),
      knownSources: fieldSize(physicalSources, 'slotsById'), enabledSources: Number(physicalSources.enabledSourceCount()),
      nativeRequests: Number(field(input, 'nativeCheckpointRequests')),
      nativePrepared: Number(field(input, 'nativeCheckpointPrepared')),
      nativePending: size(field(input, 'nativeCheckpoints')),
      nativePeakPending: Number(field(input, 'nativeCheckpointPeakPending')),
      nativePreparationQueue: size(field(input, 'nativePreparationQueue')),
      sourceChunksWaiting: fieldSize(input, 'pendingSourceChunks'), coldRestores: fieldSize(input, 'coldRestores'),
      saveWaitCalls: Number(field(input, 'saveWaitCalls')), saveWaitNs: Number(field(input, 'saveWaitNanos')),
      unloadWaitCalls: Number(field(input, 'unloadWaitCalls')), unloadWaitNs: Number(field(input, 'unloadWaitNanos')) }
    try { result.publishedFields = publishedFields(level, input) }
    catch (error) { result.fieldDiagnosticsError = String(error) }
    return result
  } catch (error) { return { diagnosticsError: String(error) } }
}
function sample(server, phase) {
  var level = server.overworld(), chunks = level.getChunkSource(), map = chunks.chunkMap
  var iterator = map.getChunks().iterator()
  while (iterator.hasNext()) seen.add(iterator.next().getPos().toLong())
  var worker = field(map, 'worker'), mailbox = worker === null ? null : field(worker, 'mailbox')
  var result = { ticks: ticks, phase: phase,
    players: Number(server.getPlayerList().getPlayerCount()), levelPlayers: size(field(level, 'players')),
    loadedChunks: Number(chunks.getLoadedChunksCount()), uniqueTrackedChunks: Number(seen.size()),
    updatingChunks: fieldSize(map, 'updatingChunkMap'), pendingUnloads: fieldSize(map, 'pendingUnloads'),
    unloadQueue: size(field(map, 'unloadQueue')), ioWrites: size(worker === null ? null : field(worker, 'pendingWrites')),
    ioMailbox: size(mailbox === null ? null : field(mailbox, 'queue')),
    sourcesPlanted: planted, saves: saveCount, geometryMutations: geometryMutations,
    thermal: thermalSample(level),
    positions: players.map(function(p) { return [Number(p.x), Number(p.y), Number(p.z)] }) }
  write('telemetry.jsonl', result, true)
  console.info('FH_MEMORY_REPRO ' + JSON.stringify(result))
}
function setupServer(server) {
  try {
    var level = server.overworld()
    for (var lane = 0; lane < 3; lane++) {
      var x = 800 + lane * 1600, z = -800 - lane * 1600
      level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true)
      var top = Number(level.getHeight(HeightmapTypes.MOTION_BLOCKING_NO_LEAVES, x, z))
      if (top <= -64) throw new Error('Initial terrain was not generated before player placement')
      var identity = UUID.fromString('ba13dca0-0000-4000-8000-00000000000' + lane)
      var player = FakePlayers.get(level, new GameProfile(identity, 'MemoryWalker' + lane))
      var homeX = sourceGeometry === 'pockets' ? x - 5 : x
      var homeZ = sourceGeometry === 'pockets' ? z - 5 : z
      var homeY = sourceGeometry === 'pockets' ? top + 4 * Math.ceil(heatCount / 256) : top
      player.setPos(homeX + 0.5, homeY + 2, homeZ + 0.5)
      player.setGameMode(GameType.CREATIVE)
      player.getAbilities().invulnerable = true
      player.getAbilities().mayfly = true
      player.getAbilities().flying = true
      var roster = field(server.getPlayerList(), 'players')
      if (roster === null) throw new Error('Cannot access the actual mutable player roster')
      roster.add(player)
      var identities = field(server.getPlayerList(), 'playersByUUID')
      if (identities !== null) identities.put(identity, player)
      level.addNewPlayer(player)
      level.getChunkSource().move(player)
      players.push(player); environments.push(new EnvironmentSample()); bases.push([homeX, homeY, homeZ])
      for (var index = 0; index < heatCount; index++) {
        if (sourceGeometry === 'pockets') setup.push([x + 4 * (index % 16) + 1,
          top + 4 * Math.floor(index / 256) + 1, z + 4 * (Math.floor(index / 16) % 16) + 1])
        else setup.push([x + 4 * (index % 8), top, z + 4 * Math.floor(index / 8)])
      }
    }
    started = true
    sample(server, 'registered-three-players')
    write('scenario-ready.json', { players: 3, sourcesPerBase: heatCount, durationMinutes: minutes }, false)
  } catch (error) { failed = true; console.error('FH_MEMORY_SETUP_FAILED ' + error); write('scenario-failed.json', { error: String(error) }, false) }
}
ServerEvents.tick(function(event) {
  if (failed) return
  if (!started) { setupServer(event.server); return }
  try {
    ticks++
    var server = event.server, level = server.overworld()
    if (setup.length > 0) {
      for (var count = 0; count < 4 && setup.length > 0; count++) {
        var cell = setup.shift(), pos = new BlockPos(cell[0], cell[1], cell[2])
        if (sourceGeometry === 'pockets') {
          // Same four-cubed stone brick and two-block pocket as the existing
          // 1024-source overload regression, now on a normal terrain server.
          for (var by = -1; by <= 2; by++) for (var bz = -1; bz <= 2; bz++) for (var bx = -1; bx <= 2; bx++)
            level.setBlockAndUpdate(pos.offset(bx, by, bz), Blocks.STONE.defaultBlockState())
        }
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState())
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState())
        // Vanilla campfires default to LIT=true. Avoid Rhino's generic Comparable coercion.
        level.setBlockAndUpdate(pos, Blocks.CAMPFIRE.defaultBlockState())
        var entity = level.getBlockEntity(pos)
        var fuel = entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null)
        if (fuel === null) throw new Error('Real campfire fuel capability is missing')
        var leftover = fuel.insertItem(0, new ItemStack(Items.COAL, 64), false)
        if (Number(leftover.getCount()) >= 64) throw new Error('Campfire did not accept actual fuel')
        fires.push(pos)
        planted++
      }
      if (setup.length === 0) {
        exploreAtTick = ticks + warmupTicks
        mutationStartsAt = ticks + 400
        write('sources-ready.json', { ticks: ticks, sources: planted, explorationStartsAt: exploreAtTick,
          geometry: sourceGeometry, residentPlayer: residentPlayer }, false)
      }
    } else if (ticks > exploreAtTick && ticks <= minutes * 1200) {
      // Walking/sprinting speed. Paths keep entering new terrain rather than revisiting six sites.
      for (var lane = 0; lane < 3; lane++) {
        if (residentPlayer && lane === 0) continue
        var p = players[lane], speed = 0.28
        var dx = lane === 1 ? 0 : speed, dz = lane === 0 ? 0 : (lane === 1 ? -speed : speed)
        p.setPos(p.x + dx, p.y, p.z + dz)
        if (ticks % 20 === 0) level.getChunkSource().move(p)
      }
      // Periodically revisit a heated base, then resume travelling through new chunks.
      if (ticks % 2400 === 0) {
        // Player two continuously enters new terrain. Player one repeats the
        // base route so source chunks repeatedly unload and reload under pressure.
        var returning = residentPlayer ? 1 : Math.floor(ticks / 2400) % 3
        var base = bases[returning]
        players[returning].setPos(base[0] + 0.5, base[1] + 2, base[2] + 0.5)
        level.getChunkSource().move(players[returning])
      }
    }
    if (setup.length === 0 && mutationsPerCycle > 0 && ticks >= mutationStartsAt && ticks % mutationInterval === 0
        && (mutationStopTick === 0 || ticks <= mutationStopTick)) {
      mutationCycles++
      var replacement = ((mutationCycles & 1) === 1 ? Blocks.IRON_BLOCK : Blocks.STONE).defaultBlockState()
      for (var baseLane = 0; baseLane < 3; baseLane++) {
        for (var changed = 0; changed < Math.min(mutationsPerCycle, heatCount); changed++) {
          var sourceIndex = baseLane * heatCount + Math.floor(changed * heatCount / Math.min(mutationsPerCycle, heatCount))
          var foundation = fires[sourceIndex].below()
          if (level.getChunkSource().getChunkNow(foundation.getX() >> 4, foundation.getZ() >> 4) !== null) {
            level.setBlockAndUpdate(foundation, replacement)
            geometryMutations++
          }
        }
      }
    }
    for (var receiver = 0; receiver < players.length; receiver++) {
      var entity = players[receiver]
      var at = new BlockPos(Math.floor(entity.x), Math.floor(entity.y), Math.floor(entity.z))
      ThermalInput.gameplayPlayerEnvironment(entity, Temperature.naturalAir(level, at), environments[receiver])
    }
    if (ticks === 12000) {
      // Keep the resident base burning throughout a twenty-minute run. Only
      // touch currently loaded chunks and use the normal fuel capability.
      var refueled = 0
      for (var fire = 0; fire < fires.length; fire++) {
        var position = fires[fire]
        if (level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) === null) continue
        var campfire = level.getBlockEntity(position)
        if (campfire === null) continue
        var handler = campfire.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null)
        if (handler === null) continue
        handler.insertItem(0, new ItemStack(Items.COAL, 64), false)
        level.setBlockAndUpdate(position, Blocks.CAMPFIRE.defaultBlockState())
        refueled++
      }
      write('fuel-maintenance.json', { ticks: ticks, loadedCampfires: refueled }, false)
    }
    if (ticks % 6000 === 0) {
      // Non-flush save every five game minutes; durable backup-style flush every ten.
      server.saveEverything(true, ticks % 12000 === 0, false); saveCount++
    }
    if (ticks % 200 === 0) sample(server, setup.length > 0 ? 'placing-sources' : (ticks <= exploreAtTick ? 'heating-bases' : 'exploring-new-terrain'))
    if (ticks === minutes * 1200) write('scenario-finished.json', { ticks: ticks, saves: saveCount, uniqueTrackedChunks: Number(seen.size()), geometryMutations: geometryMutations }, false)
  } catch (error) { failed = true; console.error('FH_MEMORY_TICK_FAILED ' + error); write('scenario-failed.json', { ticks: ticks, error: String(error) }, false) }
})
