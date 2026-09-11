# Thermal Phase A Dispatch And Evidence Closure

- Time: `2026-08-24 14:45:23 +08:00`
- Author: `Codex; primary integration agent`, assisted by `phasea_core_contract` (`OpenAI gpt-5.6-sol`, `ultra`)
- Status: `completed`
- Scope: `content.climate.thermal.profile`, Phase A resolver census, JMH/JFR/JOL evidence, living climate documentation, and the active thermal plan

## Completed

- Added immutable `ThermalSignatureResolverDispatcher` registration and dispatch for explicit override, automatic state-static fallback, bounded contextual resolver, and observable unsupported routes.
- Kept moving piston as a hard `UNRESOLVED_DYNAMIC` exclusion even if an explicit or contextual resolver is registered. Static modded states remain automatically trusted by the generic resolver; no per-mod adapter work was added.
- Extended the active Forge census with one non-neutral explicit fixture and one bounded `SELF + EAST` contextual fixture with two conservative closed outputs.
- Added a two-generation registry rebuild prototype with deterministic ID comparison and simultaneous old/new cardinality reporting.
- Added `thermalPhaseAJmh`, `thermalPhaseAJfr`, `thermalPhaseARetainedHeap`, `thermalPhaseAValidateJmhReport`, `thermalPhaseAEnvironmentManifest`, and the aggregate `thermalPhaseAEvidence` task.
- Updated `docs/climate/data-lifecycle-and-integration.md` and the active implementation plan with the implemented behavior, measured costs, limitations, and next phase.

## Decisions

- Resolver priority is moving-piston hard exclusion, explicit override, generic static fallback, registered contextual, then observable unsupported.
- Resolver registration freezes a canonical namespaced ID, declared dependency mask, and maximum output regions. Duplicate lane bindings and conflicting IDs are rejected before a dispatcher snapshot is built.
- The contextual fixture is a contract/census input, not a Bamboo compatibility adapter. Its output closes uncertain airflow and does not establish a per-mod maintenance obligation.
- Phase A PR 1/2 is complete as a correctness prototype. Gameplay-calibrated material/source profiles remain PR 9/10, and datapack/world production wiring remains PR 8.
- The observed maximum region count remains `4`, but production `Rmax` and packed widths remain unfrozen; correctness IDs stay `int`.

## Validation

- Java 17 `compileJava`, `compileTestJava`, `jmhClasses`, and thermal JUnit: `89/89` passed with zero failures, errors, or skips.
- Forge GameTest: all `15/15` required tests passed.
- Dispatcher census: `2,392` blocks, `84,147` states, `82,210` resolved, `1,925` unregistered dynamic, `12` moving-piston unresolved, `262` complete signatures, `259` geometry patterns, `2` contextual outputs, and maximum observed local regions `4`.
- Registry rebuild: first census `480,784,500 ns`, reload pass `219,870,300 ns`, `262 + 262` simultaneous signatures, deterministic IDs.
- JMH sample p95/p99: explicit/contextual resolver `100/100 ns`; generic air `1.5/1.9 us`; generic fence `1.6/2.0 us`; Brick fixtures `15.09..24.58 us` p95 and `20.00..28.48 us` p99.
- JOL: one synthetic census-cardinality registry `41,000 B`, old/new overlap `79,936 B`, and compiled Brick graphs `1,960..3,816 B`. JFR artifact size was about `4.4 MB`.
- The first aggregate evidence invocation completed JMH but exposed an incorrect percentile-key check (`0.95/0.99` instead of JMH JSON's `95.0/99.0`). The checker was fixed, the existing benchmark artifact was revalidated without rerunning it, and the remaining aggregate evidence tasks completed successfully.

## Remaining

- Implement Phase B / PR 3: `ThermalPage`, `int coverageRef[64]`, coarse/fine face ownership, geometry coalescing, and signed geometry ingress/egress ledgers.
- Keep Phase 0b production-like multiplayer and whole-server retained-heap evidence as a separate backlog; do not optimize the legacy sampler further.
- Do not enable V1 gameplay authority until FarField, runtime publication/mailbox, global memory admission, production integration, and shadow gates pass.
