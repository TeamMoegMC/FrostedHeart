# Thermal Runtime Documentation Refresh

- Time: `2026-08-29 01:50:20 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `docs/climate` living documentation and current thermal validation record

## Completed

- Reconciled climate documentation with the current asynchronous Page/Brick
  runtime, local Air/FarField topology, indexed physical sources, and 20-tick
  worker cadence.
- Removed stale references to deleted component/forest state, old thermal
  coordinators, the old source manager, and the obsolete continuation cap.
- Recorded the current functional validation: `108/108` thermal JUnit tests and
  `14/14` Forge GameTests, with controlled JFR/heap profiling kept as the
  remaining performance evidence.

## Decisions

- No architecture or production code changed in this documentation-only pass.
- Absolute performance optimality remains a measured claim; the documented
  structural bounds are the current acceptance contract.

## Validation

- `gradlew.bat test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain`: passed, `108/108`.
- `gradlew.bat compileGameTestJava --offline --console=plain`: passed.
- `gradlew.bat runGameTestServer --offline --console=plain`: passed, `14/14` required tests.
- `git diff --check -- docs/climate`: passed; only existing line-ending warnings.
- Searched all `docs/` files for deleted thermal class names; no stale matches remain.

## Remaining

- Controlled post-change JFR and retained-heap workload evidence is still
  required before publishing hardware-specific performance numbers.
