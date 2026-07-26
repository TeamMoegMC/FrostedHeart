# Town save failure and building-link recovery

- Time: `2026-07-27 00:55:18 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `TeamTownData terrain resources, team-data persistence, and town building block-entity links`

## Completed

- Traced the missing buildings and residents to a failed `town` serialization in the previous run log: a non-empty `ChunkResourceTracker.extractedResources` map used numeric NBT map keys and failed with `Not a string`.
- Changed chunk-position map keys to their packed-long string representation so non-empty terrain resource extraction data round-trips through NBT.
- Added server-side recovery for a town building block entity that still has a valid persisted town provider but whose building-map entry is missing.
- Prevented `CTeamDataManager` from overwriting an existing team file when any in-memory special-data component was omitted by a failed serialization.

## Decisions

- Keep the existing compact map representation and encode packed chunk positions as strings because NBT compound/map keys must be strings.
- Only reconstruct missing buildings on the logical server and only when the block entity already contains a valid town provider; generated/unowned town blocks remain unlinked.
- Treat partial team serialization as a failed save and preserve the previous disk file instead of silently replacing it with incomplete data.

## Validation

- `./gradlew compileJava` completed successfully.
- A targeted JShell check encoded and decoded a tracker containing chunk `(-1, -2)` with `88.125` extracted resources; decoding also accepted an empty legacy tracker.
- `./gradlew build` completed successfully with only the repository's pre-existing non-fatal license warnings.
- `git diff --check` passed.

## Remaining

- Building instances already lost from the affected test save will self-reconstruct when their chunks tick after this fix.
- Resident records already overwritten out of that save have no remaining source in the current team-data file and cannot be reconstructed automatically; restore an older world/team-data backup or recruit replacements.
