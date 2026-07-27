# Town work eligibility and random initial proficiency

- Time: `2026-07-27 17:59:35 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `TeamTownData work assignment, resident profession proficiency, and town production documentation`

## Completed

- Made `TeamTownData.assignWork` skip candidates rejected by each work building's `canResidentWork` attribute and housing checks.
- Avoided re-queuing a work building when it has no eligible candidate, preventing the assignment loop from stalling.
- Restored the legacy squared-random `0..50` initial proficiency when a resident first encounters an unrecorded profession, while retaining the current `0..100` stored-value normalization.
- Updated the production-model document to describe random initial proficiency.

## Decisions

- Kept `ITownResidentWorkBuilding.canResidentBeAssigned` unchanged because its current behavior is intentional.
- Did not add automatic proficiency growth.

## Validation

- `./gradlew compileJava --offline` completed successfully with only the repository's existing 20 deprecation warnings.
- `git diff --check` passed.
- Confirmed `ITownResidentWorkBuilding.java` has no diff.

## Remaining

- This change filters new work assignments; it does not evict a previously assigned resident whose attributes later fall below the work threshold.
