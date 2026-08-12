# Town model scenarios

`baseline/stage12-one-day.json` is the reference input for the implemented stage 1–2 Java model.
The four `baseline/stage3-t1-*.json` files are the 1/8/24/48-resident,
120-day stage-3 references.

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
