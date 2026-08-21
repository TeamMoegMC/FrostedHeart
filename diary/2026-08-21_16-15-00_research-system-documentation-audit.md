# Research system documentation audit

- Time: `2026-08-21 16:15:00 +0800`
- Author: `Codex; OpenAI; primary documentation agent with three GPT-5.6 Terra high subagent audits`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedresearch`, directly coupled Chorda/team and Frosted Heart consumers, `docs/research/`

## Completed

- Audited the definition/codec model, stable registry, clues, effects, team state, formulas, insight, persistence, packets, reload lifecycle, gameplay surfaces, UI, commands, APIs, and optional-mod integrations.
- Added a navigable research-system document set under [`docs/research/`](../docs/research/README.md), including a source-grounded risk and validation-gap register.
- Corrected the UI document's full-sync refresh contract, editor-hidden graph behavior, codec point default wording, and best-effort FTB sidebar restoration claim.
- Inspected the companion pack's research catalogue and language/resource ownership without changing that repository.

## Decisions

- Kept implementation truth separate from intended plans and open design questions.
- Documented config JSON, registry slots, team progress, and derived/client state as separate authority layers because their migration and synchronization rules differ.
- Treated research/clue/effect IDs and clue/effect list order as compatibility contracts even though only part of that identity is persisted by nonce.
- Recorded source-confirmed defects separately from runtime compatibility hypotheses so future developers can prioritize validation correctly.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` — `BUILD SUCCESSFUL` (`9` actionable tasks, `2` executed, `7` up-to-date).
- `git diff --check` — passed.
- Cross-checked all new documentation against source anchors and the current 81-file development/companion research catalogue; no companion-repository files were modified.

## Remaining

- Resolve or explicitly accept the issues in [`known-risks.md`](../docs/research/known-risks.md), especially single-player incremental synchronization, kill clues, drawing-desk packet authorization, reset semantics, stable-data migration, full-sync archive refresh, and optional-mod startup matrices.
- Perform the in-game and compatibility validation listed there; the current unit suite does not cover those paths.
