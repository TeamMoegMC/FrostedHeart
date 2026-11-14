/* Scripted Minecraft 1.20.1 / Forge FML3 clients for local server load tests.
 * These exercise real sockets, ServerPlayer ticks, tracking and packet delivery.
 * They do not render, run client mods, or qualify human gameplay by themselves.
 * Dependency: npm install --prefix <directory> minecraft-protocol
 */
'use strict'

const fs = require('node:fs')
const path = require('node:path')
const { createRequire } = require('node:module')

const options = {
  host: '127.0.0.1', port: 25585, clients: 100, seconds: 120,
  'ramp-ms': 200, 'join-timeout': 180, 'view-distance': 6,
  modules: 'build/thermal-network-clients/dependencies',
  output: 'build/thermal-network-clients', movement: 'stationary', 'server-result': ''
}
for (let index = 2; index < process.argv.length; index += 2) {
  const name = process.argv[index].replace(/^--/, '')
  if (!(name in options) || process.argv[index + 1] === undefined) {
    throw new Error(`Unknown or incomplete option: ${process.argv[index]}`)
  }
  options[name] = typeof options[name] === 'number' ? Number(process.argv[index + 1]) : process.argv[index + 1]
}
for (const key of ['port', 'clients', 'seconds', 'join-timeout', 'view-distance']) {
  if (!Number.isInteger(options[key]) || options[key] <= 0) throw new Error(`Invalid --${key}`)
}
if (!Number.isInteger(options['ramp-ms']) || options['ramp-ms'] < 0 || !['stationary', 'walk'].includes(options.movement)) {
  throw new Error('Use a nonnegative ramp and --movement stationary or walk')
}
const dependency = createRequire(path.resolve(options.modules, 'package.json'))
const minecraft = dependency('minecraft-protocol')
const output = path.resolve(options.output)
fs.mkdirSync(output, { recursive: true })
const events = fs.createWriteStream(path.join(output, 'events.jsonl'))
const started = Date.now()
const entries = []
let joined = 0, active = 0, maximumActive = 0, allJoinedAt = 0, stopping = false

function event (type, detail = {}) {
  events.write(JSON.stringify({ elapsedMs: Date.now() - started, type, ...detail }) + '\n')
}

function varInt (value) {
  const bytes = []
  do {
    const next = value & 127
    value >>>= 7
    bytes.push(next | (value ? 128 : 0))
  } while (value)
  return Buffer.from(bytes)
}

function textBytes (value) {
  const bytes = Buffer.from(value, 'utf8')
  return Buffer.concat([varInt(bytes.length), bytes])
}

function cursor (data) {
  let offset = 0
  return {
    integer () {
      let value = 0, shift = 0, byte
      do {
        if (offset === data.length || shift >= 35) throw new Error('Incomplete Forge VarInt')
        byte = data[offset++]
        value |= (byte & 127) << shift
        shift += 7
      } while (byte & 128)
      return value >>> 0
    },
    text () {
      const length = this.integer()
      if (length > data.length - offset) throw new Error('Incomplete Forge text')
      const value = data.toString('utf8', offset, offset + length)
      offset += length
      return value
    },
    bytes (length) {
      if (length > data.length - offset) throw new Error('Incomplete Forge wrapper')
      const value = data.subarray(offset, offset + length)
      offset += length
      return value
    }
  }
}

