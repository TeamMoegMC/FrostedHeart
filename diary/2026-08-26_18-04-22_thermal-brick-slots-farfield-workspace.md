# Thermal Brick slot ownership and FarField workspace

- Time: `2026-08-26 18:04:22 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Minecraft thermal material topology ownership, FarField replacement allocation, tests, living documentation, and active thermal plan`

## Completed

- Replaced Page-wide material-pole and phase-reservoir slot maps with 64 immutable, insertion-ordered Brick fragments.
- Routed material/phase migration, outstanding phase-request copying, dirty material validation, applied phase-candidate lookup, and final sweep compilation through the owning Brick fragment.
- Added an applier-owned FarField workspace for union-find, open-patch evidence, natural temperatures, component summaries, decisions, continuation profiles, and active Page slots. It clears only the used prefix, grows geometrically, and shrinks only beyond four times current demand.
- Added repeated material/phase Brick rebuild and repeated FarField replacement coverage, then updated the living climate integration document and active implementation plan.

## Decisions

- Preserve the existing Page and `materialFragmentOrder` traversal, map insertion order, migration accumulation order, FarField union/classification loops, formulas, and boundary emission order.
- Preserve `H/C` when a surviving material pole changes capacity because its exposed area changes; the non-overlapping enthalpy remains explicit geometry ingress/egress rather than being hidden in the pole.
- Keep FarField connectivity globally recomputed. The workspace removes repeated scratch allocation without introducing incremental connectivity or another topology authority.

## Validation

- `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `208/208` passed.
- Forge GameTest: `14/14` required tests passed.
- Obsolete Page-wide slot-map and Brick-removal helper names have no production-source references.

## Remaining

- A controlled same-workload JFR can quantify the allocation reduction; no further semantic or ownership work remains for this optimization.
