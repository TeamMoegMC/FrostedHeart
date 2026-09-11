# General gameplay analytic-fields architecture review

- Time: `2026-09-08 18:28:06 +08:00`
- Author: `Codex; OpenAI GPT-6; primary design agent`
- Status: `completed`
- Scope: `design and engineering review of generic analytic temperature fields; no Java implementation`

## Completed

- Wrote the [general architecture plan](../plans/2026-09-08_18-28-06_gameplay-analytic-fields-architecture.md) and marked the earlier generator-specific draft superseded.
- Traced current field publication, runtime/profile reload, Curiosity state, world capability availability, team-owned generator progression, consumer fallbacks, and legacy heating ownership.
- Answered why field ownership should be separated and compared both the current list and older heat-chunk partitioning without claiming unmeasured performance gains.

## Decisions

- World lifetime owns one general field index; physical runtime caches the same reference. Extraction addresses lifecycle coupling and unnecessary dimension-runtime startup, not steady-query speed by itself.
- General namespaced identity and an ordered list plus update map support generator, boss, and command producers. Repeated unchanged reports avoid allocations and sorting; query complexity remains O(F).
- Generator remains a normal physical source, with a natural-relative gameplay floor published from the existing team-data enumeration. Provider reconciliation handles deleted holders; source chunk unload does not delete authoritative team heat.
- Explicit controls run after relative floors. The old `BlockTemperatureModel.applyHeat` curve is a separate balance formula, not a compulsory shared-field implementation.
- Analytic bounds can authorize their own legacy warming transitions without writing worker enthalpy or indiscriminately bypassing phase ownership. Real production acceptance must cover this behavior.

## Validation

- Reviewed current Java paths and plan links; no build, GameTest, JUnit, or benchmark was run for this design-only task.
- Documentation impact: new plan, old plan supersession, and this append-only diary entry. Living documentation remains unchanged because production behavior did not change.

## Remaining

- Implement the plan and run the listed real Forge GameTest/normal-server scenarios. Measure allocation, timing, heap, field populations, and source-connected enclosure heating before claiming performance or balance results.
