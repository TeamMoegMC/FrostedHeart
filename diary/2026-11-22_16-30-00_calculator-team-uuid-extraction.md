# Calculator extraction by team UUID

- Author: Codex
- Date: 2026-11-22
- ComputeMachine query/extraction now accept the tool-bound Chorda team UUID. Calculator checks it against persisted owner without inspecting player types or claiming machines.
- Rubbing eligibility resolves research data by the bound team UUID; submission still checks the submitting player team. Legacy ownerless calculators require a real-player ordinary click before UUID extraction.
- Updated GameTests and living research docs. Validation completed below.

## Validation

- `git diff --check`: passed.
- `./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedresearch -PthermalTestProcessors=4`: BUILD SUCCESSFUL; all 2 required tests passed. Log: `build/team-uuid-gametest-validation.log`.
- `./gradlew --offline validateResearchCatalog` after GameTest exited: BUILD SUCCESSFUL; 86 definitions validated. Log: `build/team-uuid-catalog-validation-retry.log`.
- Initial catalogue validation overlapped another Gradle compilation and failed with NoClassDefFoundError for Chorda FileUtil. Concurrent builds sharing class output may have interfered; the serial retry passed. Initial log retained at `build/team-uuid-catalog-validation.log`.
- Tests cover null/wrong/correct team UUID, ownerless extraction without claim, bounded extraction, bound-team research eligibility and retained cross-team submission denial. Generated logs are not tracked.
