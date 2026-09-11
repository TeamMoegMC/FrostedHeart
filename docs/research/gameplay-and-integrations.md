# Research Gameplay And Integrations

- Status: `Current`
- Last verified: `2026-08-23`
- Scope: Player entry points, drawing-desk operations, experiment/insight sources, unlock enforcement, variants, APIs, events, commands, and compatibility modules
- Code anchors: [`DrawingDeskBlock`](../../src/main/java/com/teammoeg/frostedresearch/blocks/DrawingDeskBlock.java), [`DrawingDeskTileEntity`](../../src/main/java/com/teammoeg/frostedresearch/blocks/DrawingDeskTileEntity.java), [`DrawDeskContainer`](../../src/main/java/com/teammoeg/frostedresearch/gui/drawdesk/DrawDeskContainer.java), [`ResearchHooks`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java), [`MechCalcTileEntity`](../../src/main/java/com/teammoeg/frostedresearch/blocks/MechCalcTileEntity.java), [`ResearchDataAPI`](../../src/main/java/com/teammoeg/frostedresearch/api/ResearchDataAPI.java), [`ResearchCommand`](../../src/main/java/com/teammoeg/frostedresearch/ResearchCommand.java), [`JEICompat`](../../src/main/java/com/teammoeg/frostedresearch/compat/JEICompat.java)

## Player-Facing Loop

The implemented loop is:

1. Earn insight from exploration, inspiration items, rewards, or commands.
2. Open a team-owned drawing desk and browse the archive.
3. Pay a research's item and insight-level cost to activate it.
4. Accumulate direct experiment points and complete clue-specific activities.
5. Meet both the point target and all required-clue gates.
6. Receive team unlocks/attributes automatically and claim player-bound rewards.
7. Use the resulting recipe, block, multiblock, JEI-category, generator, forecast, or villager behavior.

The archive is a view/controller over this loop. Detailed layout and navigation behavior live in [research-ui.md](research-ui.md).

## Drawing Desk Entry

The block ID is `frostedresearch:drawing_desk`; the menu ID is `frostedresearch:draw_desk`. `DrawingDeskBlock` is a two-block structure with its block entity on the main half.

A normal open requires:

- server-side main-hand interaction while not sneaking;
- either creative mode or local raw brightness of at least `8`;
- an uncancelled `DrawDeskOpenEvent`;
- for non-creative players, body temperature of at least `-1.0` as enforced by `ResearchCommonEvents#onDrawDeskOpen`;
- team ownership: the first open assigns the current Chorda team UUID, and later opens require the same team.

The owner is provided by the `IOwnerTile` mixin contract. `BlockEntityMixin_Research` writes a `fhowner` value into block-entity NBT, so ownership survives reload.

## Drawing Desk Inventory And Operations

`DrawDeskContainer` exposes three block-entity slots:

| Slot | Constant | Accepted content |
|---:|---|---|
| `0` | `EXAMINE_SLOT` | any item |
| `1` | `PAPER_SLOT` | an ingredient matching a `frostedresearch:paper` recipe |
| `2` | `INK_SLOT` | an `IPen` with usable durability |

The client sends `FHDrawingDeskOperationPacket` operations against the desk's `BlockPos`:

| `op` | Server action |
|---:|---|
| `0` | initialize a card game |
| `1` | flip/select one card position |
| `2` | try to combine two card positions |
| `3` | submit the examine-slot item |

`ClientResearchGame` performs interaction/display checks but does not authoritatively advance the game. `DrawingDeskTileEntity` owns game state, consumes paper/pen durability, checks matches, synchronizes its NBT, and completes a `MinigameClue` on success.

The packet endpoint treats its position and card coordinates as untrusted. Before executing an operation, `FHDrawingDeskOperationPacket#handle` requires the sender's current menu to be a `DrawDeskContainer` bound to that exact loaded tile, in the sender's server level, no farther than 8 blocks from the tile center, with an `IOwnerTile` owner equal to the sender's current Chorda team UUID. Operations `0` and `3` accept no card positions, operation `1` accepts exactly one, and operation `2` exactly two; every supplied position must be inside the `9 x 9` board.

