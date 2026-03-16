# Project Structure

## Overview
**Frosted Heart** is the core mod for "The Winter Rescue" (冬季救援) Minecraft modpack.
- **Minecraft**: 1.20.1, **Forge**: 47.3.0 (NeoForge compatible)
- **Java**: 17, **Encoding**: UTF-8
- **Build**: ForgeGradle + Mixin + Parchment mappings
- **License**: GPL-3.0
- **Owner**: TeamMoeg (com.teammoeg)

## Three Sub-Mods in One Repo
All under `src/main/java/com/teammoeg/`:

| Module | Files | Description |
|--------|-------|-------------|
| `frostedheart` | ~910 | Main game mod - temperature, climate, town, energy, agriculture, scenarios |
| `chorda` | ~435 | **Library mod** - reusable utilities, UI framework, IO, math, capabilities |
| `frostedresearch` | ~151 | Research & tech tree system - insights, recipes, research data |

## Chorda Library (com.teammoeg.chorda) - Fully Documented
General-purpose library. All 377 Java files + 58 package-info.java have bilingual Javadoc.

### Package Map
```
chorda/                          Root: Chorda.java (mod entry), ChordaConfig, ChordaNetwork, CompatModule
├── asm/                         ASM bytecode generation (invokedynamic constructor factories)
├── block/                       Block base classes (CBlock, CGuiBlock, CKineticBlock, CActiveMultiblockBlock...)
│   └── entity/                  Block entity bases (CBaseBlockEntity, CTickableBlockEntity, sync helpers)
├── capability/                  Forge Capability wrappers (CapabilityRegistry, CapabilityDispatchBuilder)
│   ├── capabilities/            Concrete: ChangeDetectedItemHandler/FluidHandler, ItemHandlerWrapper
│   └── types/                   CapabilityType interface
│       ├── codec/               Codec-based serialization (object replacement semantics)
│       ├── nbt/                 NBT-based serialization (in-place update semantics)
│       └── nonpresistent/       Transient/runtime-only capabilities
├── client/                      All client-side code
│   ├── cui/                     CUI Framework - custom declarative UI system
│   │   ├── base/                Core UI elements, drawables, layout
│   │   ├── category/            Tab/category browsing
│   │   ├── contentpanel/        Scrollable content panels
│   │   ├── editor/              In-game data editors
│   │   │   └── compat/          Editor mod compatibility
│   │   ├── menu/                CUI slot integration (CUISlot, DeactivatableSlot)
│   │   ├── screenadapter/       CUI→Minecraft Screen adapters (CUIScreen, CUIOverlay)
│   │   ├── theme/               Theme system (VanillaTheme, SimpleTechTheme)
│   │   └── widgets/             Widget library (Button, TextField, ScrollBar, Panel, ItemSlot, CheckBox...)
│   ├── icon/                    FlatIcon rendering system
│   ├── model/                   Custom model loaders
│   ├── ui/                      Low-level: UV, TexturedUV, TextPosition, PointSet
│   └── widget/                  Vanilla Widget extensions (IconButton, ScrollBarWidget, TabImageButton...)
├── compat/                      Mod compatibility
│   └── ftb/                     FTB Teams/Chunks integration
├── config/                      ConfigFileType
├── creativeTab/                 Creative tab helpers (CreativeTabItemHelper, TabType)
├── dataholders/                 Extensible data storage framework
│   ├── client/                  Client-side data holders
│   ├── team/                    Team-based data (CTeamDataManager, TeamDataHolder, TeamsAPI)
│   └── world/                   World-level SavedData storage
├── effect/                      BaseEffect (custom potion effects)
├── events/                      Custom Forge events (TeamCreated, PlayerTeamChanged, MenuSlotEncoder...)
├── io/                          IO & serialization root (CodecUtil, PacketWritable...)
│   ├── codec/                   Extended Codecs (CompressDiffer, KeyMap, Optional, Alias...)
│   ├── marshaller/              Reflection-based auto NBT marshalling (Marshaller, ClassInfo, ReflectionCodec)
│   ├── nbtbuilder/              Fluent NBT builders (CompoundNBTBuilder, ArrayNBTBuilder)
│   └── registry/                Polymorphic serializer registries (TypeRegistry, TypedCodecRegistry, SerializerRegistry)
├── item/                        CBlockItem
├── listeners/                   Forge event listeners (Client/Common/Server events, registry)
├── math/                        Math utilities (CMath, Colors, Point, Rect, random generators)
│   ├── evaluator/               Math expression parser/evaluator (AST-based)
│   └── noise/                   Noise generators (OpenSimplex, Cellular/Worley, Metaballs)
├── menu/                        Container menu system (CBaseMenu, CBlockEntityMenu, CMultiblockMenu)
│   └── slots/                   Custom slots (ArmorSlot, PagedSlot, UIFluidTank)
├── mixin/                       Mixin injections
├── multiblock/                  IE multiblock templates
│   └── components/              Multiblock functional components
├── network/                     Network (CBaseNetwork, CMessage, SimpleChannel wrappers)
├── recipe/                      Codec-based recipe system
├── scheduler/                   Tick-based task scheduler (SchedulerQueue)
├── text/                        Text utils (Components, LangBuilder, CFormatHelper, ComponentOptimizer)
└── util/                        General utilities (CUtils, CRegistryHelper, CDamageSourceHelper...)
    ├── parsereader/              Text parsing framework
    │   └── source/              Line sources (Reader, Stream, String, StringList)
    └── struct/                  Data structures (FastEnumMap, PairList, OptionalLazy, LazyTickWorker...)
```

