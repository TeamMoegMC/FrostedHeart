# Thermal loaded-world runtime fix

- Time: `2026-09-08 02:28:51 +08:00`
- Author: `Codex; OpenAI GPT-6; primary implementation agent`
- Status: `completed`
- Scope: `thermal runtime startup, physical source discovery, owner allocation, and real Forge validation`

## Completed

- Implemented the [loaded-world plan](../plans/2026-09-08_00-38-47_thermal-runtime-loaded-world-reconciliation.md): lazy startup attaches existing sections, and startup/load/exact refusal share one ordered pending chunk map with eight attempts per tick.
- Removed campfire discovery polling and Page-admission discovery. Pending/live BE positions discover state sources without instantiating their entities. Machines retain their normal complete production publication.
- Applied enabled/positive-power retention across Minecraft profiles, excluded zero-share convection seeds, and refreshed old dormant support after target references are removed.
- Removed ownersByIdentity in favor of section attachments and the coordinate map. Replaced three per-owner atomic objects with inline volatile fields and shared VarHandles, preserving atomic enqueue/resync operations and bitmap reuse.
- Added nine real-world Forge GameTests using actual campfires, pending BE NBT, section mutation, formed T1/T2 generators/radiators, and ticking fountains. Replaced the obsolete capacity-flag JUnit with runtime queue recovery coverage, per the user's production-test-only instruction.
- Documentation impact: updated runtime architecture, heat production, and data lifecycle living docs. Preserved unrelated existing source/docs changes.

## Decisions

- Restore machines on their next valid production tick; bootstrap does not consume fuel or heat-network resources. No producer epochs, caches, or extra discovery ticks were added.
- Keep the existing unchanged-output dirty coalescing. The optional observe fast return and speculative bitmap/pool/scheduler redesigns were not needed for this fix.
- GameTestServer omits GameProfileCache, which real team/town initialization requires. The test fixture installs that standard server service; production team/thermal code was not modified for the fixture.
- The capacity test waits for initial world discovery before reducing its actual index limit. This isolates refusal/recovery from previously undiscovered sources in the shared test world.

## Validation

- Java 17: `gradlew.bat compileGameTestJava --offline --no-daemon --console=plain` passed.
- Final `gradlew.bat runGameTestServer --offline --no-daemon --console=plain`: BUILD SUCCESSFUL, normal server shutdown, all 25 required GameTests passed at 02:28:13, including nine new production scenarios and the existing pre-existing-campfire/neighbor warming regression.
- First run: 20/23 passed; two generator fixtures assumed an available GameProfileCache, and capacity recovery ran before unrelated initial discovery settled. A subsequent run exposed the same absent profile service through real town initialization; the fixture was corrected. The final run covers those paths successfully.
- No JUnit was added or run. No copied build checkout or path hash was introduced.
- Test-only startup sampling: cached-profile calls 3.35–6.97 ms; profile-rebuilding calls 179.53–232.69 ms; first observed call 78.81 ms. These include the whole gameplay startup call and are not isolated owner timings or a performance comparison.
- Targeted diff whitespace and documentation-link checks passed. Existing optional-mod/recipe startup warnings remain outside this change.

## Remaining

- Large-world CPU/retained-heap comparison and dedicated worker-failure/chunk-reload stress combinations have not been measured in this run. Source/owner memory figures in the plan remain estimates; the eight-chunk budget is not a measured optimum.
- Loaded but non-ticking machines retain the documented next-valid-tick recovery boundary.
