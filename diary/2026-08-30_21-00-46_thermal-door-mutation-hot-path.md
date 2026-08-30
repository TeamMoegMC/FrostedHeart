# Thermal door mutation hot-path closure

- Time: `2026-08-30 21:00:46 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `section mutation source filtering, immutable Page publication reuse, plan, and living thermal documentation`

## Completed

- Preserved the existing `SOURCE_MUTATION` classification through
  `MinecraftThermalInput` and `MinecraftPageManager.SectionOwner` as one
  cut-level boolean. A door, fence gate, trapdoor, or ordinary material-only
  cut no longer calls `PhysicalSourceSpatialIndex.resyncBlock` or reads block
  state for source discovery.
- Made `WorkerPageStore` clone the 64-reference publication directory only on
  the first real Brick query-payload write. A geometry/topology identity-only
  publication now shares the previous private immutable directory.

## Decisions

- Source relevance is section/cut-level rather than a third 4096-position
  bitmap. If a real campfire mutation shares a section and cut with other
  changes, resyncing those already coalesced positions is the smaller retained
  memory and code-complexity tradeoff.
- `PagePublication.withIdentities` does not expose the owned Brick array and
  skips both the directory clone and repeated directory validation.
- No production counter, test hook, collection, compatibility branch, or new
  traversal was added.

## Validation

- Java 17 production, test, and GameTest compilation: passed.
- Thermal JUnit: `96/96` passed.
- Forge GameTest: all `14/14` required tests passed.
- Residual source search confirms every new field, constant, and method has a
  production reader.
- `git diff --check`: passed.

## Documentation

- Updated the living thermal architecture with source-relevant cut filtering
  and identity-only publication-directory reuse.
- Updated the active async-runtime plan and marked all three closure items
  complete.

## Remaining

- Run a controlled before/after door JFR before assigning a measured CPU or
  allocation improvement to this change.
