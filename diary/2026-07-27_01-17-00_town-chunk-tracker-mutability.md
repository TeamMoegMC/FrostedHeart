# Town morning-tick chunk tracker mutability crash

- Time: `2026-07-27 01:17:00 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `Town terrain-resource loading and mine morning work`

## Completed

- Traced the morning-tick crash to `ChunkResourceTracker.cost`: the codec supplied an immutable decoded map, then mine work attempted to mutate it with `Map.merge`.
- Changed `ChunkResourceTracker` to take an owned mutable `HashMap` copy after decoding.
- Made the transient active-chunk set an owned mutable `HashSet` as the same class also exposes a clearing operation.
- Inspected the post-crash team save and confirmed all three newly recruited residents were persisted; all three had houses, two already had work positions, and one remained unassigned.

## Decisions

- Restore mutability at the tracker boundary rather than special-casing `cost`, so every future mutation operates on a collection owned by the tracker.
- Keep the prior string-key codec fix unchanged; this crash occurred only after that data became serializable and reloadable.

## Validation

- `./gradlew compileJava` completed successfully.
- A targeted NBT codec regression encoded an empty tracker, decoded it, assigned immutable active chunks, charged `88.125` resources to chunk `(-1, -2)`, cleared the active chunks, and re-encoded successfully as `{extractedResources:{-2:88.125d}}`.
- `./gradlew build --offline` completed successfully with only the repository's pre-existing non-fatal license warnings.
- `git diff --check` passed.

## Remaining

- Re-enter the test save and trigger the next morning tick once to confirm the full in-game mine production path. The crash shutdown itself saved the team data successfully, so the three recruited residents should still be present.