function handshake (entry, packet) {
  const client = entry.client
  if (packet.channel !== 'fml:loginwrapper') {
    client.write('login_plugin_response', { messageId: packet.messageId })
    return
  }
  const wrapper = cursor(packet.data)
  const channel = wrapper.text()
  const data = wrapper.bytes(wrapper.integer())
  if (channel !== 'fml:handshake' || data.length === 0) {
    throw new Error(`Unsupported test-client login channel ${channel}`)
  }
  const discriminator = data[0]
  entry.loginPackets[discriminator] = (entry.loginPackets[discriminator] || 0) + 1
  let response
  if (discriminator === 1) {
    const body = cursor(data.subarray(1))
    const mods = Array.from({ length: body.integer() }, () => body.text())
    const channels = Array.from({ length: body.integer() }, () => [body.text(), body.text()])
    // FML3: reply type 2 contains mod ids, channel versions and known registry hashes.
    // This headless client consumes registry payloads but has no registry cache.
    // Frosted Heart's C2SModListReplyMixin prefixes the ordinary mod list
    // with an optional cluster-token flag. Local test clients have no token.
    const extension = mods.includes('frostedheart') ? Buffer.from([0]) : Buffer.alloc(0)
    response = Buffer.concat([
      Buffer.from([2]), extension, varInt(mods.length), ...mods.map(textBytes),
      varInt(channels.length), ...channels.flatMap(([name, version]) => [textBytes(name), textBytes(version)]),
      varInt(0)
    ])
    if (entry.name === 'FHLoad001') event('forge-mod-list', {
      mods, channels, responseBytes: response.length, responsePrefix: response.subarray(0, 96).toString('hex')
    })
    entry.channels = channels.map(([name]) => name)
  } else if (discriminator === 3 || discriminator === 4) {
    // Registry/config delivery is complete when its entire framed payload arrives.
    // Forge's C2S acknowledgement is discriminator 99, without a payload.
    response = Buffer.from([99])
  } else if (discriminator === 5) {
    // Forge marks mod-data as noResponse and does not wait for an acknowledgement.
    return
  } else {
    throw new Error(`Unsupported Forge login discriminator ${discriminator}`)
  }
  client.write('login_plugin_response', {
    messageId: packet.messageId,
    data: Buffer.concat([textBytes(channel), varInt(response.length), response])
  })
}

function markOnline (entry) {
  if (entry.online || !entry.login || !entry.position) return
  entry.online = true
  entry.joined = true
  entry.joinedAt = Date.now() - started
  active++
  joined++
  maximumActive = Math.max(maximumActive, active)
  event('spawned', { name: entry.name, active })
  if (joined === options.clients) {
    allJoinedAt = Date.now()
    event('all-spawned', { clients: joined })
    console.log(JSON.stringify({ event: 'all-spawned', clients: joined, elapsedMs: Date.now() - started }))
  }
}

function connect (index) {
  if (stopping) return
  const name = `FHLoad${String(index + 1).padStart(3, '0')}`
  const entry = {
    name, online: false, joined: false, packets: 0, chunkPackets: 0,
    customPackets: 0, loginPackets: {}, teleports: 0, movementPackets: 0,
    errors: [], channels: [], readBytes: 0, writtenBytes: 0, respawns: 0
  }
  entries.push(entry)
  const client = minecraft.createClient({
    host: options.host, port: options.port, version: '1.20.1', username: name,
    auth: 'offline', keepAlive: true, hideErrors: true, disableChatSigning: true,
    closeTimeout: 120000, checkTimeoutInterval: 60000,
    // Forge adds recipe/ingredient serializers that the vanilla protocol
    // library cannot interpret. A headless load client consumes the framed
    // recipe table as bytes; it never displays or crafts from these recipes.
    customPackets: { '1.20': { play: { toClient: { types: { packet_declare_recipes: 'restBuffer' } } } } }
  })
  entry.client = client
  client.tagHost = '\0FML3\0'
  client.removeAllListeners('login_plugin_request')
  client.on('login_plugin_request', packet => {
    try { handshake(entry, packet) } catch (error) {
      entry.errors.push(error.message)
      event('handshake-error', { name, message: error.message })
      client.end(error.message)
    }
  })
  client.on('packet', (_packet, meta) => {
    entry.packets++
    if (meta.name === 'map_chunk') entry.chunkPackets++
    if (meta.name === 'custom_payload') entry.customPackets++
  })
  client.on('login', () => {
    entry.login = true
    client.write('settings', {
      locale: 'zh_cn', viewDistance: options['view-distance'], chatFlags: 0,
      chatColors: true, skinParts: 127, mainHand: 1,
      enableTextFiltering: false, enableServerListing: true
    })
    client.write('custom_payload', { channel: 'minecraft:brand', data: textBytes('fh-load-client') })
    // Match NetworkHooks.sendMCRegistryPackets: Minecraft's registration control
    // channels are not registered as mod payload channels themselves.
    const modChannels = entry.channels.filter(name => !name.startsWith('minecraft:'))
    client.write('custom_payload', { channel: 'minecraft:register', data: Buffer.from(modChannels.join('\0')) })
    markOnline(entry)
  })
  client.on('position', packet => {
    const previous = entry.position || { x: 0, y: 0, z: 0, yaw: 0, pitch: 0 }
    const position = {}
    for (const [key, mask] of [['x', 1], ['y', 2], ['z', 4], ['yaw', 8], ['pitch', 16]]) {
      position[key] = packet[key] + ((packet.flags & mask) ? previous[key] : 0)
    }
    entry.position = position
    entry.teleports++
    client.write('teleport_confirm', { teleportId: packet.teleportId })
    client.write('position_look', { ...position, onGround: true })
    markOnline(entry)
  })
  client.on('update_health', packet => {
    if (packet.health <= 0) {
      entry.respawns++
      client.write('client_command', { payload: 0 })
      event('respawn-requested', { name, count: entry.respawns })
    }
  })
  client.on('error', error => {
    entry.errors.push(error.message)
    event('client-error', { name, message: error.message, field: error.field, stack: error.stack })
  })
  client.on('kick_disconnect', packet => {
    entry.kick = packet.reason
    event('kick', { name, reason: packet.reason })
  })
  client.on('end', reason => {
    if (entry.online) active--
    entry.online = false
    entry.endedDuringHold = !stopping
    entry.endReason = reason
    entry.endedAt = Date.now() - started
    if (client.socket) {
      entry.readBytes = client.socket.bytesRead
      entry.writtenBytes = client.socket.bytesWritten
    }
    event('ended', { name, reason, active, expected: stopping })
  })
}

