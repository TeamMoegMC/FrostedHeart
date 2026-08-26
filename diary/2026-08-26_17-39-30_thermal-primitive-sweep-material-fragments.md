# Primitive thermal sweep and material fragments

- Time: `2026-08-26 17:39:30 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal topology replacement, ThermalSweep construction, Page material descriptions, living documentation, and active thermal plan`

## Completed

- Added a one-shot `ThermalSweep.Builder` that validates endpoints while writing pair, boundary, and phase operations directly into primitive columns.
- Changed `MinecraftThermalTopologyApplier` to stream cached Air pair fragments, FarField boundaries, material conductances, and phase contacts into the builder instead of creating global operation lists and copying them again in the sweep constructor.
- Stored material surfaces, phase surfaces, and stateless bridges as immutable per-Brick fragments. A 64-entry byte order preserves the previous remove-and-append traversal order while dirty refresh replaces only the affected fragment references.
- Updated the climate lifecycle documentation and active thermal plan.

## Decisions

- Preserve Page spatial order, per-Brick pair order, material linked-map insertion order, FarField classification, boundary/phase order, and forward/reverse sweep order exactly.
- Keep the existing list constructors for isolated solver callers and tests; only the production topology compiler uses the primitive builder.
- Do not change global FarField connectivity, arena ownership, active publication, formulas, scheduling, or topology authority in this optimization.

## Validation

- Java 17 offline Gradle `compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `207/207` passed with zero failures, errors, or skips.
- Forge GameTest: `14/14` required tests passed.
- `git diff --check` passed with only existing LF-to-CRLF warnings.

## Remaining

- Record a fresh controlled real-save JFR before deciding whether global FarField scratch, material slot maps, sweep replacement, or active publication is the next measured bottleneck.
