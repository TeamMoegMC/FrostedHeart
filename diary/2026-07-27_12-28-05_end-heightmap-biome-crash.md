# Prevent End heightmap biome crash

- Time: `2026-07-27 12:28:05 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/mixin/minecraft/temperature/ServerLevelMixin_TemperatureUpdate.java`

## Completed

- Replaced the hard-coded overworld minimum height used by the temperature freezing pass with the current dimension's `getMinBuildHeight()`.
- Empty heightmap columns in dimensions such as the End now remain at `Y=0` instead of producing the invalid section index `-1` in `CUtils.fastGetBiome`.
- Reviewed the other loaded-chunk biome lookup in the mixin; it receives positions generated from valid chunk sections and needs no change.

## Decisions

- Fixed the invalid coordinate at its source rather than clamping inside `CUtils.fastGetBiome`, preserving that utility's fast-path behavior and avoiding silently masking unrelated invalid callers.

## Validation

- `./gradlew compileJava` — successful.
- `git diff --check` — clean.
- Confirmed the crashing hard-coded `blockpos1.getY() > -64` condition is no longer present.

## Remaining

- A dedicated-server runtime reproduction in an empty End column was not performed; the repository has no existing temperature-system test harness.
