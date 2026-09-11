# Thermal internal sink removal

- Time: `2026-08-31 03:28:41 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `physical-source port and binding contracts`

## Completed

- Deleted `INTERNAL_HEAT`, `INTERNAL_RESERVOIR`,
  `MissingPortPolicy`, and their factories and resolution branches.
- Reassigned generator power to `80%` exhaust convection and `20%`
  radiation; radiator now uses `90%` convection and `10%` radiation.
- Made blocked Air ports explicit loss while retaining degraded loss for
  topology-unavailable targets.

## Decisions

- No unused future machine-reservoir contract remains. A real machine thermal
  mass would require an explicit energy, capacity, temperature, and return-flow
  owner and is not represented by a sink enum.

## Validation

- Production, JUnit, and GameTest source compilation passed on Java 17.
- Focused thermal JUnit passed.
- Forge GameTest passed: `14/14` required tests.
- Production and test source searches contain no removed contract names.

## Remaining

- The generator analytic fallback field remains a separate gameplay design
  decision.