## Theory Card Game

`ResearchHooks#fetchGameLevel` chooses the first unfinished `MinigameClue` in the current research and returns its configured level.

Starting a game requires:

- a current unfinished minigame clue;
- one paper matched by a `ResearchPaperRecipe` whose `level` is at least the clue level;
- an `IPen` whose level is at least the clue level and which can pay `5` durability/uses.

The desk consumes one paper and the initial pen cost, then creates a server-side `ResearchGame` using `GenerateInfo.all[level]`. Each successful combine attempt costs another `1` pen use. When the board finishes, `ResearchHooks#commitGameLevel` completes the first unfinished minigame clue whose required level is less than or equal to the won level.

The built-in recipe JSON shape is:

```json
{
  "type": "frostedresearch:paper",
  "item": { "item": "namespace:item" },
  "level": 0
}
```

The code indexes `GenerateInfo.all[level]`; both the codec and whole-catalogue validator enforce the supported `0..3` range before a definition can be installed.

## Examine-Slot Submission

`ResearchHooks#submitItem` evaluates the submitted stack in this order:

1. Every `ItemClue` on the current research tests the stack; a consuming clue shrinks it by the matched count and marks itself complete.
2. A `RubbingTool` already carrying `research` and positive `points` NBT submits those points to the bound research ID, even if it is not the current selection, and becomes a `rubbing_pad`.
3. An unbound `RubbingTool` records the current research ID and remains in the slot.
4. The first matching `InspireRecipe` consumes one item and adds its configured insight amount.

Because the loop visits all item clues, a stack matching several current clues can be tested/consumed several times as long as items remain.

An inspiration recipe uses:

```json
{
  "type": "frostedresearch:inspire",
  "item": { "item": "namespace:item" },
  "amount": 1
}
```

`ResearchHooks#canExamine` is the side-effect-free model shared by the button affordance and server action rules. It returns true only for an unfinished matching item clue, an unbound rubbing tool with a current research, a bound positive-point rubbing whose target is active and unfinished, or a matching inspiration recipe. Ordinary items and invalid/empty rubbings therefore do not enable the button or send an operation packet.

## Mechanical Calculator

When Create is loaded, `frostedresearch:mechanical_calculator` produces cached experiment points from kinetic speed:

| Property | Source value/behavior |
|---|---|
| accepted absolute speed | `0 < speed <= 64` RPM units as exposed by Create |
| processing target | `6400` accumulated speed-ticks |
| output per completed cycle | `20` experiment points |
| point cache | maximum normal production `100` |
| stress impact while able to work | `64` |

Each server tick adds the integer absolute speed to `process`. When it reaches `6400`, the machine resets process and adds `20` cached points. Clicking transfers as many cached points as the current research accepts through `TeamResearchData#doResearch`, leaving any unused amount in the machine.

The calculator records the placing real player's current team in the existing `IOwnerTile` NBT. A legacy ownerless calculator is claimed by the first direct or rubbing interaction from a real `ServerPlayer`. Fake players never claim or extract, and another team cannot replace the owner, redirect direct-click progress, inspect the extractable balance, or remove cached points.

`RubbingTool` extracts through the player-authorized `ComputeMachine` interface. `fetchPoint(ServerPlayer,max)` returns zero for `max <= 0` or an unauthorized player; otherwise it removes exactly `min(max,currentPoints)`. The current rubbing workflow waits for at least `100` authorized points and extracts exactly `100`, retaining any excess machine cache.

## Unlock Model

Unlock enforcement is a two-index model:

| Index | Populated from | Question answered |
|---|---|---|
| global `ResearchHooks` lock list | every loaded definition effect | “Is this object research-controlled at all?” |
| per-team unlock list | granted effect state replayed into `TeamResearchData` | “Has this team received the object?” |

