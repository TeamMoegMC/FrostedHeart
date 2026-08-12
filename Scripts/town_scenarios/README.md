# Town model scenarios

`baseline/stage12-one-day.json` is the reference input for the implemented stage 1–2 Java model.

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