## Frosted Heart Main Mod (com.teammoeg.frostedheart)
### Key Packages
```
frostedheart/
├── block/                       Custom blocks (gunpowder barrel etc.)
├── bootstrap/                   Mod initialization (client/common/reference)
├── clusterserver/               Cluster server networking
├── compat/                      Mod compat (Create, IE, Curios, JEI, Caupona, Tetra, FTBQ)
├── content/                     Main game content (largest section)
│   ├── agriculture/             Farming and crop systems
│   ├── archive/                 Archive/lore system
│   ├── climate/                 Core: temperature, weather, climate simulation
│   ├── decoration/              Decorative blocks
│   ├── energy/                  Energy systems
│   ├── health/                  Player health mechanics
│   ├── incubator/               Incubator mechanics
│   ├── loot/                    Loot table modifications
│   ├── robotics/                Robot/automation systems
│   ├── scenario/                Story scenario scripting
│   ├── steamenergy/             Steam power system
│   ├── tips/                    In-game tips/tutorials
│   ├── town/                    Core: town building & management system
│   ├── trade/                   Trading system
│   ├── utility/                 Misc utility content
│   ├── water/                   Water mechanics
│   ├── waypoint/                Waypoint system
│   ├── wheelmenu/               Radial/wheel menu
│   └── world/                   World generation
├── events/                      Event handlers
├── infrastructure/              Commands, config, data loading, world gen
├── item/                        Custom items
├── mixin/                       Mixins (Minecraft, Forge, Create, IE, FTB, Curios, Tetra...)
├── restarter/                   Server restart utilities
└── util/                        Utility classes
```

## Frosted Research (com.teammoeg.frostedresearch)
### Key Packages
```
frostedresearch/
├── api/                         Public API
├── blocks/                      Research-related blocks
├── compat/                      Mod compatibility
├── data/                        Research data definitions
├── events/                      Research event handlers
├── gui/                         Research GUI screens
├── handler/                     Research handlers/processors
├── insight/                     Insight (research points) system
├── item/                        Research items
├── network/                     Research networking
├── number/                      Number/resource system
├── recipe/                      Research recipes
└── research/                    Core research tree logic
```
