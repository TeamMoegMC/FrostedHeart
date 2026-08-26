# Thermal Architecture Deletion-First Convergence Plan

- Time: `2026-08-26 20:22:41 +08:00`
- Authors: `Codex; OpenAI GPT-5; primary planning and architecture review agent`
- Status: `implemented; automated validation complete; same-workload JFR pending`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/climate/thermal/{solver,runtime,runtime/minecraft}; matching thermal JUnit/GameTest and climate documentation`
- Related: [`plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`](2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md), [`docs/climate/README.md`](../docs/climate/README.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), [`diary/2026-08-26_19-47-34_thermal-local-component-fragment-patching.md`](../diary/2026-08-26_19-47-34_thermal-local-component-fragment-patching.md)
- Implementation gate: `Approved by the user and implemented on 2026-08-26.`

## Purpose And Authority

This is a deletion-first convergence plan for the already implemented V1 thermal runtime. It does not replace the full thermal architecture specification and does not remove its physical, gameplay, lifecycle, source-energy, FarField, publication, or consumer contracts. The existing sparse-runtime plan remains the intended-behavior authority; source remains the implemented-behavior authority.

This plan owns only the next cleanup batch: remove duplicate execution paths and duplicate bookkeeping before considering another performance data structure.

## Goal

Make the production thermal runtime smaller and easier to reason about while preserving player-visible behavior, bounded work, authoritative enthalpy/capacity ownership, exact source-energy accounting, local `4^3` mutation rebuilds, and publication semantics.

The implementation succeeds only if:

1. Every implementation stage reduces production Java line count in its own touched scope.
2. The complete batch reduces production field count and does not increase production class count.
3. No new runtime manager, adapter, shadow path, cache table, benchmark source set, or future-only abstraction is introduced.
4. One final validation run passes after all code edits; implementation does not stop to run the full suite after every stage.
5. A final same-workload JFR decides whether any further optimization is justified.

## Verified Current State

The following facts were verified against the current working tree on `2026-08-26`. Existing uncommitted work was not changed during this review.

- Ordinary known-position block mutation is already local to a dirty `4^3` Brick. It does not compile or scan all `4096` Page blocks.
- Unknown-position section/container replacement and first Page admission still perform an authoritative `4096`-signature observation. That is a separate cold/recovery path.
- `ThermalCellArena` is the sole authority for live cell `H/C`, lifecycle generation, and slot ownership.
- Production topology installation uses `ThermalSweepFragments`, but `ThermalSweep` still contains a second flat-array builder, flat operation storage, and flat execution loop.
- `MinecraftThermalTopologyApplier` keeps three dirty Page identity maps plus `topologyDirty`, `fullTopologyCompilationRequired`, Page-local dirty masks, and Page-local rebuilt masks.
- Material order is represented three times: `PageState.materialFragmentOrder`, `ThermalSweepFragments.materialTraversal`, and `ThermalSweepFragments.materialRank`. Any traversal patch rebuilds ranks across the complete installed material-fragment domain.
- Material order currently follows mutation history through `moveFragmentsToEnd`. Two worlds with the same final topology can therefore aggregate equal material contacts in different floating-point orders.
- `MinecraftThermalTopologyApplier.installedActiveBySection` already provides installed Page lookup by section, but `resolveAirFacePort` still scans `pages.values()`.
- `MinecraftPhysicalSourceManager` maintains `sourcesByTargetSection`, while every topology-generation change also marks every source dirty. The index therefore coexists with a full-source fallback.
- `MinecraftThermalInput.drainDeferredBlockMutations` traverses every attached section owner whenever geometry is released, even when `deferredBlockMutationRevision` has not advanced.
- `ThermalCellArena.findFreeSpan` linearly scans from slot `0` to `highWaterMark`, but no current JFR proves that replacing it is worth another allocator data structure.
- The last recorded completed validation is thermal JUnit `214/214`, Forge GameTest `14/14`, and `git diff --check` clean. This planning turn did not rerun tests.

Current production-file size baseline for planning, not a performance claim:

| File | Lines |
|---|---:|
| `ThermalSweep.java` | 986 |
| `ThermalSweepFragments.java` | 1,314 |
| `MinecraftThermalTopologyApplier.java` | 4,121 |
| `MinecraftPhysicalSourceManager.java` | 626 |
| `MinecraftThermalInput.java` | 3,601 |
| Total | 10,648 |

Implementation must recapture the relevant file counts immediately before editing because other sessions may change the dirty working tree. No copied source snapshot or path-sensitive hash is required.

## What Already Exists

| Existing mechanism | Decision |
|---|---|
| `ThermalSweepFragments` primitive fragment execution and patching | Keep as the only production solve layout. |
| `ThermalCellArena` authoritative primitive state | Keep unchanged. Do not add a second state view. |
| Page-local `desiredSignatureIds` dirty-only clone/release | Keep unchanged. It already avoids stable duplicate `int[4096]`. |
| Brick-local geometry, pair, material, boundary, phase, and FarField fragment patching | Keep. Consolidate only the bookkeeping that selects this work. |
| `installedActiveBySection` | Reuse for source port resolution instead of adding another Page index. |
| Bounded physical source count and dirty source set | Reuse. Prefer bounded `O(source)` rebind over a second target-section index. |
| Deferred mutation revision and temporary per-owner bitset | Reuse. Add only an early return based on the existing revision. |
| Persistent derived Air adjacency/component index | Keep. Exact local edge-deletion split detection requires it; it contains no `H/C`. |
| Current executor boundary | Keep as the sole future async scheduling interface. Do not implement async execution now. |

## Target Architecture

```text
Minecraft mutation / lifecycle / environment input
                         |
                         v
              MinecraftThermalInput
        existing revisions + existing owner bitsets
                         |
                         v
          MinecraftThermalTopologyApplier
      fullCompilationRequired OR one dirty Page set
          PageState owns per-work-type Brick masks
                         |
            +------------+-------------+
            |                          |
            v                          v
  existing derived Air graph   fixed spatial material order
            |                          |
            +------------+-------------+
                         v
              ThermalSweepFragments
            only production solve layout
                         |
                         v
                ThermalCellArena
             sole authoritative H/C state
                         |
                         v
                QueryPublication
               read-only query projection

