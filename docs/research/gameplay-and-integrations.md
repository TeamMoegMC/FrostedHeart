# Research Gameplay And Integrations

- Status: `Current`
- Last verified: `2026-08-27`
- Scope: Player entry points, legacy progression, the Phase 2 knowledge-laboratory loop, V2 results/prototypes, unified unlock enforcement, APIs, events, commands, and compatibility modules
- Code anchors: [`ResearchNotebookItem`](../../src/main/java/com/teammoeg/frostedresearch/item/ResearchNotebookItem.java), [`ObservationProviderRegistry`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationProviderRegistry.java), [`ResearchWorkflowRegistry`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchWorkflowRegistry.java), [`TeamResearchService`](../../src/main/java/com/teammoeg/frostedresearch/api/TeamResearchService.java), [`GeologyResearchIntegration`](../../src/main/java/com/teammoeg/frostedheart/content/utility/oredetect/GeologyResearchIntegration.java), [`PersonKnowledgeIntegration`](../../src/main/java/com/teammoeg/frostedheart/content/town/resident/PersonKnowledgeIntegration.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java)

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

## Phase 2 Observation, Discovery, And Research Loop

`frostedresearch:research_notebook` is a non-stackable early observation tool, not a topic selector. Using it on a block or living entity begins a `40` tick (`2 s`) capture; releasing early records nothing, and a HUD bar shows progress. The resulting `ObservationContext` can retain location, game/day time and a derived dawn/day/dusk/night period, biome, vanilla weather, visible block state, and provider measurements. Sneak-use in air cycles `STANDARD`, `COMPACT`, and `ENVIRONMENT` field sets. `ENVIRONMENT` includes block temperature when the player also carries a soil thermometer. `ObservationProviderRegistry` selects the highest-priority provider that supports a block context; its catch-all `GenericBlockObservationProvider` makes every block recordable even when no content topic recognizes it, while the entity fallback archives type and entity UUID. Generic block observations deduplicate by kind, dimension, exact position, subject, and retained visible state, then merge observers and observation time.

Providers may enrich the same public record contract with a specialized kind, facets, deduplication policy, or sealed server fact. Frosted Heart registers `GeologyBlockObservationProvider`: any `forge:ores` block and any `forge:stone` block receive stable geology facets and a `16³` cell semantic key; stone also seals `OreProspectingModel.scan(level, position, 4, 3)`. Its scan volume is half-open, with horizontal offsets `[-4,4)` and vertical offsets `[-3,3)`. Those mineral counts remain server-only. The prospector pick and geologist's hammer use the same read-only model but keep their own durability, noise, and player feedback.

At the drawing desk, the Knowledge Laboratory accepts at most five record pins. Starting “Organize” asks every loaded topic's registered direct Idea rule to match any subset of those pins; no match consumes no paper or ink. One or more matches consume one compatible level-0 paper and five pen uses, then run the existing card game as `DrawingDeskTileEntity.GamePurpose.V2_INSPIRATION`. The synchronized `DrawingDeskTileEntity.InspirationStatus` explains a no-match, missing-paper, or missing-ink rejection without naming the hidden topic. The active evidence set is frozen while the game runs. Players can move freely between the card board and laboratory, whose controls display the same session and offer an explicit cancel. Candidates are revalidated after completion; a single surviving match immediately persists or merges its `IdeaRecord`, while two or three surviving matches ask the player to choose. `LEGACY_CLUE` games still use `GamePurpose.LEGACY_CLUE` and call `ResearchHooks.commitGameLevel` unchanged.

Only persisted ideas compile action cards. `ResearchWorkflowRegistry#actionCards` asks registered protocol handlers for unfinished ideas and projects at most three actions. An `ActionCard` names its topic, protocol, presentation action, and whether it is executable; clicking it sends that exact topic/protocol pair. `ProtocolHandler.Execution` may append a comparison artifact or complete a lightweight theory without fabricating one, then attaches evidence and updates Idea state. The registered resolution handler decides whether result acceptance is legal.

