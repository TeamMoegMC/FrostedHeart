# RC semantic compiled thermal program plan revision

- Time: `2026-08-23 16:10:30 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Integrated the RC semantic-IR and compiled thermal-program discussion into the current climate/temperature architecture plan.
- Revised the decision summary, runtime lifecycle, solver model, package boundaries, implementation phases, validation matrix, production gates, performance model, open questions, and outcome.

## Decisions

- RC graphs are compile-time semantic IR; production runtime executes immutable `CompiledThermalPlan` kernels with per-instance enthalpy, input, phase, witness, and portal state.
- Strictly equivalent graph reduction and bounded canonical plan sharing are allowed; stateful or observable thermal history cannot be silently removed.
- Unguarded linear programs advance on events by reproducing canonical discrete steps. Phase, nonlinear, dynamic-portal, and otherwise unsafe programs remain in guarded fixed solve buckets.
- Topology changes align affected state at the change tick and use an explicit `TopologyMigrationPlan` to preserve or account for enthalpy and source/portal state.

## Validation

- Reviewed the integrated plan for obsolete all-active fixed-bucket terminology and conflicting lifecycle statements.
- Ran `git diff --check` on the plan; no content errors were reported. Git only reported the existing LF-to-CRLF working-copy warning.
- Documentation-only change; Java tests were not run because implemented behavior and living climate documentation did not change.

## Remaining

- Execute Phase 0 baselines and build the canonical graph/reference-step oracle before implementing the semantic compiler or production runtime.