Physical sources
  bounded source set -> O(1) installed Page lookup -> fragment cell binding
                    -> ThermalSourceTimeline -> same arena
```

No second solver representation, mutation-history traversal, source-to-section cache, or shadow runtime remains in the target architecture.

## Deletion Rules

1. No new production class, manager, adapter, resolver callback, shadow implementation, profiler wrapper, JMH/JOL source set, or compatibility registry.
2. No new persistent map, set, queue, or array unless the same stage removes more persistent state and reduces total production lines.
3. Test migration may reuse existing builders. It must not create a new production test API or a parallel reference solver.
4. Do not combine structural cleanup with physics changes. Conductance, radiation, FarField formulas, source power, `integral P dt`, phase energy, solver cadence, and consumer composition stay unchanged.
5. The one intentional numerical normalization is material aggregation order: equal final topology must use equal fixed spatial order regardless of mutation history. This may change last-bit floating-point results but must remain within the existing numeric tolerance and preserve energy accounting.
6. If a stage cannot finish with net-negative production lines, stop and revise the plan. Do not hide the increase in a helper class.
7. Do not delete old architecture documentation merely because an implementation mechanism is removed. Update only statements that describe current runtime behavior.

## Ordered Implementation

All stages touch the same runtime and solver ownership boundary. Implement them sequentially in one working batch. Do not split them into parallel PRs or agents.

### Stage 0: Reconfirm The Baseline

- Re-read the current diff before editing so concurrent user/session changes are preserved.
- Record line, field, and class counts for the exact target files.
- Confirm all direct production construction sites of `ThermalSweep` and all callers of the APIs scheduled for deletion.
- Do not run the full test suite yet.

Exit condition: the deletion list below matches the current source; otherwise update this plan before code.

### Stage 1: Delete The Flat `ThermalSweep` Execution Path

Keep `ThermalSweep` as the thin runtime solve facade and home of the existing compact operation/result value types if moving those types would increase churn. Make `ThermalSweepFragments` its only operation storage and execution owner.

Delete:

- Flat pair, boundary, phase, generation, and state-slot arrays from `ThermalSweep`.
- List-based flat constructors.
- The old `ThermalSweep.Builder` and its capacity-growth helpers.
- Flat forward/reverse loops and the `fragments == null` execution branch.
- Tests whose only purpose is proving the deleted flat implementation agrees with the fragment implementation.

Change:

- Construct the disabled/empty production sweep through an empty `ThermalSweepFragments` layout.
- Migrate solver behavior tests to the existing fragment builder without adding a second test-only solver.
- Keep pair, boundary, phase, energy-residual, stale-generation, forward/reverse, and phase-runtime behavior coverage against the single implementation.

Acceptance:

- `ThermalSweep` has one execution path and no duplicate primitive columns.
- Production lines decrease materially; this stage is expected to provide the largest deletion.

### Stage 2: Delete Mutation-History Material Traversal

Define one canonical material order: installed Page spatial order, then Brick index `0..63`, then the existing operation order inside each Brick fragment.

Delete:

- `PageState.materialFragmentOrder`.
- `moveFragmentsToEnd`.
- `ThermalSweepFragments.materialTraversal` and `materialRank`.
- `rebuildMaterialRanks`, traversal replacement maps, and `replaceMaterialTraversal`.
- Full-domain rank rebuild work after a local material patch.

Change:

- Aggregate equal material cell-pair contributions by fixed fragment index and existing operation index.
- Preserve the current conductance formula, pair key, per-fragment operation order, forward/reverse solve order, and lifecycle generation checks.
- Add a regression case where two different break/place histories produce the same final topology and therefore the same canonical material result within existing numeric tolerance.

Acceptance:

- Material aggregation depends on final topology, not mutation history.
- A one-Brick material patch touches only its affected pair keys; it performs no dimension-wide rank fill or traversal rewrite.
- Production lines and fields decrease.

### Stage 3: Consolidate Topology Dirty Bookkeeping

Use one identity-based dirty Page membership collection. Keep separate Page-local Brick masks only where they describe genuinely different work: geometry rebuild, pair-owner rebuild, and material-neighbor rebuild.

Delete:

- `dirtyGeometryPages`, `dirtyPairPages`, and `dirtyMaterialPages` as three independent membership authorities.
- Repeated union/copy loops whose only purpose is combining those Page collections.
- `topologyDirty` if the assignment audit confirms it is fully derived from `fullTopologyCompilationRequired`, nonempty dirty Page membership, or pending retirement.

Keep:

- `fullTopologyCompilationRequired` for lifecycle/global inputs that truly change the fragment layout or global FarField classification.
- Page-local masks needed to distinguish work type.
- Rebuilt masks only as a short-lived journal between compilation and the same apply commit; clear them in that commit.

Change:

- Every mutation/lifecycle entry point inserts the Page once and ORs the relevant existing masks.
- Full compilation ignores incremental masks and clears the unified membership after successful commit.
- Incremental compilation iterates each dirty Page once and dispatches work from its masks.

Acceptance:

- One Page cannot be represented in three dirty collections.
- A local mutation still rebuilds its dirty Brick, three negative-axis pair owners when required, and six material-face neighbors without recursive expansion.
- No new dirty-state class or enum is added.

### Stage 4: Simplify Physical Source Rebinding

Use the existing source hard cap and existing installed Page index instead of maintaining both a target-section index and a global topology fallback.

Delete:

- `sourcesByTargetSection`.
- `indexTargets`, `unindexTargets`, and their registration/removal scans.
- Target-index-specific chunk/Page invalidation branches.
- The redundant topology-generation fallback after every binding-invalidating lifecycle path is proven to mark sources through the remaining explicit callbacks.

Change:

- `resolveAirFacePort` uses `installedActiveBySection.get(sectionKey)` and retains all dirty, revision, retirement, mixed-face, and lifecycle checks.
- A Page invalidation, withdrawal, or relevant chunk lifecycle event marks the bounded source set dirty directly.
- Rebinding remains `O(source * declared ports)` with `O(1)` Page lookup; the V1 source cap keeps this bounded and simpler than a second index.
- Preserve the existing zero-cut, settle-before-release, missing-port policy, radiation registration, lifecycle generation, and exact source timeline semantics.

Acceptance:

- Source resolution is no longer `O(source * Page)`.
- Source removal/profile change no longer scans every indexed section set.
- No source binds a retired/replaced arena span and no source energy is duplicated or dropped.

### Stage 5: Add The Existing-Revision Deferred Fast Return

At the start of `drainDeferredBlockMutations`, compare the existing `deferredBlockMutationRevision` with `drainedDeferredBlockMutationRevision`. Return immediately when equal.

Do not add a deferred-owner collection. When revisions differ, retain the current owner scan and temporary precise bitsets because an off-thread callback can arrive without a main-thread dirty-owner index.

Acceptance:

- Normal main-thread mutation sealing does not traverse all attached section owners when no deferred mutation exists.
- A mutation arriving during a drain remains pending because the captured revision is written only after processing that captured cut.
- Production code increases only by the guard while the complete batch remains strongly net-negative.

### Stage 6: Final Validation And Allocator Decision Gate

After all edits and test migrations are complete, run validation once. Then capture a real-save JFR using the same repeated door/mining workload used for the earlier profile.

Inspect:

- Thermal CPU share and top thermal stacks.
- Allocation rate and retained thermal objects.
- Counts/time for local topology patch, source rebind, solve, and publication where JFR exposes them without adding runtime counters.
- Whether `ThermalCellArena.findFreeSpan` appears as a material hotspot.

Decision:

- If `findFreeSpan` is not a measured hotspot, leave it unchanged.
- If it is a measured hotspot, write a separate plan for a primitive free-span allocator. Do not add `TreeMap`, dual indexes, or allocator state in this batch.

## Expected Data And Test Flow

```text
known-position mutation
  -> one Page membership + Brick masks
  -> local geometry/pair/material fragment patch
  -> single fragment solve path
  -> arena H/C
  -> publication

