# Thermal production lifecycle test convergence

- Time: `2026-08-27 00:32:52 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal Minecraft input GameTests, production caller surface, lifecycle documentation, and production-caller convergence plan`

## Completed

- Deleted the copied Phase A GameTest capture implementation and removed the public test-only `MinecraftThermalInput.sealTick`, `geometryDeltas`, and `resolvedInputs` entrances.
- Reworked all thermal Minecraft integration tests to enter through real loaded-section capture/Page admission and `MinecraftThermalInput.sealActiveLevel`, then assert topology apply, arena state, source routing, mutation invalidation, and publication through production owners.
- Removed fake preallocated air cells and fixed invalid test assumptions about continuation watermarks, resolver output capacity, player networking, stale publication, source registration timing, section ownership, and tick-end mutation deadlines.
- Kept static vanilla shape coverage in `StateStaticThermalResolverTest`, which uses the same resolver as production capture.
- Updated `docs/climate/data-lifecycle-and-integration.md` and completed `plans/2026-08-26_22-17-53_thermal-production-caller-convergence.md`.

## Decisions

- A test caller does not justify a production API or a copied world-capture path.
- Source exactly-once correctness is asserted by comparing authoritative arena enthalpy with the actual `THERMAL_NODE` routed energy for every device. The test only requires the production timeline to advance; it does not assume GameTest callback time equals an exact 20-tick source interval.
- Static shape fixtures remain JUnit coverage for the production resolver; Minecraft lifecycle behavior remains Forge GameTest coverage for the production input path.

## Validation

- `./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --rerun-tasks --offline --console=plain`: successful; `175/175` thermal JUnit tests passed across `32` suites with zero failures, errors, or skips.
- `./gradlew.bat runGameTestServer --offline --console=plain`: successful; all `11/11` required Forge GameTests passed, including `10` thermal integration tests and `1` Frosted Research test. The final pass remained successful while the server reported being `49` ticks behind.
- Deleted-symbol and GameTest-bypass searches returned no matches. `git diff --check` reported no whitespace errors.

## Remaining

- Real-save multiplayer timing, gameplay value calibration, and profiler measurements remain separate release work; they are not replaced by these correctness tests.
