# Dedicated dev server: companion mods and RCON driving

- Time: `2026-11-09 02:06:34 -0600`
- Author: `Claude; Anthropic/claude-opus-5-5; dev tooling`
- Status: `completed`
- Scope: `build.gradle` (companion runtime dependencies, `gameTestServer` mod sources), `Scripts/rcon.py`, `Scripts/dev_server_nogui.init.gradle`, `README.md`

## Completed

- `./gradlew runServer` crashed in `ResearchCatalog.load` (`FHServerEvents.serverAboutToStart`). The cause: `run/config/fhresearches` references `kubejs:servo_core`, `charcoal_pit:straw`, `stone_age:fat`, `immersiveindustry:car_kiln_brick` and similar items, but the companion mods were only `clientRuntimeOnly`. The same five coordinates are now also added as `serverRuntimeOnly`, from a single list.
- `gameTestServer` no longer lists `sourceSets.server`, which is an empty source set with no other dependencies. This keeps GameTests independent of the companion mods; they keep using `src/gametest/catalog`.
- Added `Scripts/rcon.py`, a standard-library RCON client. It reads `rcon.port` and `rcon.password` from a `server.properties` path, so the password never appears on the command line. It reassembles multi-packet replies.
- Added the opt-in `Scripts/dev_server_nogui.init.gradle`. It suppresses the Swing console window when the server runs in the background.
- Documented the workflow in `README.md`. The same notes are in `.claude/memory/architecture.md`, but `.claude/` is git-ignored, so that copy is local only.

## Decisions

- Vanilla `RconClient` rejects a socket read that contains more than one packet, and drops the connection. The client therefore sends its end-of-reply marker only after the first reply packet arrives. Pipelining the command and the marker worked for the first command and then lost the connection.
- `--nogui` stays in an init script instead of `build.gradle`, so the IDE/server-console experience for other contributors is unchanged.
- RCON is enabled only in the git-ignored `run/server.properties`. No defaults were added to the repo.

## Validation

- `./gradlew runServer -I Scripts/dev_server_nogui.init.gradle`:
  - All five companion jars loaded, launch args contained `--nogui`, and the server reached `Done` with `RCON running on 0.0.0.0:25575`.
  - Over `Scripts/rcon.py`, `forge tps`, `help town` and `stop` worked, with 20 TPS on every dimension. All dimensions saved on stop.
  - Before the fix, the same run crashed with the catalogue error above.
- An earlier run with the scratch client confirmed that the 5.4 KB `help` output (more than one 4096-byte packet) came back complete.
- `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_buildings`: all 17 required tests passed, and the log showed no companion jars.
- `./gradlew dependencies --configuration clientRuntimeClasspath` still resolves all five companion coordinates.
- Documentation impact: `README.md` run section updated, plus the local, git-ignored `.claude/memory/architecture.md`. No living game-system document under `docs/` was affected.

## Remaining

- Most FH commands (`/town`, `/temperature`, `/research`) need an online player. Driving them over RCON needs a client connected to the dedicated server, for example through Quick Play, which is not wired up yet.
- `runClient` itself was not relaunched. Only its dependency resolution was checked.
