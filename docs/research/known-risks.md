# Research System Known Risks And Validation Gaps

- Status: `Current`
- Last verified: `2026-08-22`
- Scope: Source-confirmed defects, unsafe compatibility contracts, behavioral limitations, and missing validation; this is not an exhaustive security audit
- Code anchors: [`FHDrawingDeskOperationPacket#handle`](../../src/main/java/com/teammoeg/frostedresearch/network/FHDrawingDeskOperationPacket.java), [`TeamResearchData#resetData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`FHResearchDataSyncPacket#handle`](../../src/main/java/com/teammoeg/frostedresearch/network/FHResearchDataSyncPacket.java), [`TickListenerClue#initListener`](../../src/main/java/com/teammoeg/frostedresearch/research/clues/TickListenerClue.java), [`ResearchData#getProgress`](../../src/main/java/com/teammoeg/frostedresearch/data/ResearchData.java), [`ResearchDataAPI#putVariantLong`](../../src/main/java/com/teammoeg/frostedresearch/api/ResearchDataAPI.java)

## How To Read This Document

- **Confirmed defect** means the current source has a directly identifiable incorrect/contradictory control path.
- **Fragile contract** means behavior works only while undocumented identity/order/context assumptions hold.
- **Validation gap** means source suggests a risk, but the actual failure needs a focused runtime/compatibility test.
- Priority describes likely impact to this system, not a project-wide release decision.

## Recently Resolved High-Priority Defects

| Resolved | Former risk | Current contract | Validation |
|---|---|---|---|
| 2026-08-22 | drawing-desk C2S authorization gap | `FHDrawingDeskOperationPacket#handle` now requires the sender's open `DrawDeskContainer` to reference the same loaded tile in the same level, within 8 blocks, owned by the sender's current team; operation/card-coordinate shapes are also validated | `FHDrawingDeskOperationPacketTest` covers the authorization predicate, operation shapes, and hostile coordinates |
| 2026-08-22 | incomplete administrative rollback | `TeamResearchData#resetData` now revokes current and repeat-level-recorded reversible effects, clears matching current selection, resets repeat level, restores overlapping unlocks from remaining grants, and synchronizes effect/variant/project state; infinite iteration rollover uses a separate non-revoking path | `TeamResearchDataResetTest` covers accumulated stat reversal, state/level reset, and reward-preserving infinite rollover |
| 2026-08-22 | `always` listener null-team initialization crash | tick and kill listener registration treats a null team as global scope, matching `ResearchHooks.ListenerList`; null-scope removal is also supported | `AlwaysListenerClueTest` covers global registration, cross-team dispatch, and removal for both listener families |

The current development catalogue still contains no `always: true` listener, so its complete advancement/kill event behavior should also be verified in an integrated server before deploying the first such definition.

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

Relevant unit tests currently cover:

- online/offline member collection for the no-FTB `SinglePlayerTeam` fallback;
- the kill-clue decision for matching, mismatched, and already-completed inputs;
- drawing-desk operation authorization predicates, operation shapes, and malformed card coordinates;
- administrative reset of project state, repeat level, and additive stats, plus reward-preserving infinite rollover;
- global null-team registration/dispatch/removal for `always` tick and kill listeners;
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
- shared-unlock overlap and irreversible reward behavior across reset/re-completion on an integrated server;
- full server-event/listener lifecycle for kill clues and an `always` listener backed by a real advancement/entity event;
- drawing-desk authorization with real menus/two teams, real CUI input/rendering, or full-sync archive refresh;
- FTB sidebar resource reload behavior;
- startup matrices with JEI/Create/IE absent;
- cross-system variant type and reset behavior.

## Recommended Verification Order

1. Add integrated server tests for no-FTB packet delivery and the complete kill/always-listener event lifecycle.
2. Exercise the drawing-desk authorization and reset/re-completion contracts with two players, two teams, overlapping unlock effects, and player-bound rewards.
3. Add catalogue validation for IDs, nonces, parents, cycles, numeric ranges, and minigame levels.
4. Test saved-world migrations with rename, deletion, insertion, and clue/effect reorder.
5. Open the archive, trigger a team-change/full reload, and verify discovery/layout refresh.
6. Run client/server startup matrices with optional integrations absent one at a time.
7. Exercise GUI scales and FTB sidebar resource reload in game.
