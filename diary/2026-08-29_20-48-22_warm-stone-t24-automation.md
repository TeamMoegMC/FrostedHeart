# Warm stone T24 automation

- Time: `2026-08-29 20:48:22 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `T24 targeted/full JUnit, Forge GameTest, build, companion static validation, and one portable test-fixture repair`

## Completed

- Re-ran the warm-stone kernel, models, NBT, Curios, synchronization observer, player integration, T23 command, radiation, Minecraft environment, and workload suites under JDK `17.0.2`.
- Re-ran all required Forge GameTests, the complete FrostedHeart JUnit suite, and the full Gradle build.
- Revalidated TheWinterRescue warm-stone recipes, Hot Water NBT, research, quest, bilingual tooltip/progress resources, forbidden charger path, and both Git diff modes.
- Replaced `TeamTownActualSaveCodecProbeTest`'s hardcoded private macOS save path with a repository-contained `NbtOps` persistence fixture so the complete suite is machine-independent.

## Decisions

- The town test repair changes only test setup; it preserves the persisted-payload-to-full-sync-packet contract and changes no production behavior.
- No climate living document changed because no warm-stone behavior, model, lifecycle, persistence, configuration, synchronization, or cross-system contract changed.
- The four known complete-catalogue `workbench` parent failures remain outside T24 and are not attributed to `warm_stone`.

## Validation

- Targeted JUnit: `21` suites, `101/101` tests, zero failures/errors/skips.
- Portable town fixture: focused `1/1` passed.
- Full JUnit after repair: `201` suites, `868/868` tests, zero failures/errors/skips.
- Forge GameTest: `13/13 required` passed (`12` thermal, `1` Frosted Research).
- `gradlew.bat build`: passed.
- TheWinterRescue: KubeJS syntax, `7` JSON parses, research/quest/language/NBT/forbidden-path assertions, and worktree/index `git diff --check` passed.
- Complete `validateResearchCatalog`: expected failure only for `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra` missing parent `workbench`; no `warm_stone` error.

## Remaining

- Stop before T25. T25 owns the real gameplay matrix and parameter measurements; T26-T28 own documentation consolidation, final two-repository verification, and plan closure.
- The aggregate-temperature Tooltip/client-config follow-up remains deferred until explicitly restored by the user.
