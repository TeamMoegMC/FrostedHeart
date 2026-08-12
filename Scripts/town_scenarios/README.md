# Town model scenarios

`baseline/stage12-one-day.json` is the reference input for the implemented stage 1–2 Java model.
The four `baseline/stage3-t1-*.json` files are the 1/8/24/48-resident,
120-day stage-3 references.
The three `baseline/stage4-t1-*.json` files are the 8/24/48-resident,
120-day × 1000-seed current-climate, one-T1-sphere references.
`experiments/stage4-t1-population-sweep.json` runs 100 evenly spaced integer
populations from 1 through 200 with paired seeds and writes ten selected
population reserve trajectories, concentrated around the stage-3 threshold.

Its experiments are deliberately independent:

- fixed workers produce one day of mining output;
- the T1 kernel is checked over an exact fuel-repeat period, without a town inventory;
- each hunting Monte Carlo run is one independent day with the configured carry input;
- the house consumes only its explicit `foodInventory` and does not consume the hunting sample;
- diagnostic arrays generate CSV points for the two committed figures.

This separation is the stage boundary. The simulator does not yet decide same-day building order, carry resident health into another day, change work eligibility, grow proficiency, assign jobs, or run climate/T2 logic. Those belong to stage 3 or later.

## Scenario-only quantities

- `coalToCokeCapacityPerDay`: maximum coal items converted to coke in the independent production day; `null` means unlimited.
- `rawMeatProcessingCapacityPerDay`: maximum raw-meat items converted to the matching cooked meat; `null` means unlimited.
- `initialLootRollCarry`: fractional roll budget at the start of the day, in `[0,1)` under normal game state.
- `availableHuntUnits`: maximum integer loot rolls allowed by the HUNT resource.
- house area is in floor blocks, volume in interior air blocks, temperature in °C, and food inventory in item-resource units.

FH-owned defaults never belong in scenario files. The Java simulator obtains those from `TownModelParameters.currentDefaults()`; gameplay obtains the same defaults through `FHConfig`.

## Stage-3 quantities

- `modelStage: 3` selects the multi-day Java loop.
- The baseline holds the house at `24 °C`; climate and T2 are not evaluated.
- Every resident starts housed. Home and work assignments then persist exactly as
  they do in `TeamTownData`; only vacant slots are automatically filled.
- `buildingOrder` is the stable order used for equal-priority daily settlements.
- `capacityItems` counts all item-resource units, including non-food hunting loot
  and non-coal mining coproducts.
- `coalToCokeItemsPerDay` and `rawMeatItemsPerDay` are scenario logistics limits;
  `null` means unlimited and does not model a particular machine.
- `cokeItem` and `tower.fuelItem` resolve the generator recipe's
  `forge:coal_coke` tag to the actual scenario item. TWR baselines use
  `immersiveengineering:coal_coke`.
- Initial food and coke are seven-day operational reserves. They affect survival,
  but never enter the structural fuel/food self-supply numerators.
- The T1 model uses the audited fuel recipe and exact finite 20-tick batch logic.
- `compareBuildingOrders` writes all six fixed-seed permutations to
  `order-comparison.csv`; it does not change the main Monte Carlo order.

## Stage-4 quantities

- `modelStage: 4` keeps the stage-3 resource/resident loop and replaces its fixed
  building temperature with current climate plus one T1 spherical heat field.
- `climateBurnInDays` discards the opening part of the generated long-term
  climate. The baseline uses 365 days and does not add the initial story blizzard.
- `morningHour` selects the only hourly thermal snapshot that affects daily town
  settlement. All other hours are risk observations.
- `location` declares dimension and biome temperature in °C. Current baseline is
  Overworld `-10 °C` plus TWR snowy plains `0 °C`, with altitude contribution
  disabled and every interior voxel above sea level.
- `thermalLayout.towerCenter` is the sole T1 sphere center `[x,y,z]`.
- Each thermal building declares one role (`house` or `hunt`), its floor area,
  and non-overlapping integer voxel boxes. A box is `{min:[x,y,z], size:[x,y,z]}`;
  the coordinates already contain height.
- House box voxel count and floor area must equal the stage-3 house volume and
  area. Stage 4 currently uses exactly one aggregate house and one aggregate
  hunting building.
- Runtime T1/climate defaults are not duplicated here. They come from
  `TownModelParameters.currentDefaults()` and share their source defaults with
  `FHConfig.SERVER.CLIMATE` / `FHConfig.SERVER.TOWN.GENERATOR_T1`.
- An optional `populationSweep` object selects a range, number of curve points,
  and explicit `trajectoryPopulations`. Each requested population gets a
  three-block-high balanced integer rectangle whose current house/hunting
  capacity formula is at least that population. Initial edible food scales with
  population to preserve reserve days; the T1 coke inventory remains fixed
  because tower demand is independent of population.
- Sweep output is `population.csv`, `reserve-trajectories.csv`, and a
  self-defining `summary.json`. All populations reuse the same run seeds, so
  their differences are paired comparisons under the same climate samples.
