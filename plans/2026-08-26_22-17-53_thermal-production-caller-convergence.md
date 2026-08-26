# Thermal Production-Caller Convergence

- Time: `2026-08-26 22:17:53 +08:00`
- Authors: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/climate/thermal; matching thermal tests, climate documentation, and diary`
- Related: [`2026-08-26_20-22-41_thermal-architecture-deletion-first-convergence.md`](2026-08-26_20-22-41_thermal-architecture-deletion-first-convergence.md), [`2026-08-26_21-15-44_thermal-runtime-dead-path-and-dispatch-convergence.md`](2026-08-26_21-15-44_thermal-runtime-dead-path-and-dispatch-convergence.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md)

## Goal

Delete production code whose only caller is a test, whose modeled dimension cannot occur in the V1 Minecraft runtime, or whose state is maintained solely for diagnostics. Preserve the current physical and player-visible semantics while reducing runtime branches, retained arrays, mutation-path allocations, and public API surface.

This batch must reduce production classes, fields, and lines. It must not introduce a replacement manager, adapter, compatibility shell, cache, diagnostic layer, or future-only abstraction.

## Verified Current State

- Gameplay Page admission captures 4096 signatures and installs 64 world-aligned `4^3` fragments through `MinecraftThermalTopologyApplier.registerCapturedPage`.
- `MinecraftThermalInput.admitAllAirPage`, `registerAllAirPage`, `ThermalPage.allAir`, and pure-LOD split/merge are sustained only by tests and the pre-enable loop reachable from that test entrance.
- Every gameplay regular cell is created with width `4`; mixed components, material poles, and phase reservoirs are also stored at arena level zero.
- Production reads only geometry summary indices `0..63`. Octant summaries `64..71` and section summary `72` are test-only, but are rebuilt and allocated on real mutations.
- Cumulative migration, FarField affected-cell, phase retry/energy, sweep count, and radiation sample counters have no gameplay consumer.
- `CompiledBrick.atomBlockIndex` and `atomLocalRegionId` are retained without production consumers. `EmissionPort.channel` duplicates the channel authority in `MinecraftPhysicalSourceProfile.Port` and is not packed into the source registry.
- Gameplay FarField lookup varies only by `EnvironmentClass`; all other key axes are fixed constants. Wind changes conductance through a separate scalar.
- The single fragment solver, arena `H/C` ownership, source timeline, publication, local dirty masks, phase mutation path, Forge events, section Mixin attachment, and executor boundary are live production mechanisms.

## Deletion Rules

1. A test caller does not justify a production API. Migrate the test to the real production entrance or delete a test for behavior that no longer exists.
2. Remove a superseded feature as one connected branch. Do not leave width flags, constructors, transactions, or validation for a deleted feature.
3. Keep formulas required by current migration and solving, but remove cumulative ledgers and counters that do not affect decisions.
4. Preserve exact source integration, phase reservoir state, material traversal order, FarField conductance/domain behavior, and publication generation checks.
5. Keep `ArenaSpan` and `ThermalCellArena.PageAllocation.cellSpan`; fragment allocation still requires them. Remove only Page-level coarse span ownership.
6. Keep the user-selected `IMPULSE` source contract as the minimal timeline -> registry -> accumulator path. Do not restore source history, replay, counters, or cold-route with it.
7. Run the full validation once after the complete edit batch.

## Implementation

### Stage 1: Remove Coarse Page And Pure LOD

- Delete `admitAllAirPage`, `registerAllAirPage`, `ThermalPage.allAir`, `FullGeometryState.uniformAllAir`, Page-level `cellSpan`, `coverageWidths`, and `coverageRepartitionRequired`.
- Delete `MutationObservation.invalidatedCoarseSupport/materializedBrick` and branches derived only from them.
- Delete `prepareSplitPureLod`, `prepareMergePureLod`, `PageLayoutReplacement`, their compatibility helpers, and their dedicated tests.
- Make a Page describe exactly 64 `4^3` fragment coverage entries.
- After all production allocation sites are reconfirmed as width `4`, remove arena `levels[]`, `levelForWidth`, width `8/16` handling, and regular-cell width parameters that exist only for pure LOD.

### Stage 2: Collapse Geometry Summaries

- Store exactly 64 base summaries.
- Delete octant/section constants, accessors, merge logic, `installAllAirProof`, `rebuildAllParents`, `GeometrySummary.singleMedium`, and coarse-merge predicates.
- `setBaseSummary` must update one primitive entry without allocating temporary arrays.
- Update `FullGeometryState` and tests to accept exactly 64 summaries.

### Stage 3: Remove Runtime Test State And Redundant Columns

- Keep static geometry migration formulas and per-commit `MigrationResult`; remove the cumulative ledger owner, snapshot, instance-only migration methods, and cumulative checked additions.
- Remove FarField full-rebuild/affected-cell counters and their counting branches.
- Remove test-only runtime diagnostics and operation-count facades where tests can assert the installed fragment layout directly.
- Remove phase retry counters and cumulative committed-energy state; test each ACK's arena effect instead of retaining a global test ledger.
- Remove radiation result statistics that do not affect flux, confidence, flags, or budgets. Keep candidate visit counting where it enforces the configured bound.
- Remove `CompiledBrick.atomBlockIndex`, `atomLocalRegionId`, object-returning `FacePort`, and unused scalar accessors. Keep the forward atom-to-component mapping used by topology compilation.
- Remove zero-caller getters and PageBuild residue confirmed by the final call search.

### Stage 4: Collapse Source And FarField Metadata

- Remove `EmissionPort.channel`; keep channel classification in `MinecraftPhysicalSourceProfile.Port` for convection/contact/radiation routing.
- Restrict `ThermalSourceMode` to `POWER_SOURCE` and the explicitly retained `IMPULSE` contract. Delete cold-route because it has no gameplay producer.
- Replace the seven-axis FarField key with direct `EnvironmentClass` lookup containing conductance and `ApplicabilityDomain`.
- Delete unused key enums, candidate approval state, stored calibration error envelope, and test-only `TopologyGuard` material/unresolved/boundary-operation branches.
- Preserve the actual V1 decision: sky-proven natural openings use an in-domain ambient impedance; other open components remain continuation/unresolved according to the current loaded-only logic.

## Validation

Run once after all code and test migrations:

```powershell
./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain
git diff --check
```

Then search every deleted symbol, compare production line/field counts, and verify that no test-only constructor or getter has been reintroduced.

## Documentation Impact

Update climate lifecycle documentation to state that admitted Pages are fixed collections of 64 `4^3` fragments, geometry summaries are Brick-local, and FarField profiles are selected only by Minecraft environment class. Append a diary entry with exact validation counts and remaining deliberate dormant contracts.

## Outcome

Completed on `2026-08-27`.

- Deleted the copied Phase A GameTest world-capture implementation and the public test-only `MinecraftThermalInput.sealTick`, `geometryDeltas`, and `resolvedInputs` entrances.
- Migrated Minecraft integration tests to the production lifecycle: real loaded-section capture and Page admission, `MinecraftThermalInput.sealActiveLevel`, topology apply, arena solve, and `QueryPublication`/gameplay query reads. Tests no longer preallocate fake air cells or call a parallel sealing path.
- Kept static shape fixture coverage in `StateStaticThermalResolverTest`, where it exercises the same production resolver used by Minecraft capture.
- Corrected test assumptions around continuation watermarks, resolver capacity, unconnected `ServerPlayer`, publication invalidation, per-source ledger timing, section ownership, and the five-tick mutation deadline.
- Validation completed with `175/175` thermal JUnit tests and `11/11` required Forge GameTests, including `10` thermal integration GameTests and `1` Frosted Research GameTest.
- Living behavior is recorded in [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md).