function finish (reason) {
  if (stopping) return
  stopping = true
  clearInterval(ticker)
  const activeBeforeStop = active
  for (const entry of entries) {
    if (entry.client.socket) {
      entry.readBytes = entry.client.socket.bytesRead
      entry.writtenBytes = entry.client.socket.bytesWritten
    }
    entry.client.end('Load test finished')
  }
  const summary = {
    options, reason, elapsedSeconds: (Date.now() - started) / 1000,
    heldAllClientsSeconds: allJoinedAt ? (Date.now() - allJoinedAt) / 1000 : 0,
    requested: options.clients, created: entries.length, joined, maximumActive, activeBeforeStop,
    qualified: ['duration-complete', 'server-complete'].includes(reason) && activeBeforeStop === options.clients &&
      entries.every(entry => entry.joined && !entry.endedDuringHold && entry.errors.length === 0),
    scope: 'Real headless network clients; no rendering or client-mod logic; no endurance or thermal qualification implied.',
    clients: entries.map(({ client, ...values }) => values)
  }
  fs.writeFileSync(path.join(output, 'summary.json'), JSON.stringify(summary, null, 2))
  event('complete', { reason, qualified: summary.qualified, joined, maximumActive, activeBeforeStop })
  events.end()
  console.log(JSON.stringify({ event: 'complete', reason, qualified: summary.qualified, joined, maximumActive, output }))
  setTimeout(() => process.exit(summary.qualified ? 0 : 1), 1500)
}

let lastReport = started
const ticker = setInterval(() => {
  const now = Date.now()
  if (options['server-result'] && fs.existsSync(options['server-result'])) return finish('server-complete')
  if (allJoinedAt && now - allJoinedAt >= options.seconds * 1000) return finish(options['server-result'] ? 'server-result-timeout' : 'duration-complete')
  if (!allJoinedAt && now - started >= options['join-timeout'] * 1000) return finish('join-timeout')
  if (entries.length === options.clients && entries.every(entry => entry.endedAt !== undefined)) return finish('clients-disconnected')
  if (now - lastReport >= 10000) {
    lastReport = now
    console.log(JSON.stringify({ event: 'progress', created: entries.length, joined, active, elapsedSeconds: (now - started) / 1000 }))
  }
  for (const [index, entry] of entries.entries()) {
    if (!entry.online || !entry.position) continue
    // Keep the same current position for stationary clients; walk clients move
    // slowly back and forth. Server collision/teleport corrections remain active.
    if (options.movement === 'walk') {
      const direction = (Math.floor((now - allJoinedAt) / 3000) + index) % 2 === 0 ? 1 : -1
      entry.position.x += 0.06 * direction
    }
    entry.client.write('position_look', { ...entry.position, onGround: true })
    entry.movementPackets++
  }
}, 50)

for (let index = 0; index < options.clients; index++) setTimeout(() => connect(index), index * options['ramp-ms'])
process.on('SIGINT', () => finish('interrupted'))
process.on('SIGTERM', () => finish('interrupted'))
