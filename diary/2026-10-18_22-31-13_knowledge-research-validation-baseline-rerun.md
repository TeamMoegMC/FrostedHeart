# Knowledge/research validation baseline re-run on master

- Time: `2026-10-18 22:31:13 -0500`
- Author: `Claude Opus 5; AI verification agent`
- Status: `completed (baseline not clean)`
- Scope: re-ran the maintained automated baseline from
  [docs/research/known-risks.md](../docs/research/known-risks.md) against `master` at `9a5d5f98c`, to check whether the
  `3ad011603` knowledge-system merge (2026-10-16) and the later master commits introduced regressions. Verification
  only; no source, resource or documentation behaviour was changed.

## Completed

- Ran the four assigned baseline commands on a clean `master` worktree and recorded every result:
  - `./gradlew test` — **failed**: 691 tests, 3 failed, 0 errors, 0 skipped.
  - `./gradlew runGameTestServer` — **passed**: `All 105 required tests passed :)`.
  - `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei` — **failed**: 1 of 105 required tests failed.
  - `git diff --check` — **passed** (exit 0); `git status` stayed empty throughout.
- Confirmed the knowledge/research code itself is green: all 108 `frostedresearch` unit tests across 33 classes passed
  (`KnowledgeServiceTest`, `KnowledgeDefinitionsTest`, `LinkMatcherTest`, `ResearchWorkspaceStateTest`,
  `TeamKnowledgeDataTest`, the `knowledge.client.*` layout/presentation tests and the rest), and the single knowledge
  GameTest passes whenever FTB Teams is present.
- Diagnosed the one knowledge-side failure. `KnowledgeLoginGameTests.firstLoginWaitsForTeamCreation` opens with
  `helper.assertTrue("ftbteams".equals(TeamsAPI.getAPI().getProviderName()), ...)`. `-PwithoutFtb` drops
  `ftb-teams-forge` from the runtime (build.gradle:373), so `TeamsAPI` falls back to `SinglePlayerTeamAPIProvider`,
  whose `getProviderName()` returns `"chorda"`. The assertion therefore cannot hold in that configuration: the test
  hard-asserts instead of aborting, so it is counted as a required failure rather than skipped.
- Established that this is not a merge regression. The test and its guard were both added on 2026-09-08 in `807f43691`,
  and [the entry that added it](2026-09-08_10-32-21_knowledge-first-login-team-lifecycle-fix.md) only ever validated it
  via `-PgameTestNamespaces=frostedresearch_knowledge_login runGameTestServer`, i.e. with FTB present. The
  `-PwithoutFtb` half of the documented baseline has never been able to pass since the test landed.
- Attributed the three unit-test failures to other subsystems and showed they predate the merge. All three live under
  `frostedheart.content.*`, and since `3ad011603` no commit has touched thermal or town source except the GPL header
  commits `d363a509b` / `063fa3519`; `9a5d5f98c` adds only a discussion document.
- Ran two extra diagnostic executions beyond the assigned four to separate deterministic failures from flaky ones:
  a second `-PwithoutFtb -PwithoutJei` run, and a `-PwithoutJei`-only run.

## Decisions

- Did not update `Last verified` in the five `docs/knowledge/` documents. The baseline did not come back clean, and the
  one failure that is knowledge-owned sits in the knowledge GameTest, so stamping today's date would overstate what was
  verified.
- Did not modify any source, test or resource to make the baseline pass; the task was diagnosis only. The
  `firstLoginWaitsForTeamCreation` guard is left exactly as written, and the fix direction is recorded under Remaining
  rather than applied.
- Treated the `-PwithoutJei`-only failures as flakiness, not a JEI dependency, because both tests passed in the other
  three GameTest executions of this session, including the run that also had JEI disabled.
- Ran the extra `-PwithoutJei` and repeat `-PwithoutFtb -PwithoutJei` executions because a single red run cannot
  distinguish a deterministic guard from an intermittent one; the repeat reproduced the knowledge failure byte for byte
  and cleared the two thermal tests.
- Left `./gradlew validateResearchCatalog -PresearchCatalogDir=<companion>/config/fhresearches` unrun. It is part of the
  documented baseline but was not in this task's four commands and needs an external companion pack path.

## Validation

- `./gradlew test` — BUILD FAILED, 3 of 691:
  - `DormantChunkThermalStateTest.savingDoesNotAdvanceOrQuantizeDormantCoolingAgain` —
    `AssertionFailedError: expected: <4> but was: <5>` at `DormantChunkThermalStateTest.java:44` (thermal).
  - `TownHousingPlanTest.normalizationAddsBetterHousingFirstAndEditsAreBounded` —
    `IllegalStateException: Cannot get config value before config is loaded`, thrown from
    `ForgeConfigSpec$ConfigValue.get` via `HouseBuilding.calculateTemperatureRating(HouseBuilding.java:617)` (town).
  - `WarehouseBuildingCodecTest.codecPersistsOnlyWarehouseStructureAndCapacityState` —
    `AssertionFailedError: expected: <true> but was: <false>` at `WarehouseBuildingCodecTest.java:52` (town).
  - Re-running only these three classes with `--rerun-tasks` reproduced all three, so they are deterministic.
- `./gradlew runGameTestServer` — BUILD SUCCESSFUL in 1m 21s, `All 105 required tests passed :)`, including the
  knowledge login test. `cleanResearchGameTestWorld`, `prepareResearchGameTestStructures` and
  `prepareResearchGameTestCatalog` ran as normal task dependencies; nothing was bypassed.
- `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei` — BUILD FAILED in 1m 14s, `1 required tests failed :(`:
  `firstloginwaitsforteamcreation failed! this login test must run with the real FTB Teams provider`. The remaining 104
  passed. A second identical run reproduced exactly the same single failure.
- `./gradlew runGameTestServer -PwithoutJei` (extra diagnostic) — BUILD FAILED, `2 required tests failed :(`:
  `ThermalTransitionDataGameTests.randomEntryConvertsWithoutPageAndCarriesExactEnthalpy`
  (`result=DEFERRED, state=Block{minecraft:water}[level=0]`) and
  `ThermalDormantCoolingGameTests.realChunkTicksFreezeOnlyTheScheduledSurfaceSample`. Both passed in all three other
  GameTest runs, and the knowledge login test passed here because FTB was present.
- `git diff --check` — clean, exit 0. `git status --short` was empty before and after every run; `run-gametest/` is
  gitignored (.gitignore:133) and no build output or log was added to the tree.
- The `Invalid research catalogue: invalid-gametest: points must be greater than zero` error in the GameTest logs is
  deliberate preflight fixture data and appears identically in the fully passing run.
- No player save under `run/` was read or written; the GameTest harness used `run-gametest/` only, and `run/saves/`
  entries still carry their 2024 timestamps.

## Remaining

- `firstLoginWaitsForTeamCreation` should abort rather than fail when FTB Teams is absent, so that the documented
  `-PwithoutFtb` baseline can go green; alternatively the baseline should state that this namespace requires FTB. Not
  changed here because this task was verification only.
- The three deterministic unit failures in the thermal and town subsystems are unowned by this pass and still need a
  fix from those subsystems.
- The two thermal GameTests above are intermittent and need their flakiness characterised; one red run out of four is
  not enough to localise the cause.
- `validateResearchCatalog` against a companion pack is still unrun on this master.
- In-game client visual acceptance of the knowledge journal, copy desk and graph surfaces is still not done; the
  automated layers cover logic and layout only.
- The multiplayer matrix (team migration, permission bridging and sync across real clients) is still not exercised.
