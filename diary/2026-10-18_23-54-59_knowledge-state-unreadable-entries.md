# Knowledge state keeps unreadable entries instead of dropping the team file (review P0-1)

- Time: `2026-10-18 23:54:59 -0500`
- Author: `Claude Opus 5.5 (Anthropic, claude-opus-5-5); implementation agent, side session`
- Status: `completed`
- Scope: `frostedresearch/knowledge/state/KnowledgeState`, `knowledge/model/{Observation,ObservationValue,KnowledgeElement,KnowledgeKey}`,
  `knowledge/observation/ObservationSampling`, `data/TeamKnowledgeData`, `docs/knowledge/state-and-api.md`

## Completed

- Acted on P0-1 of the [knowledge review](../discussion/2026-10-18_23-01-04_knowledge-system-development-review.md) (the
  file is untracked in the main checkout at the time of writing). Re-verified its chain first: `KnowledgeState.read` decoded every
  list with `listOf()` + `getOrThrow`, and `new ResourceLocation` / `UUID.fromString` on hint and dream-topic keys threw too.
  `DataHolderMap.load` wraps the exception as `DataLoadException`. `CTeamDataManager.load` then skips the team, and the next
  `save()` deletes every `.nbt` file without a loaded team, which takes the town, legacy research, generator and labour data
  stored in the same file with it.
- `KnowledgeState.read` no longer throws. It decodes `archive`, `inbox`, `links`, `research` and `acquisitions` one entry at a
  time, and `hints` / `dream_topics` one key at a time. Every failure is copied raw into a private `unreadable` compound in the
  saved layout. Such entries never reach the live maps, and `write()` puts them back into their own sections unchanged.
  `replaceState` copies them, which matters because `TeamKnowledgeData.CODEC` decodes through it. `merge` carries them over
  once each. Each read logs a single `FRMain.LOGGER` warning with the count and the first location and cause.
- `schema_version` is now read. `KnowledgeState.SCHEMA_VERSION` (2) is the single constant, and
  `TeamKnowledgeData.CURRENT_SCHEMA_VERSION` now references it. `KnowledgeState.upgrade(tag, version)` is the one migration
  entry point and has no steps yet. A missing or newer version decodes leniently.
- `Observation`'s compact constructor now carries the validity rule and throws `IllegalArgumentException`. Decoding builds
  through a private `Stored` record and turns that exception into a codec error, so callers that use `.result()` (notes, client
  previews, journal) still get an empty result instead of an exception.
