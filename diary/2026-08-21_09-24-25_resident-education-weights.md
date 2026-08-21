# Resident education generation weights

- Time: `2026-08-21 09:24:25 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `ordinary recruit education distribution defaults, tests and town living documentation`

## Completed

- Changed the default education weights for levels `0..5` to `0.15/0.50/0.20/0.10/0.04/0.01`.
- Updated the fixed-seed frequency assertion and the current town-model documentation.

## Decisions

- This distribution supersedes the `0.20/0.60/0.15/0.04/0.01/0.00` default recorded in `2026-08-21_09-16-00_resident-initial-heterogeneity.md`.
- Runtime values remain relative weights and are normalized by `ResidentGenerationModel.pickEducationLevel`.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.resident.ResidentGenerationModelTest --console=plain`: passed.
- `git diff --check`: passed.

## Remaining

- None.
