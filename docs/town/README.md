# Town System Documentation

The town system covers resident recruitment and simulation, housing, work assignment and production, food and nutrition, heating, daily settlement, warehouse interaction, and the Mayor's Seal management interface.

## Reading Map

| Document | Use it for | Documentation status |
|---|---|---|
| [town-model.md](town-model.md) | Exact production, food, housing, temperature, settlement, observability, staffing, and simulation formulas | Transitional. It is the most complete reference, but it also contains experiment history and future simulation stages that must not be mistaken for implemented behavior. |
| [hybrid-simulation-architecture.md](hybrid-simulation-architecture.md) | Citizen data layout, scheduling, behavior, movement, synchronization, rendering, interaction, and persistence | Transitional. It began as an architecture proposal and accumulated implementation updates; verify remaining roadmap material against source. |

## Deprecated Predecessors

The [earlier TWR numerical model design](../deprecated/TWR%E5%9F%8E%E9%95%87%E6%95%B0%E5%80%BC%E6%A8%A1%E5%9E%8B%E8%AE%BE%E8%AE%A1.md) preserves historical self-sufficiency and thermal-model reasoning. It has been superseded by later implementation and modeling work and is not part of the current reading path.

## Primary Code Anchors

- `com.teammoeg.frostedheart.content.town`
- `TeamTown`, `TeamTownData`, and `TownModelParameters`
- `TownStageOneTwoScenario`, `TownStageThreeScenario`, and `TownStageFourScenario`
- `HouseBuilding`, `HuntingBaseBuilding`, `MineBaseBuilding`, and `MineBuilding`
- `CitizenSim`, `CitizenSimScheduler`, and citizen synchronization/rendering classes
- `FHConfig.SERVER.TOWN`

Town behavior also depends on climate and temperature code, recipe and loot data, and companion-modpack configuration. Inspect both repositories when a model input crosses that boundary.

## Transition Work

Future revisions should split current gameplay mechanics from simulator instructions, historical experiment results, and unimplemented proposals. Until then, use each active document's explicit stage/status statements and confirm exact behavior in code before making decisions.
