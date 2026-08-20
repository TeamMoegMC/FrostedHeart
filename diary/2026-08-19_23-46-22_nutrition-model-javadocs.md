# Nutrition model Javadocs

- Time: `2026-08-19 23:46:22 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `food nutrition types, resolver/event/capability, Caupona adapter, resident public menu and attribute support model`

## Completed

- Documented the distinct raw recipe, food percentage, player state and resident support units at their owning classes and conversion methods.
- Added formula, parameter, clamping and lifecycle contracts to the key player eating, hunger loss, persistence and configuration migration paths.
- Documented the internal dynamic-food event contract and Caupona's percentage-weighted aggregation boundary.
- Added planning and scoring contracts to the resident public menu model and settlement contracts to the resident satisfaction, support, potential, growth, decay and effective-intelligence model.

## Decisions

- Javadocs focus on public numeric boundaries and non-obvious pure functions; obvious channel getters and mechanical setters remain uncluttered.
- No living documentation update was needed because this work only documents behavior already implemented and verified in `docs/nutrition/` and `docs/town/`.

## Validation

- `./gradlew test`: build successful.
- `git diff --check`: passed.
- Targeted UTF-8 Javadoc generation with strict doclint passed for all ten documented nutrition classes.
- The project-wide `./gradlew javadoc` task remains blocked by pre-existing US-ASCII configuration and legacy Javadoc errors in unrelated systems.

## Remaining

- Repairing the repository-wide Javadoc task is outside this nutrition documentation change.
