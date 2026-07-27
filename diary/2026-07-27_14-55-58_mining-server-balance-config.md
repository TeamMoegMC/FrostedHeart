# Mining server balance configuration

- Time: `2026-07-27 14:55:58 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `FHConfig server town settings and town mining-base balance calculations`

## Completed

- Added a dedicated `ITown.Mining` server-config section for standard-worker daily output, worker-slot floor area, minimum slots, camp connection radius, proficiency curve, attribute weights, and resident-assignment priorities.
- Reframed mining productivity around a fixed standard worker (all four attributes at 50 and zero proficiency) so `baseOutputPerStandardWorkerDay` is measured directly in town item units per worker per Minecraft day.
- Set the default standard-worker output to `3.5`, preserving the previous `6 * residentScore` output exactly with default settings.
- Added accurate Java names and comments for ore reserve per chunk and configured recovery per chunk-day while retaining the legacy fields and TOML keys as compatibility aliases.

## Decisions

- Treat the standard-worker definition as the unit reference, not another tuning control; making both the reference and its output configurable would create redundant knobs.
- Keep biome output composition data-driven through recipes rather than putting item IDs and weights into Forge config.
- Do not add mining-camp throughput or ore regeneration behavior in this change because neither is an existing constant; the current per-chunk recovery limitation is documented rather than silently changing balance.

## Validation

- `./gradlew compileJava --offline` completed successfully; only the repository's existing deprecation warnings were reported.
- `./gradlew build --offline` completed successfully; only the repository's existing non-fatal license violations were reported.
- Formula-equivalence checks at proficiency 0, 10, 25, and 50 reproduced the previous outputs: `3.5`, `5.230820`, `6.818633`, and `8.039490` item units per day for a worker with all attributes at 50.
- `git diff --check` passed.

## Remaining

- Chunk-tracked ore currently does not use the configured recovery value.
- Mining-camp throughput remains implicit, and biome recipe weight totals still affect allocation between camps in addition to output composition.