The first geology fixture is intentionally elementary: any recorded stone plus any recorded ore can produce `frostedheart:rock_and_ore_signs`, regardless of distance, order, extra pins, or mineral scan contents. After the card game records that Idea, one “develop a simple theory from the notes” action moves it to review, and acceptance grants the existing Finding and copper-prospecting-pick Design. Nearby/control samples and `MATCH / NO_MATCH / INSUFFICIENT` are no longer gates for this introductory topic; old comparison artifacts remain readable history.

## Bundled Geology Content Plugin

`GeologyResearchIntegration` is Frosted Heart content layered on those generic interfaces. Its field-evidence pattern reads only public facets and can reveal `frostedheart:rock_and_ore_signs` from any recorded `forge:ores` block plus any recorded `forge:stone` block. It does not inspect distance, order, a fixed pin count, or the sealed mineral snapshot. The topic definition retains its earlier comparison protocol identifiers during rapid iteration, but the registered runtime handler now interprets that method as the single lightweight theory action and produces no new `FieldComparisonArtifact`; artifacts already saved by earlier builds remain readable history.

Accepting the ready geology Idea atomically acquires `frostedheart:prospecting_signs_indicate_nearby_ore` and `frostedheart:copper_prospecting_pick`. The registered `frostedheart:prospecting_report_detail` Finding view may then annotate a rock-sample summary with `frostedheart:ore_trace_present` or `frostedheart:ore_trace_absent`; it still does not expose mineral counts. The copper prospector's pick retains its more detailed immediate result, so the Finding and tool do not replace each other.

## Person Knowledge Packages

Refugee and simulated-resident dialogue uses the domain-neutral “Talk about your experience” action and `PersonKnowledgeDialogue#shareFirst`. `PersonKnowledgePackageCatalog` is a generic ordered registration surface; Frosted Heart's `PersonKnowledgeIntegration` registers the current content packages during bootstrap. `PersonKnowledgeOverlay` persists a one-time background roll and a set of package IDs, including a successfully initialized empty set. Adult and elder refugees receive `frostedheart:prospecting_experience` on rolls `0..9`, `frostedheart:cold_weather_experience` on `10..19`, and no current package on the remaining rolls; other ages receive none. A package definition determines its reply and optional `KnowledgeOffer`: prospecting experience merges the geology Idea but does not complete its comparison, while cold-weather experience is currently conversation-only. An empty person gives a domain-neutral no-experience reply rather than guessing that the player asked about prospecting.

Recruitment copies the overlay to the new `Resident` before `TeamTown#addResident`. A failed insertion does not mutate or discard the refugee; a successful resident retains the same initialized package set and can use the shared dialogue path without rerolling.

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
| `4` | match current pins and initialize a V2 inspiration game |
| `5` | toggle one synchronized observation UUID in the five desk-local pins |
| `6` | clear the desk-local knowledge session |
| `7` | revalidate and record candidate index `0..2` |
| `8` | execute the exact topic/protocol pair named by a currently executable action card |
| `9` | accept the first ready topic resolution |

`ClientResearchGame` performs interaction/display checks but does not authoritatively advance the game. `DrawingDeskTileEntity` owns game state, consumes paper/pen durability, checks matches, synchronizes its NBT, and completes a `MinigameClue` on success.

The packet endpoint treats its position, card coordinates, record UUID, candidate index, topic ID, and protocol ID as untrusted. Before executing an operation, `FHDrawingDeskOperationPacket#handle` requires the sender's current menu to be a `DrawDeskContainer` bound to that exact loaded tile, in the sender's server level, no farther than 8 blocks from the tile center, with an `IOwnerTile` owner equal to the sender's current Chorda team UUID. Every operation has a fixed payload shape; card positions must be inside the `9 x 9` board, pin UUIDs must resolve in the sender's team knowledge, candidate indices are limited to `0..2` and revalidated against the current pins, and protocol execution re-resolves the named topic/protocol/Idea/handler on the server.

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