The four built-in types are `BLOCK_UNLOCK_LIST`, `RECIPE_UNLOCK_LIST`, `MULTIBLOCK_UNLOCK_LIST`, and `CATEGORY_UNLOCK_LIST`. `EffectUse`, `EffectCrafting`, `EffectBuilding`, and `EffectShowCategory` declare and then grant the matching entries.

### Execution Ownership Contract

All current enforcement entry points resolve authority through one of two contexts:

- player paths use the real player's current Chorda team; a null player or `FakePlayer` fails closed for restricted content;
- machine paths use a persisted owner UUID; a null/unknown owner may execute unrestricted content but fails closed for research-restricted content.

This is an integration contract, not a global interception mechanism. Every recipe executor, formation path, or automation mod still needs an explicit call site.

### Enforcement Sites

| Resource | Enforcement path | Important boundary |
|---|---|---|
| block use | Forge `PlayerInteractEvent.RightClickBlock` via `ResearchHooks#canUseBlock` | restricts right-click interaction, not placement, breaking, capability access, or every automation path |
| vanilla crafting | `RecipeManager`/result-container mixins and crafting-player context | absent/fake player fails closed for locked recipes |
| campfire | player passed into the real campfire placement recipe path | follows the same player-team rule |
| Create mechanical crafting | `MechanicalCrafterBlockEntityMixin` scopes persisted owner around `tryToApplyRecipe`; recipe-grid and recipe `matches` read it | nested, exception-safe `ThreadLocal`; ownerless locked recipes fail closed |
| IE multiblocks | player formation event plus persisted owner UUID for machine/server checks | FakePlayer formation is rejected; ownerless locked targets fail closed |
| IE assembler | persisted pattern owner on server, client team only for preview | checks recipe unlock for the owning team |
| generator upgrade | generator state's persisted owner | client preview uses local team; server mutation uses owner and fails closed |
| JEI | `JEICompat#syncJEI` | presentation only: hides recipes/categories and shows research information |

The system is not a universal authorization layer. A new crafting machine, automated placer, alternate recipe executor, or direct capability path must opt into these contracts if research should restrict it. The outstanding third-party-machine audit is tracked in `plans/`; current code does not claim unknown paths are covered automatically.

## Stats And Cross-System Variants

`EffectStats` adds a number to `TeamResearchData.variants`, an arbitrary `CompoundTag`:

```text
delta = percent ? val / 100 : val
variants[vars] = existing + delta
```

The enum `ResearchVariant` documents common tokens but does not constrain `EffectStats.vars`:

| Token | Current consumers |
|---|---|
| `generator_effi` | generator fuel/efficiency calculations |
| `generator_heat` | generator heat calculations |
| `overdrive_recover` | generator overdrive recovery / town operational status |
| `has_forecast` | forecast availability and related HUD |
| `vlg_relationship` | villager relationship behavior |
| `vlg_forgive` | villager forgiveness behavior |
| `max_energy` | enum token present; no current repository consumer found |
| `max_energy_multiplier` | enum token present; no current repository consumer found |

Other systems may also read raw string keys. Because the NBT is mutable and untyped, a key's numeric type and additive semantics are part of the integration contract. Administrative reset reverses recorded `EffectStats` applications, including completed infinite iterations. Load replay does not add them again because the exact typed NBT is already persisted.

## Public APIs

`ResearchDataAPI` is the main bridge:

- `getData(Player)` resolves server team data or the client mirror;
- `getData(UUID)` resolves a team holder by internal UUID;
- `isResearchComplete(Player, String)` is the stable compatibility check used by Tetra;
- `getVariantDouble/Long`, `putVariantDouble/Long`, `getVariants`, and `sendVariants` expose attributes.

`ClientResearchDataAPI` exposes the local Chorda mirror. The older `ClientResearchData.last` field is only a legacy UI selection cache, not authoritative progress.

Callers should note:

- `getVariants` returns the mutable underlying `CompoundTag`, not a defensive copy;
- mutations through the raw tag require an explicit sync;
- both typed and raw-string `putVariantLong` overloads write `LongTag`, including values above `2^53`, and variant snapshots preserve that type through save/sync/reset;
- `TeamResearchManager` is presently an empty placeholder, not a service layer.

## Events

| Event | Side/timing | Intended use |
|---|---|---|
| `ResearchLoadEvent.Pre/Post/Finish` | server definition load | extend/observe catalogue indexing |
| `PopulateUnlockListEvent` | common rebuild | add global research-controlled objects |
| `ResearchDataLoadedEvent` | team load/init | observe reconstructed team state |
| `ResearchStatusEvent` | server progress/completion | react to authoritative state changes |
| `ClientResearchStatusEvent` | client incremental update | refresh integrations such as JEI |
| `DrawDeskOpenEvent` | server block interaction, cancelable | impose additional drawing-desk access rules |

Archive-specific client notifications are not Forge events; they route through `ResearchUtils`/`ResearchGui`.

## Administrative Commands

All branches require permission level `2` and are registered under both `/research ...` and `/frostedheart research ...`:

```text
insight add|get|getLevel|getUsedLevel|set|setLevel|setUsedLevel
complete <research-id>|all
transfer <from-team-uuid> <to-team-uuid>
edit <true|false>
attribute <name>|all|set <name> <nbt>
reset <research-id>|all
info <research-id>
get <research-id> <field>
```

`complete` bypasses normal materials, insight, parents, points, and clues; `complete all` skips definitions marked locked/incompletable. `reset` rolls back recorded reversible effects, selection, progress, and repeat level, but cannot recover already-issued item/experience/command rewards or infer activation-cost refunds; see [state-persistence-and-sync.md](state-persistence-and-sync.md). `transfer` delegates Chorda team data transfer and operates on team UUIDs, not research IDs.

The valid names are `/research edit true` or `/frostedheart research edit true`; the shorter historical `/frostedheart edit true` is not registered by current code.

## Optional-Mod Integrations

| Mod | Registration/ID | Behavior |
|---|---|---|
| JEI | plugin UID `frostedresearch:jei_plugin` | hides locked recipes/categories, supplies research info and tooltips, resyncs on relevant effect progress |
| FTB Quests | reward type `frostedheart:insight` | claiming directly adds insight to the player's team |
| FTB Teams | Chorda team backend | Chorda's event bridge is the sole contract for join, leave, creation/deletion, ownership transfer, data transfer, and `PlayerTeamChangedEvent`; Frosted Research has no duplicate FTB event registration |
| Tetra | crafting requirement `frostedheart:research` | JSON field `research`; calls `ResearchDataAPI.isResearchComplete` |
| Create | calculator, stress registration, mechanical-crafting mixins | required by the product; produces points and gates owner-team recipes |
| Immersive Engineering | event/mixin hooks | required by the product; gates player/team multiblock formation and owner-team assembler recipes |

Ordinary research code accesses JEI through `ResearchJeiBridge`, so no-JEI startup does not load the annotated plugin class. FTB-specific mixins are gated by class presence and the sidebar mixin is `@Pseudo`. The Forge GameTest run supports `-PwithoutJei -PwithoutFtb`; both that combination and the full runtime have passed dedicated-server startup plus the calculator, listener lifecycle, orphan, non-current completion, and reversible-effect tests. Create and IE remain required in the product and their absence is outside the supported matrix.

## Adding A New Integration

1. Decide whether it reads completion, reads a variant, or enforces a locked object.
2. Resolve authoritative team data on the server; do not trust the client mirror for game rules.
3. If enforcing an object, populate both the global restriction declaration and the per-team grant/replay path.
4. Define ownership for automation and fake players explicitly.
5. Add full-load/relogin behavior, not only incremental event behavior.
6. Test no-team fallback, FTB team changes, `/reload`, and an existing saved team.
7. Document identifiers, units, defaults, bypasses, and revocation/reset behavior here.