off-thread mutation
  -> existing owner bitset + revision
  -> revision changed? no: return
                     yes: scan owners, drain only nonempty bitsets
  -> same known-position mutation path

source lifecycle/topology event
  -> bounded source set dirty
  -> each declared port
  -> installedActiveBySection O(1)
  -> exact face component/lifecycle generation
  -> timeline seal and same arena solve
```

Planned coverage:

```text
Single solver path
  |-- pair forward/reverse and buoyancy
  |-- fixed boundary energy accounting
  |-- phase contact and lifecycle generation
  |-- empty/disabled sweep
  `-- fragment patch commit/repeated replacement

Local topology path
  |-- one mutation and neighbor ownership
  |-- break/place batch cancellation
  |-- material-only and geometry+material changes
  |-- canonical result across different mutation histories
  `-- Page admission/retirement full-layout fallback

Source path
  |-- exact face-port bind through installed section index
  |-- target Page mutation and withdrawal
  |-- profile/source removal without stale index state
  |-- zero-cut/rebind without duplicate integral P dt
  `-- blocked/degraded/missing-port policies

Deferred path
  |-- unchanged revision returns without owner work
  |-- one off-thread position drains once
  `-- mutation arriving across a drain remains scheduled
```

## Failure Modes

| Change | Realistic failure | Required coverage/handling | Player visibility |
|---|---|---|---|
| Flat solver deletion | Empty or disabled runtime has no valid sweep | Unit test empty solve and disabled topology path | Crash or stalled runtime if missed |
| Canonical material order | Equal contacts aggregate in a changed order and exceed tolerance | Different-history/same-topology regression plus energy residual assertions | Usually silent numeric drift |
| Dirty collection merge | A pair-owner or material-neighbor Page is not inserted | Local face-neighbor and material-neighbor GameTests | Stale temperature near edited wall |
| Source index deletion | Source remains bound to a released slot | Page withdrawal/rebuild lifecycle test and stale-generation rejection | Missing heat or wrong-cell heating |
| Source mark-all rebind | Re-registration duplicates source energy | Timeline watermark and `integral P dt` regression | Excess heat after edits |
| Deferred early return | Concurrent revision is acknowledged without drain | Cross-drain revision test; captured-cut comparison remains | Delayed/stale topology |

No failure above may remain both silent and uncovered before implementation is accepted.

## Final Validation

Run once after the complete edit batch:

```powershell
./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain
git diff --check
```

Then:

- Confirm all required thermal JUnit and Forge GameTests pass; record exact counts rather than assuming `214/214` and `14/14` remain unchanged.
- Search for every deleted field/method name and confirm there are no production callers.
- Record before/after production lines, fields, classes, and persistent collection fields for each stage in `Outcome`.
- Update `docs/climate/data-lifecycle-and-integration.md` only where current behavior changed.
- Update the original sparse-runtime plan outcome with a link to this plan; do not duplicate this full plan into it.
- Append one new diary entry with decisions, final commands, counts, JFR result, and remaining work.

## Stop Conditions

Stop implementation and return to planning if any stage:

- Requires a second runtime representation to preserve behavior.
- Adds a production class or increases production lines in that stage.
- Changes `ThermalCellArena` authority or copies authoritative `H/C` state.
- Changes source power, energy integration, phase, radiation, FarField, or gameplay compositor semantics.
- Cannot preserve source lifecycle safety without the removed target index.
- Needs allocator, FarField, or scheduling work to make its own tests pass.

## NOT In Scope

- `ThermalCellArena` allocator redesign: known linear scan, but a new structure requires post-cleanup JFR evidence.
- `ThermalCellArena` state layout or Page span ownership rewrite: current primitive arena remains authoritative.
- FarField formulas, connected-component physics, continuation distance, or the `200`-tick natural-environment refresh: these are separate semantic/performance investigations.
- Radiation algorithm, ray budget, source parameters, or receiver cache.
- Source timeline, watermarks, common-time ordering, or exact `integral P dt` semantics.
- Page admission and unknown-position full-resync `4096` observation: these are authoritative cold/recovery paths, not ordinary mutation work.
- Gameplay temperature balance, campfire power, phase recipe values, material calibration, HUD, crop, town, or machine behavior.
- Real asynchronous execution: retain only the current executor interface.
- JMH, JOL, synthetic benchmark source sets, permanent runtime profiling tables, or shadow comparison paths.
- Old climate design/history deletion: this cleanup removes duplicate implementation mechanisms, not useful architectural records.

## Parallelization

Sequential implementation, no parallelization opportunity. The stages share solver/topology/source lifecycle contracts, and splitting them would recreate temporary adapters or duplicate authorities.

## Expected Outcome

- One production solver representation instead of flat plus fragment implementations.
- One deterministic material ordering derived from final topology instead of mutation history plus traversal/rank mirrors.
- One dirty Page membership authority instead of three collections plus a redundant boolean.
- One installed Page lookup for source ports and a bounded source rebind instead of target index plus global fallback.
- No normal deferred-owner scan when no off-thread mutation exists.
- Fewer production lines, fields, collections, branches, and state transitions without changing player-visible thermal behavior.
- A clean measurement point from which any remaining allocator or periodic global work can be judged by JFR instead of assumption.

## Outcome

Implemented on `2026-08-26` without adding a production class, manager, adapter, cache table, benchmark source set, or shadow path.

| Core production file | Before | After | Delta |
|---|---:|---:|---:|
| `ThermalSweep.java` | 986 | 216 | -770 |
| `ThermalSweepFragments.java` | 1,314 | 1,250 | -64 |
| `MinecraftThermalTopologyApplier.java` | 4,121 | 4,018 | -103 |
| `MinecraftPhysicalSourceManager.java` | 626 | 585 | -41 |
| `MinecraftThermalInput.java` | 3,601 | 3,605 | +4 |
| Total | 10,648 | 9,674 | **-974** |

The batch removed the flat solver's 24 persistent operation/scratch fields, the two material traversal/rank mirrors, Page-local material order, three duplicate dirty Page maps plus the redundant dirty boolean, and the source-to-section reverse index. One unified dirty Page map replaced the three maps, for a net reduction of at least 31 persistent fields. Production class count did not increase.

Implemented behavior:

- `ThermalSweepFragments` is the only operation storage and execution implementation; `ThermalSweep` is a thin facade.
- Material aggregation uses fixed fragment/operation order derived from final topology, with no mutation-history traversal or full-domain rank rebuild.
- `MinecraftThermalTopologyApplier` has one dirty Page membership authority while retaining work-specific Page-local Brick masks.
- Physical source ports resolve through `installedActiveBySection`; the bounded source set replaces the reverse target index. The topology-generation fallback remains intentionally because first cold Page admission has no invalidation callback.
- `drainDeferredBlockMutations` returns immediately when its existing revision has not advanced and adds no owner index.

Validation completed with:

```powershell
./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain
git diff --check
```

Results: thermal JUnit `212/212` passed with zero failures, errors, or skips; Forge GameTest `14/14` required passed; `git diff --check` reported no whitespace errors. The JUnit count decreased from `214` because two tests existed only to compare the deleted flat and fragment solvers.

Same-workload real-save JFR remains a measurement follow-up. No allocator or profiling structure was added without that evidence; `ThermalCellArena.findFreeSpan`, physics, FarField, radiation, source `integral(P dt)`, arena `H/C` authority, cadence, and gameplay values remain unchanged.
