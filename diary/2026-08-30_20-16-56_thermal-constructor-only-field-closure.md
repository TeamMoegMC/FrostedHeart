# Thermal constructor-only field closure

- Time: `2026-08-30 20:16:56 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `MinecraftThermalInput construction ownership and semantic field reachability`

## Completed

- Removed the `MinecraftThermalInput.signatureCapture` field. Construction now
  uses a local `MinecraftSignatureCapture` and passes it to
  `MinecraftPageManager` and `MinecraftPhaseController`, which already own the
  two required lifetime references.
- Extended the compiled field audit from write/read counts to the methods that
  perform each read. The thermal production tree now has no field whose only
  reads occur in its owning constructor.

## Decisions

- Constructor wiring does not justify retaining a duplicate dimension-lifetime
  field when all runtime consumers already own the dependency.

## Validation

- Java 17 `compileJava` and `compileTestJava`: passed.
- Thermal JUnit: `96/96` passed.
- Compiled constructor-only field set: empty.
- `git diff --check`: passed.

## Documentation

- No living-document update was needed; runtime behavior and ownership did not
  change.

## Remaining

- Controlled JFR/heap profiling remains the existing performance-evidence task.