- Found while testing: DFU 6.0.8 `OptionalFieldCodec.decode` returns `success(empty)` when a present value fails to parse. A
  saved observation with one unreadable value would therefore have loaded with its whole `values` map empty and been saved that
  way, so its values were lost silently even though the entry looked readable. `ExtraCodecs.strictOptionalField` only exists
  from 1.20.2, so `KnowledgeKey.strictOptionalField` (DFU's own encoder plus a strict decoder) now backs `Observation.values`
  and `KnowledgeElement.observation`. As a side effect, the logged cause is now the inner one ("Unknown or mistyped
  observation field: temperature") instead of the misleading "Observation identity must agree…".
- Added `src/test/java/com/teammoeg/frostedresearch/knowledge/state/KnowledgeStateTest.java` (6 tests). It covers a readable
  state round-tripping to an identical tag, one bad archive entry set aside while every other section loads, raw entries in
  all seven sections surviving write/read/`TeamKnowledgeData` replace/double merge, an observation with one unreadable value
  being set aside rather than emptied, missing/newer versions and misshapen sections, and invalid observations rejected at
  construction while the codec still returns an error.

## Decisions

- Raw entries go back into their original sections rather than a separate key. A later version that can decode them
  reads them in place, with no recovery path to write. List entries are appended after the live ones. A keyed entry is
  only written back when no live entry holds the same key, so a re-set dream topic or hint list supersedes it.
- `write()` appends raw entries without deduplication, which keeps saves O(n). Only `merge` deduplicates, using a hash set.
- The constructor rule also checks `ObservationValue` payloads (state/type agreement, finite numbers). Decoding would reject
  those through the nested codec, so leaving them out would still let unloadable records be created.
  `ObservationValue`'s own constructor stays unchecked because `LinkRule` builds `known(Math.sqrt(...))` values purely for
  comparison.
- `ObservationSampling` now records non-finite `x`/`y`/`z`/`temperature` as unknown through one `number()` helper, which
  replaces the existing inline temperature guard. Sampling runs in a server tick, so a constructor exception there would
  crash the server instead of producing a record.
- Confirmed that `ObservationSampling` (environment fields plus `block_state.*` / `entity_instance`), `ItemRetrieval`
  (`item_block`, `item.*`, item-only `item_data`) and every existing test fixture build valid records under the rule.
- Did not migrate the pre-knowledge `schemaVersion: 1` layout (`acquired*Ids`). It only existed on the `knowledge-system`
  branch from `5b2713b33` to `807f43691` and never reached master.
- Did not touch `CTeamDataManager` (separate subsystem) or the four files owned by the parallel sync/access session.

## Validation

- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`: BUILD SUCCESSFUL, 114 tests (108 existing + 6 new), 0
  failures. Re-running after the final refactor gave the same result.
- Temporarily put the lenient `optionalFieldOf("values")` back. Exactly
  `observationWithOneUnreadableValueIsSetAsideRatherThanEmptied` failed, then I restored the strict version.
- `./gradlew runGameTestServer` (FTB present): the first run failed `1 required tests failed :(` after 1m 34s. The only
  failure was the known intermittent `randomEntryConvertsWithoutPageAndCarriesExactEnthalpy`
  (`result=DEFERRED, state=Block{minecraft:water}[level=0]`), the same signature recorded in
  [the previous entry](2026-10-18_22-42-01_knowledge-login-gametest-ftb-skip.md). The re-run gave **BUILD SUCCESSFUL in
  1m 21s, `All 105 required tests passed :)`**. Neither run logged a `Knowledge data: set aside` warning.
- `git diff --check`: clean.

## Remaining

- **Risk outside this change:** `CTeamDataManager.load` still skips a team whose file has any component that throws
  (`DataLoadException`), and `CTeamDataManager.save` then deletes every `.nbt` file with no loaded team. The knowledge
  component can no longer trigger this, but any other `SpecialDataType` codec that throws on load (town, research, generator,
  labour, scenario…) still loses the whole team file at the next save. The fix belongs in Chorda: keep or rename a file that
  failed to load instead of deleting it.
- While `KnowledgeSyncSnapshot` still embeds `team_data`, clients decode `KnowledgeState` on every snapshot. For a team
  with set-aside entries they repeat the warning per snapshot, and the raw NBT also travels to clients. Removing `team_data`
  (review P1-1 step 1, owned by the parallel session) removes both.
- Other DFU 6 lenient optional fields still default silently on an unreadable value: `Observation.Source` observer,
  organization, dimension and position, `AcquisitionSource.actor`, `KnowledgeEntry.learned_at`/`result_type`, and
  `KnowledgeElement.title`/`body`. Their formats are fixed primitives, so only hand-edited data can hit this.
- Hand-edited oddities stay best effort. A non-compound raw list entry cannot join a list that already has live compound
  entries, so it is not written. A keyed section that is not a compound is read as empty.
- After a downgrade the file is stamped with the older `SCHEMA_VERSION`, although set-aside entries may be in a newer layout.
  A future migration step should tolerate entries that are already migrated.
- `ItemRetrieval` now throws `IllegalArgumentException` at retrieval time for a registered empty field name or an empty
  datapack `nbt_keys` entry, where it used to persist an unloadable record. Rejecting empty keys when definitions load would
  be friendlier.
- Documentation: only the 「所有权与存储」 section of `docs/knowledge/state-and-api.md` was updated, as instructed. Its
  `Last verified` header was not stamped. `docs/knowledge/observations-and-notes.md` does not mention that non-finite
  sampled coordinates are now recorded as unknown, a case that does not arise in a healthy world.
- No in-game check of a team with set-aside entries.