## Unified Unlock Model

Legacy definitions still populate global and per-team unlock lists, but execution now consumes `TechnologyAccessProjection`, which combines those lists' authority with V2 results while retaining source identity:

| Channel | Managed by | Valid team sources |
|---|---|---|
| recipe | V2 Design targets plus legacy `EffectCrafting` declarations | acquired Design or completed/granted recipe effect |
| multiblock formation | V2 Construction targets plus legacy `EffectBuilding` declarations | acquired Construction or completed/granted building effect |
| right-click block use | V2 Procedure targets plus legacy `EffectUse` declarations | acquired Procedure or completed/granted use effect |

The access rule remains default-open: a target absent from both the V2 managed universe and the matching legacy lock list is available. A managed target needs at least one source. Category locking remains legacy-only through `CATEGORY_UNLOCK_LIST`.

`AccessDecision` preserves all current provenance. V2 sources identify topic, result type, and result ID. Legacy sources identify research ID and effect nonce and are rebuilt from completed plus granted `effectData`, not guessed from source-less unlock sets.

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
| JEI | `ResearchJeiBridge#sync` / `JEICompat#syncJEI` | presentation only: schedules visibility changes on the client main thread and matches runtime entries by stable recipe ID, including after grant/revoke |

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

V2 adds:

- `KnowledgeDataAPI` / `ClientKnowledgeDataAPI` for independent team knowledge and the atomic client projection mirror;
- `TechnologyAccessResolver#hasFinding`, `#isRecipeUnlocked`, `#canFormMultiblock`, and `#canUseBlock` as the narrow Boolean adapters;
- `TeamResearchService` as the observation/Idea/protocol/resolution/result mutation boundary;
- `TeamResearchManager#grantResult` as a compatibility facade rather than a mutable store.

Callers should note:

- `getVariants` returns the mutable underlying `CompoundTag`, not a defensive copy;
- mutations through the raw tag require an explicit sync;
- both typed and raw-string `putVariantLong` overloads write `LongTag`, including values above `2^53`, and variant snapshots preserve that type through save/sync/reset;
- `TeamResearchManager` is a compatibility facade; new result mutations belong in `TeamResearchService`.

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
result grant|revoke|info <result-resource-id>
<online-player> result grant|revoke|info <result-resource-id>
```

`complete` bypasses normal materials, insight, parents, points, and clues; `complete all` skips definitions marked locked/incompletable. `reset` rolls back recorded reversible effects, selection, progress, and repeat level, but cannot recover already-issued item/experience/command rewards or infer activation-cost refunds; see [state-persistence-and-sync.md](state-persistence-and-sync.md). `transfer` delegates Chorda team data transfer and operates on team UUIDs, not research IDs.

Without a player argument, `result` operates on the command source's team. `/research <online-player> result ...` instead resolves that player's current Chorda team; the same shape is available under the `/frostedheart research` alias.

`result grant` accepts only a result in the current V2 snapshot. Finding, Design, Construction, and Procedure acquire an idempotent team ID. Prototype creates a non-stackable `frostedresearch:upgrade_prototype` for the affected player with frozen profile revision, a new serial UUID, and that player's current team UUID, then uses Forge's existing inventory-or-nearby-drop delivery helper. It does not issue the topic's ordinary rewards.

`result revoke` idempotently removes a Finding, Design, Construction, or Procedure ID and sends a full projection refresh. It can remove retained orphan IDs even when their catalogue definition is absent. Prototype revoke is rejected because Prototype authority is the physical item, not team knowledge. `result info` reports the affected team, catalogue topic/type/payload, acquisition categories, orphan status, and prototype profile revision without mutating state. Suggestions include both current catalogue results and the affected team's orphan IDs.

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
