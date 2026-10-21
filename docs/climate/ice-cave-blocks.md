# Ice Cave Blocks

- Status: `Partial` (registrations, drops, and provisional models are source-defined; worldgen placement and in-game appearance are not verified)
- Last verified: `2026-10-21`
- Scope: `frostedheart:ice_crystal`, `glowstone_crystal`, `hydrothermal_rock`, `pyrolusite_ore`, `deepslate_pyrolusite_ore`, `glowstone_shard`, and `raw_pyrolusite`
- Primary code anchors: `FHBlocks.ICE_CRYSTAL`, `FHBlocks.GLOWSTONE_CRYSTAL`, `FHBlocks.HYDROTHERMAL_ROCK`, `FHBlocks.PYROLUSITE_ORE`, `FHBlocks.DEEPSLATE_PYROLUSITE_ORE`, `FHItems.GLOWSTONE_SHARD`, `FHItems.RAW_PYROLUSITE`

## Block and Drop Behavior

`ice_crystal` uses `AmethystClusterBlock(7, 3, properties)`, with six attachment directions and a small translucent prism cluster model. Its support must remain sturdy; losing support breaks the cluster. It has source light level 1 and its loot table is Silk Touch only; without Silk Touch it drops nothing. `glowstone_crystal` is a full cube rendered with the translucent layer, uses a pale-yellow block and item tint (`0xE9D78A`), has provisional source light level 7, and drops two `glowstone_shard` items with or without Silk Touch. Glowstone shards intentionally have no fluorite, glowstone-dust, or other processing tags. They are raw inputs for a later processing stage.

`pyrolusite_ore` and `deepslate_pyrolusite_ore` require a stone-tier pickaxe and drop `raw_pyrolusite`, with the standard ore Fortune count modifier; Silk Touch preserves the ore block. The default `DropExperienceBlock` constructor adds no mining experience. They carry Forge ore tags for pyrolusite and their respective stone/deepslate host. `raw_pyrolusite` is tagged as a Forge raw material but has no first-edition processing recipe.

`hydrothermal_rock` is a decorative full cube for spring-floor traces. It has no heat, fluid, or player-temperature behavior; its model combines dark smooth basalt with small calcite deposits on the top face.

## Provisional Assets and Parameters

Block/item resources are authored under `src/main/resources/assets/frostedheart/`. Crystal blocks reuse the existing FrostedHeart ice texture; the glowstone variant is tinted at render time. Pyrolusite ore reuses the existing lead-ore visuals for a provisional dark-metal appearance, and the raw item reuses the vanilla coal sprite. These are temporary visual choices, not claims about final material appearance.

`ice_crystal` light level 1, `glowstone_crystal` light level 7, and the shard drop count of 2 are provisional first-edition values pending balance review. Mining levels, strength, and experience follow the source properties in `FHBlocks`; none have separate runtime configuration.

Worldgen registration and distribution live in the companion TheWinterRescue data. This document covers the FrostedHeart resources and drop behavior only; natural frequencies and feature placement remain unverified here.
