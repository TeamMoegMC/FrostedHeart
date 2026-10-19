# Knowledge login GameTest skips instead of failing without FTB Teams

- Time: `2026-10-18 22:42:01 -0500`
- Author: `Claude Opus 5; implementation agent`
- Status: `completed`
- Scope: `src/gametest/java/com/teammoeg/frostedresearch/knowledge/KnowledgeLoginGameTests.java`

## Completed

- Acted on the first Remaining item of
  [the baseline re-run](2026-10-18_22-31-13_knowledge-research-validation-baseline-rerun.md):
  `firstLoginWaitsForTeamCreation` now skips when FTB Teams is absent instead of hard-failing, so the documented
  `-PwithoutFtb` half of the baseline in [known-risks.md](../docs/research/known-risks.md) can go green.
- Replaced the opening `helper.assertTrue("ftbteams".equals(...))` with an early return. When the provider is not
  `ftbteams` the test logs the provider actually in use through `FRMain.LOGGER` and calls `helper.succeed()` before
  registering any listener or placing a player, so nothing is left to unregister.
- Left the FTB-present path unchanged. Every first-login ordering assertion still runs exactly as before whenever the
  real provider is present.

## Decisions

- Skip by succeeding early rather than marking the whole `@GameTest` as `required = false`. The latter would also stop
  the test from failing the build in the one configuration it exists to guard.
- Log the observed provider name instead of skipping silently, so a run that unexpectedly lacks FTB Teams is visible in
  `latest.log` rather than indistinguishable from a genuine pass.
- Kept the provider check itself (`TeamsAPI.getAPI().getProviderName()`) rather than switching to
  `ModList.get().isLoaded("ftbteams")`. What the test needs is the provider actually in use, which is what the original
  guard already expressed correctly; only its failure mode was wrong.
- Did not stamp `Last verified` in the five `docs/knowledge/` documents. Both GameTest baselines are green now, but
  `./gradlew test` still carries three deterministic town/thermal failures and in-game visual acceptance is still not
  done, so the knowledge documents would overstate coverage.

## Validation

- `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei` — **BUILD SUCCESSFUL in 1m 19s**, `All 105 required tests
  passed :)`. The skip fired as intended:
  `Skipping firstLoginWaitsForTeamCreation: team provider is 'chorda', not 'ftbteams'`. This is the exact command that
  reported `1 required tests failed :(` before the change.
- `./gradlew runGameTestServer` — `1 required tests failed :(` in 1m 30s, but the failure is
  `ThermalTransitionDataGameTests.randomEntryConvertsWithoutPageAndCarriesExactEnthalpy`
  (`result=DEFERRED, state=Block{minecraft:water}[level=0]`), the intermittent thermal test already recorded in the
  previous entry. No skip line appeared, confirming the guard did not trigger with FTB present, and the knowledge login
  test was not among the failures.
- `./gradlew runGameTestServer` again — **BUILD SUCCESSFUL in 1m 24s**, `All 105 required tests passed :)`. The thermal
  test cleared on the repeat, so that failure is the known intermittent one rather than anything this change caused.
  Both documented GameTest baselines are therefore green after this change.
- `git diff --check` — clean.

## Remaining

- The three deterministic unit failures in `frostedheart.content.*` (`TownHousingPlanTest`,
  `WarehouseBuildingCodecTest`, `DormantChunkThermalStateTest`) are untouched and still belong to those subsystems.
- `ThermalTransitionDataGameTests.randomEntryConvertsWithoutPageAndCarriesExactEnthalpy` flakiness is still
  uncharacterised. It has now gone red twice and green four times across the two sessions; the cause was not
  investigated here.
- `validateResearchCatalog` against a companion pack is still unrun on this master.
- In-game client visual acceptance of the knowledge journal, copy desk and graph, and the multiplayer matrix, are still
  not done.
