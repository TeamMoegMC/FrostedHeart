# Linear town productivity and hunting expectation

- Time: `2026-07-27 16:51:00 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town resident proficiency, mining/hunting productivity, hunting loot settlement and server balance config`

## Completed

- Replaced the hunting and mining geometric/exponential resident score with one shared weighted-arithmetic, linear productivity function in `TownMathFunctions`.
- Added planner-facing endpoints for attribute productivity, proficiency range/bonus, and final productivity range to both server config sections.
- Normalized resident work proficiency to `0..100`, made new professions start at `0`, and corrected NBT proficiency loading so saved values are actually restored.
- Removed hunting Loot Luck from output composition, converted the loot table to direct fixed weights, and added a codec-persisted fractional roll carry to make long-run expected rolls exact.
- Removed the default workerless hunting roll; passive expected rolls remain configurable and default to zero.

## Decisions

- A standard worker is four weighted attributes at `50` with profession proficiency `0`, giving `S_i = 1.0` under defaults.
- Default resident productivity is `0.5` at attribute `0`, `1.5` at attribute `100`, plus up to `1.0` at proficiency `100`, clamped to `0.5..2.5`.
- Hunting uses `7/6` expected rolls per standard worker-day. The fixed loot table averages `1.5` items per roll, giving `1.75` expected items per standard worker-day before terrain and storage limits.
- Only the fractional roll remainder is carried. Whole rolls blocked by depleted hunt terrain are lost rather than accumulated as an unbounded backlog.

## Validation

- `./gradlew compileJava --offline` completed successfully.
- `./gradlew build --offline` completed successfully; the existing non-fatal repository-wide license violation report remains.
- Boundary calculations produced `S_i = 0.5`, `1.0`, and `2.5` for default minimum, standard, and maximum residents.
- Six settlements at `7/6` rolls produced exactly seven whole rolls with only floating-point epsilon remaining.
- Parsed `hunting.json`: total fixed weight `19`, no quality fields, `1.5` expected items per roll, and `1.75` expected items per standard worker-day.

## Remaining

- The current codebase has no caller that increases work proficiency during successful work. This change deliberately does not invent a progression rate; define a planner-facing “days to mastery” or per-work gain before enabling automatic progression.
- `src/test` has no unit-test framework, so formula and carry checks are pure-function/manual validations rather than committed automated tests.
