# Research System Known Risks And Validation Gaps

- Status: `Current`
- Last verified: `2026-08-21`
- Scope: Source-confirmed defects, unsafe compatibility contracts, behavioral limitations, and missing validation; this is not an exhaustive security audit
- Code anchors: [`ResearchHooks#kill`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java), [`SinglePlayerTeam#getOnlineMembers`](../../src/main/java/com/teammoeg/chorda/dataholders/team/SinglePlayerTeam.java), [`FHDrawingDeskOperationPacket#handle`](../../src/main/java/com/teammoeg/frostedresearch/network/FHDrawingDeskOperationPacket.java), [`TeamResearchData#resetData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`FHResearchDataSyncPacket#handle`](../../src/main/java/com/teammoeg/frostedresearch/network/FHResearchDataSyncPacket.java), [`TickListenerClue#initListener`](../../src/main/java/com/teammoeg/frostedresearch/research/clues/TickListenerClue.java), [`ResearchData#getProgress`](../../src/main/java/com/teammoeg/frostedresearch/data/ResearchData.java), [`ResearchDataAPI#putVariantLong`](../../src/main/java/com/teammoeg/frostedresearch/api/ResearchDataAPI.java)

## How To Read This Document

- **Confirmed defect** means the current source has a directly identifiable incorrect/contradictory control path.
- **Fragile contract** means behavior works only while undocumented identity/order/context assumptions hold.
- **Validation gap** means source suggests a risk, but the actual failure needs a focused runtime/compatibility test.
- Priority describes likely impact to this system, not a project-wide release decision.

## Highest-Priority Confirmed Defects

| Priority | Kind | Source fact | Consequence |
|---|---|---|---|
| P0 | confirmed defect | `SinglePlayerTeam#getOnlineMembers` constructs `ImmutableList.of(player)` but does not return it, then always returns an empty list | without FTB Teams, team broadcasts used by incremental research updates have no online recipients; direct login full sync can mask the problem until progress changes |
| P0 | confirmed defect | `ResearchHooks#kill` calls `setClueCompleted` only when `isClueCompleted` is already true and never evaluates the killed entity through `KillClue` | an unfinished built-in kill clue cannot complete from the normal kill hook |
| P1 | confirmed authorization gap | `FHDrawingDeskOperationPacket#handle` accepts an arbitrary loaded desk position in the sender's dimension and checks neither open menu, distance, nor desk owner | a crafted packet can operate a desk the sender could not normally open, consume its resources, or submit its examine item using the sender's team state |
| P1 | confirmed state-reversal gap | `TeamResearchData#resetData` clears project flags/maps but does not immediately revoke unlock caches, subtract `EffectStats`, reset repeat level, or clear a matching current ID | administrative reset is not a true rollback; cache behavior can change after reconstruction, and re-completion can duplicate additive stats |
| P1 | confirmed initialization defect | `Research#doIndex` can initialize an `always` listener with null team; current listener implementations dereference the team | a definition using `always: true` can fail during definition indexing/load |

The current development catalogue contains three `kill` clues in `animal_cage.json`, so the kill-hook defect affects shipped content rather than only an unused type. The same catalogue contains no `always: true` listener, making the null-team defect latent until such a definition is introduced.

## Identity, Definition, And Math Risks

| Priority | Kind | Source fact | Consequence |
|---|---|---|---|
| P1 | fragile contract | persistent state keys research/clue/effect data by string/nonce, while active IDs and packets also use registry/list indices | renames, duplicate nonces, or clue/effect reorder can lose or misapply progress without a migration |
| P1 | fragile contract | missing parent IDs are filtered out; cycles are not rejected by runtime definitions | a typo can remove an intended prerequisite; cycles can create projects that cannot be normally unlocked |
| P1 | validation gap | codecs accept zero/negative points, negative insight costs, and arbitrary clue contribution values | progress can be negative or NaN/infinite; completion thresholds can be bypassed or become impossible |
| P2 | confirmed API edge | `ResearchData#commitPoints` returns a negative input unchanged instead of rejecting it; it does not mutate `committed` because only a positive `tocommit` is applied | callers can propagate a nonsensical negative remainder even though saved progress is not reduced by this method |
| P2 | confirmed validation gap | `MinigameClue` codec construction bypasses the setter's `0..3` clamp | invalid JSON may index outside `GenerateInfo.all` during game start |
| P2 | fragile contract | config definitions are not bundled/tracked in this repository and registry snapshots store names only | a mod-only deployment or incomplete world migration can retain progress slots without usable definitions |
| P2 | confirmed inconsistency | `FHRegistry#runIfPresent(int)` uses `id - 1` while ordinary registry access is zero-based; no current callers were found | a future caller can resolve the previous slot or index `-1` |

## Persistence And Synchronization Risks

| Priority | Kind | Source fact | Consequence |
|---|---|---|---|
| P1 | confirmed notification gap | full `FHResearchDataSyncPacket` replaces client team data but emits no `ResearchUtils` archive refresh notification | an already-open archive may retain an old discovered-definition set/layout/selection after team change or reload until another event/reopen |
| P1 | fragile contract | clue/effect full and delta network data is definition-order based | a server/client catalogue mismatch can attach progress to the wrong clue/effect |
| P2 | confirmed robustness gap | several client delta handlers dereference research and list indices without null/bounds checks | stale or mismatched packets can throw on the client instead of being discarded |
| P2 | confirmed data-loss boundary | `ClueData` network compression omits optional custom NBT `data` | a future custom clue using this payload cannot expect it on the client |
| P2 | confirmed semantic issue | effect bitset read logic compares against `BitSet.size()` (storage capacity) rather than a logical definition count | the code relies on out-of-range `BitSet#get` returning false; behavior is obscure and easy to break when refactored |
| P2 | migration risk | active research persists as an integer registry slot while bodies live in separate config files | losing/replacing `fhregistries.dat` can make a saved current ID refer to another definition |
| P2 | confirmed selection side effect | normal `checkResearchComplete` unconditionally clears the team's current selection after any project completes | completing a non-current project through a bound rubbing can pause an unrelated current project |

## Gameplay And Integration Risks

| Priority | Kind | Source fact | Consequence |
|---|---|---|---|
| P1 | validation gap | `frostedresearch.mixins.json` is `required: true` and names Create/IE targets, while those mods are declared optional | startup without either optional mod must be tested; loaded-mod conditional registration alone does not prove safe absence |
| P2 | confirmed ownership gap | `MechCalcTileEntity#onClick` assigns cached points to the clicker's team and does not check a persisted owner | any player who can interact with the machine can redirect its accumulated work |
| P2 | confirmed API-contract defect | `MechCalcTileEntity#fetchPoint(int max)` ignores `max` and empties the cache | callers expecting bounded extraction receive all points |
| P2 | fragile context | Create mechanical crafting uses static `ResearchHooks.te` as part of owner context | nested/concurrent/unbalanced hooks may leak owner context between checks; needs focused execution-path testing |
| P2 | confirmed broad UI predicate | `ResearchHooks#canExamine` returns true for every nonempty item | UI can advertise examinability even when submission performs no action |
| P2 | design boundary | block and recipe hooks cover selected interaction/crafting paths, not every automation/capability path | a new machine or alternate execution route can bypass research unless explicitly integrated |
| P2 | confirmed type defect | `ResearchDataAPI#putVariantLong(ServerPlayer, String, long)` uses `putDouble` | raw callers receive the wrong NBT numeric type |
| P2 | compatibility gap | `FTBTeamsEvents#init` has its team-event registrations commented out | do not rely on that class for synchronization; current behavior depends on Chorda team events instead |

## Client/UI Limits

| Priority | Kind | Source fact | Consequence |
|---|---|---|---|
| P2 | confirmed editor omission | editor filtering passes hidden definitions to lists/details, but graph snapshot/projection/layout still exclude hidden nodes | hidden projects can be inspected through editor list/detail but cannot be seen in the graph |
| P2 | validation gap | FTB sidebar hiding/restoration uses reflection and can overwrite its cached group set when groups repopulate | behavior is best-effort across FTB Library resource reloads and requires in-game QA |
| P2 | UX limitation | `fitToVisible` centers bounds at the fixed minimum zoom `0.15` instead of computing a bounds-to-viewport scale | “fit” may leave small graphs unnecessarily tiny or large graphs partially outside the ideal view |
| P3 | intended boundary | `ResearchOpenContext.BROWSE` exists in state/tests but has no production screen/entry | no standalone read-only research browser is currently player-accessible |
| P3 | intended boundary | `EXPERIMENT` tab has no records; future town-system integration is not implemented | the tab is an empty extension boundary, not missing synchronized experiment data |

## Test Coverage Present

Tests under `src/test/java/com/teammoeg/frostedresearch` currently cover:

- experiment-point and required-clue completion behavior;
- archive construction, per-category state, and navigation back order;
- clue sorting, synthetic point presentation, tab classification, and read-only destinations;
- normal/editor definition filtering at the archive boundary;
- deterministic graph layout, cycles, missing parents, manual anchors/conflicts;
- hidden-node graph privacy, category aliases, and ancestor context projection.

## Important Gaps

There are no focused automated tests for:

- research/clue/effect JSON round trips, invalid schema, or catalogue-wide graph validation;
- stable registry migration across rename/delete/reorder scenarios;
- Chorda team NBT round trips and definition-registry mismatch recovery;
- packet encode/decode/order mismatch, malformed IDs/indices, or no-FTB incremental delivery;
- reset/re-completion effect idempotence;
- kill and always-listener clues;
- drawing-desk C2S authorization, real CUI input/rendering, or full-sync archive refresh;
- FTB sidebar resource reload behavior;
- startup matrices with JEI/Create/IE absent;
- cross-system variant type and reset behavior.

## Recommended Verification Order

1. Add regression tests for `SinglePlayerTeam#getOnlineMembers` and kill-clue completion.
2. Reproduce and constrain drawing-desk packets with two players and two team-owned desks.
3. Define reset semantics, then test effect/variant idempotence before changing the implementation.
4. Add catalogue validation for IDs, nonces, parents, cycles, numeric ranges, and minigame levels.
5. Test saved-world migrations with rename, deletion, insertion, and clue/effect reorder.
6. Open the archive, trigger a team-change/full reload, and verify discovery/layout refresh.
7. Run client/server startup matrices with optional integrations absent one at a time.
8. Exercise GUI scales and FTB sidebar resource reload in game.
