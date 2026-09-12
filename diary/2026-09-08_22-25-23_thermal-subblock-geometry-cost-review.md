# Internal block geometry cost review

- Time: `2026-09-08 22:25:23 +08:00`
- Author: `Codex; OpenAI GPT-6; primary review agent`
- Status: `completed`
- Scope: `current microcell geometry memory/CPU accounting and clarified block-level model requirement`

## Completed

- Recorded the user's requirement to remove internal Air/solid subdivision of stairs and other blocks while retaining block-level effective thermal properties and unknown-block ventilation 75 in the [working plan](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md).
- Derived mixed-geometry storage from actual arrays, separated geometry rebuild/migration from steady node/edge solving, and identified shared versus per-Brick/per-slot costs.

## Decisions

- Report formulas, layout assumptions, and hypothetical population examples rather than invent measured server memory or milliseconds.
- Preserve the benefit of current homogeneous Air aggregation in the replacement; removing internal subdivision must not blindly expand every Brick to 64 solver nodes.

## Validation

- Read `ComponentBrickCompiler`, `ThermalCellArena`, `ThermalPhaseReservoirStore`, `PageSignatures`, `BrickMaterialKernel`, `BrickMigrationKernel`, and `ThermalFragment`.
- Checked arithmetic for the 1,508-byte example geometry payload, approximately 1,728-byte object layout, and 104-byte slot-array payload including current phase metadata.
- No Java changes, tests, heap profiling or performance benchmarks. Documentation impact is the working plan and this diary; living implemented-system documentation is unchanged.

## Remaining

- Design the block-level replacement and measure it against matching production worlds before claiming total savings or speedup.
