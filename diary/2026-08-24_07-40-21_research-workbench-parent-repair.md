# Research Workbench Parent Repair

- Time: `2026-08-24 07:40:21 +08:00`
- Author: `Codex; OpenAI GPT-5; primary coding agent`
- Status: `completed`
- Scope: `Frosted Research external catalogue graph in the companion pack and local development mirror`

## Completed

- Traced the rejected catalogue to commit `c4ba3f8f`, which deleted `config/fhresearches/workbench.json` without removing four surviving parent references.
- Changed `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra` to explicit root definitions with `"parents": []` in both `D:/TheWinterRescue/config/fhresearches` and `run/config/fhresearches`.
- Kept missing-parent validation strict; no runtime alias or silent edge removal was introduced.
- Reverified the graph contract in `docs/research/definitions-and-codecs.md`.

## Decisions

- The deleted `workbench` research remains deleted. The four affected projects are independent roots, matching the documented catalogue design.
- The companion pack remains the production authority, while the ignored `run/config/fhresearches` copy is updated as the local development runtime input.

## Validation

- Before the fix, `./gradlew validateResearchCatalog` reproduced all four `missing parent workbench` diagnostics.
- After the fix, `./gradlew validateResearchCatalog` validated all `81` development definitions.
- `./gradlew validateResearchCatalog -PresearchCatalogDir=D:/TheWinterRescue/config/fhresearches` validated all `81` companion definitions.
- Structured JSON graph checks reported zero missing parents in both catalogues.
- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` passed all `57` research tests.
- `./gradlew test` passed `151` suites and `581` tests with zero failures, errors, or skips.
- `git diff --check` passed in both repositories; unrelated existing climate and companion world-generation work was left untouched.

## Remaining

- None.
