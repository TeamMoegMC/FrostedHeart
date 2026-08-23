# Research cache dependency audit

- Time: `2026-08-24 04:52:37 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Research archive cache invalidation, layout ownership, nullable selection, documentation, and regression tests`

## Completed

- Audited every archive cache key, dirty flag, revision counter, and early return against the state used to build its output.
- Made an open `ResearchArchiveLayer` detect `FHResearch.editor` transitions and rebuild the complete filtered archive view once.
- Removed the drawing-desk screen's duplicate width/height cache so `ResearchArchiveLayer.ArchiveLayoutKey` alone owns layout reuse and includes project-dialog visibility.
- Added presentation revision to the left-list cache identity and made nullable selection safe for immutable definition maps.
- Strengthened the editor transition regression to verify exactly one definition revision is produced.
- Updated `docs/research/research-ui.md` with the editor refresh, layout ownership, and empty-selection contracts.

## Decisions

- Kept cache inputs explicit through named keys or revision counters. Definition changes remain explicit invalidations; localized presentation and synchronized state retain separate revisions.
- Preserved legacy project ordering, graph coordinates, zoom limits, clue/completion behavior, and packet actions.

## Validation

- `./gradlew.bat compileJava --no-daemon --console=plain`: passed.
- Archive GUI tests: `9` suites, `27` tests, zero failures, errors, or skips.
- `./gradlew.bat test --tests "com.teammoeg.frostedresearch.*" --no-daemon --console=plain`: `20` suites, `57` tests, zero failures, errors, or skips.
- `./gradlew.bat test --no-daemon --console=plain`: `138` suites, `509` tests, zero failures, errors, or skips.
- `git diff --check`: passed; Git reported only existing LF-to-CRLF conversion notices.

## Remaining

- In-game visual verification is still required for live integrated-server `/research edit` transitions and GUI-scale changes.
