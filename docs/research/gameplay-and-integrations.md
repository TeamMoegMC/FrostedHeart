# Research Gameplay And Integrations

- Status: `Current`
- Last verified: `2026-08-21`
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

The packet handler currently checks only that the supplied position resolves to a `DrawingDeskTileEntity` in the sender's dimension. It does not re-check that the sender has that desk menu open, is nearby, or owns the desk. The block-open checks above therefore do not protect the packet endpoint; see [known-risks.md](known-risks.md).

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

The code indexes `GenerateInfo.all[level]`; although the normal design range is `0..3`, definition codec loading does not clamp it. Out-of-range levels can fail at game initialization.

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

`ResearchHooks#canExamine` returns true for every nonempty stack, regardless of whether a clue or recipe can consume it. Treat it as a UI permissiveness check, not proof that submission will do work.

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

`RubbingTool` can instead extract points through the `ComputeMachine` interface. `MechCalcTileEntity#fetchPoint(int max)` currently ignores `max` and empties the entire cache, so callers must not assume the parameter limits extraction. The calculator has no persisted team owner check on click; points go to the clicking player's current team research.

## Unlock Model

Unlock enforcement is a two-index model:

| Index | Populated from | Question answered |
|---|---|---|
| global `ResearchHooks` lock list | every loaded definition effect | “Is this object research-controlled at all?” |
| per-team unlock list | granted effect state replayed into `TeamResearchData` | “Has this team received the object?” |

The four built-in types are `BLOCK_UNLOCK_LIST`, `RECIPE_UNLOCK_LIST`, `MULTIBLOCK_UNLOCK_LIST`, and `CATEGORY_UNLOCK_LIST`. `EffectUse`, `EffectCrafting`, `EffectBuilding`, and `EffectShowCategory` declare and then grant the matching entries.

### Enforcement Sites

| Resource | Enforcement path | Important boundary |
|---|---|---|
| block use | Forge `PlayerInteractEvent.RightClickBlock` via `ResearchHooks#canUseBlock` | restricts right-click interaction, not placement, breaking, capability access, or every automation path |
| vanilla crafting | `RecipeManager`/result-container mixins and crafting-player context | targets crafting recipes; depends on a valid player context |
| Create mechanical crafting | Create recipe-grid mixins and owner UUID | uses the owner added through `IOwnerTile`; static `ResearchHooks.te` is part of the current context bridge |
| IE multiblocks | multiblock formation event/mixins and owner UUID | checks team multiblock unlock |
| IE assembler | pattern/recipe owner path | checks recipe unlock for the owning team |
| JEI | `JEICompat#syncJEI` | presentation only: hides recipes/categories and shows research information |

The system is not a universal authorization layer. A new crafting machine, automated placer, alternate recipe executor, or direct capability path must opt into these contracts if research should restrict it.

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

Other systems may also read raw string keys. Because the NBT is mutable and untyped, a key's numeric type and additive semantics are part of the integration contract. Resetting research does not subtract previous stats additions.

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
- `putVariantLong(ServerPlayer, String, long)` currently writes a double tag instead of a long tag;
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

`complete` bypasses normal materials, insight, parents, points, and clues; `complete all` skips definitions marked locked/incompletable. `reset` has the non-revocation behavior described in [state-persistence-and-sync.md](state-persistence-and-sync.md). `transfer` delegates Chorda team data transfer and operates on team UUIDs, not research IDs.

The valid names are `/research edit true` or `/frostedheart research edit true`; the shorter historical `/frostedheart edit true` is not registered by current code.

## Optional-Mod Integrations

| Mod | Registration/ID | Behavior |
|---|---|---|
| JEI | plugin UID `frostedresearch:jei_plugin` | hides locked recipes/categories, supplies research info and tooltips, resyncs on relevant effect progress |
| FTB Quests | reward type `frostedheart:insight` | claiming directly adds insight to the player's team |
| FTB Teams | Chorda team backend | progress follows team holder; legacy `FTBTeamsEvents` subscriptions are currently commented out, while Chorda team-change events provide the live sync path |
| Tetra | crafting requirement `frostedheart:research` | JSON field `research`; calls `ResearchDataAPI.isResearchComplete` |
| Create | conditional calculator, stress registration, mechanical-crafting mixins | produces points and gates owner-team recipes |
| Immersive Engineering | event/mixin hooks | gates owner-team multiblock formation and assembler recipes |

The code imports JEI classes directly and uses an annotated plugin. Create/IE classes also appear in a Mixin configuration marked `required: true` even though their mod dependencies are declared optional. Supported absence combinations need startup testing; do not infer robust optionality only from loaded-mod checks.

## Adding A New Integration

1. Decide whether it reads completion, reads a variant, or enforces a locked object.
2. Resolve authoritative team data on the server; do not trust the client mirror for game rules.
3. If enforcing an object, populate both the global restriction declaration and the per-team grant/replay path.
4. Define ownership for automation and fake players explicitly.
5. Add full-load/relogin behavior, not only incremental event behavior.
6. Test no-team fallback, FTB team changes, `/reload`, and an existing saved team.
7. Document identifiers, units, defaults, bypasses, and revocation/reset behavior here.
