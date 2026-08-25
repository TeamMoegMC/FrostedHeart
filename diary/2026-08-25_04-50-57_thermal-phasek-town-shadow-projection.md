# Thermal Phase K town shadow projection

- Time: `2026-08-25 04:50:57 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `town building-scan projection, passive aggregate query, shadow comparison, climate/town documentation and thermal implementation plan`

## Completed

- Added `TownThermalProjection`, which compresses the air positions already visited by a town structure scan into world-aligned `4x4x4` base-Brick groups containing one deterministic real-air representative and an integer voxel weight.
- Connected only the active legacy temperature consumers, `HouseBlockScanner` and `HuntingBaseBlockScanner`. Their existing per-voxel `WorldTemperature.block` average remains unchanged and authoritative; successful scans now submit one read-only shadow observation after that same traversal.
- Added passive weighted town aggregation and bounded `TownShadowSnapshot` counters to `MinecraftThermalInput`. Complete group coverage records a legacy/new average error; partial coverage reports `QUERY_PARTIAL_REGION` and voxel confidence without entering the error aggregate.
- Kept `MineBaseBlockScanner` and the inactive `MineBlockScanner` outside the observation path, so mine scans do not allocate or populate unused thermal projections.
- Updated the living town/climate documentation and Phase K implementation snapshot.

## Decisions

- Town aggregation cannot scan a room, execute a second voxel traversal, retain voxel positions, create Page/Brick/Cell/Interest, or hold a mesh lease. It reads only already-published cells and naturally misses when no Page exists.
- Base-Brick grouping is deliberately a shadow projection rather than an exact replacement for the legacy voxel average. Gameplay authority cannot switch until Phase 0b evidence, FarField approval, the surface compositor, and gameplay/reference calibration are complete.
- No generic scanner callback or town-consumer hierarchy was added. Projection collection lives directly in the two scanners that already calculate gameplay temperature.

## Validation

- `.\gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` passed after the final active-consumer-only refinement.
- Repository JUnit: `810/810`; thermal JUnit: `237/237`; Forge GameTest: `19/19` required.
- The Minecraft input scenario proves two town voxels collapse to one weighted group lookup and 4,096 passive missing groups preserve Page count, arena high-water mark, live-cell count, and total enthalpy.

## Remaining

- Continue Phase K with the HUD consumer while preserving legacy gameplay authority.
- Collect production-like Phase 0b town projection cardinality and shadow-error evidence before considering any grouping refinement or authority switch.
