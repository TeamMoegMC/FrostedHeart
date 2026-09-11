# Phase G Minecraft physical sources

- Time: `2026-08-25 01:08:38 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `dormant Campfire and Generator physical source profiles, lifecycle, exact port binding, topology-cut settlement, and validation`

## Completed

- Added frozen shadow profiles for Campfire at `1 kW` and Generator at `10 kW * TLevel`, with explicit convection, contact/internal, radiation/loss shares and no change to legacy HU, power, or `tempLevel` gameplay behavior.
- Added a dirty-only main-thread source manager with bounded cold Page ownership, chunk unload/revival, profile and anchor replacement, stable Generator observation, and concrete Campfire/Generator hooks.
- Bound air emission to the single exact component touching the declared face. Blocked ports use their profile policy; unloaded, unresolved, or ambiguous topology uses `DEGRADED_LOSS` instead of selecting an arbitrary air cell.
- Completed topology-cut source settlement and recovery: epoch-start commands are consumed before the interval, pre-applied energy is not injected twice, and deferred topology frames ACK an empty unresolved sweep so the single in-flight epoch cannot remain stuck.
- Added physical profile, source lifecycle, topology rebind, and exactly-once JUnit coverage plus two Forge GameTests for resolved physical ledgers and blocked/unresolved sink routing.
- Made ForgeGradle's `downloadMcpConfig` task skip execution under `--offline`, allowing the existing cached MCP artifact to support repeatable offline verification.

## Decisions

- Physical sources remain explicitly enabled dormant shadow inputs. Normal gameplay does not construct the manager and the legacy temperature path remains authoritative.
- A source observed before its target Page has a published topology is degraded by policy. The resolved-ledger GameTest therefore publishes its all-air topology before registering the sources, while preserving same-tick source registration semantics.
- Moving Create structures remain air while moving; static `hasDynamicShape=false` states remain trusted; unsupported dynamic geometry remains unresolved.

## Validation

- `gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` with Java 17: passed.
- JUnit XML: thermal `220/220`, repository `748/748`, with zero failures, errors, or skips.
- Forge GameTest: `18/18` required tests passed, including both Phase G physical source tests.
- Offline validation logged `downloadMcpConfig SKIPPED` and used the existing cached output.

## Documentation impact

- Updated `docs/climate/data-lifecycle-and-integration.md` with the Phase G lifecycle surface, dormant authority boundary, test coverage, and final counts.
- Updated `docs/climate/heat-production-and-network.md` with the physical power profiles, port shares, sink policies, and legacy isolation.
- Updated the thermal implementation plan outcome to mark Phase G / PR9 complete as dormant shadow work.

## Remaining

- Phase H / PR10 material boundaries is the next implementation stage.
- Production-like Phase 0b workload evidence and approved FarField profiles remain activation gates; gameplay query authority must not switch before those gates and later shadow workloads pass.
