# Ice Cave Blocks

- Status: `Current` (checked against source; source values may be newer than the installed JAR)
- Last verified: `2026-10-22`
- Scope: `frostedheart:ice_crystal`, `large_ice_crystal`, `glowstone_crystal`, `hydrothermal_rock`, `pyrolusite_ore`, `deepslate_pyrolusite_ore`, `glowstone_shard`, and `raw_pyrolusite`
- Primary code anchors: `FHBlocks.ICE_CRYSTAL`, `FHBlocks.LARGE_ICE_CRYSTAL`, `FHBlocks.GLOWSTONE_CRYSTAL`, `FHBlocks.HYDROTHERMAL_ROCK`, `FHBlocks.PYROLUSITE_ORE`, `FHBlocks.DEEPSLATE_PYROLUSITE_ORE`, `FHItems.GLOWSTONE_SHARD`, `FHItems.RAW_PYROLUSITE`, `Scripts/ice_crystal_assets.py`

## Block and Drop Behavior

`ice_crystal` uses `AmethystClusterBlock(7, 3, properties)` with source light level **1**; `large_ice_crystal` uses `AmethystClusterBlock(14, 1, properties)` with source light level **3**. Both use six attachment directions, vanilla waterlogging and sturdy support checks. Losing support breaks the cluster. Both loot tables are Silk Touch only; without Silk Touch they drop nothing.

`glowstone_crystal` is a full cube rendered with the translucent layer, uses a pale-yellow block and item tint (`0xE9D78A`), has source light level **12**, and drops two `glowstone_shard` items with or without Silk Touch. Glowstone shards intentionally have no fluorite, glowstone-dust, or other processing tags. They are raw inputs for a later processing stage.

`pyrolusite_ore` and `deepslate_pyrolusite_ore` require a stone-tier pickaxe and drop `raw_pyrolusite`, with the standard ore Fortune count modifier; Silk Touch preserves the ore block. The default `DropExperienceBlock` constructor adds no mining experience. They carry Forge ore tags for pyrolusite and their respective stone/deepslate host. `raw_pyrolusite` is tagged as a Forge raw material but has no first-edition processing recipe.

`hydrothermal_rock` is a decorative full cube for spring-floor traces. It has no heat, fluid, or player-temperature behavior; its model combines dark smooth basalt with small calcite deposits on the top face.

## Assets and Parameters

Block/item resources are authored under `src/main/resources/assets/frostedheart/`. The two ice crystals use clipped, center-split crossed planes with `minecraft:translucent`. Their independent 16×16 textures are `small_ice_crystal.png` and `ice_crystal.png`: visible pixels have Alpha=204 (80% opacity), empty pixels Alpha=0. The small sprite is separately drawn rather than scaled from the large one. Both use the same pixel scale; visible heights are 7/16 and 15/16 blocks, distinct from the collision heights 7/16 and 14/16. The reproducible pixel and model source is [ice_crystal_assets.py](../../Scripts/ice_crystal_assets.py); no runtime renderer is added.

The glowstone variant still uses the existing FrostedHeart ice texture, tinted at render time. Pyrolusite ore reuses the existing lead-ore visuals for a provisional dark-metal appearance, and the raw item reuses the vanilla coal sprite.

Light values are Minecraft block-light levels in the range 0–15. Mining levels, strength, experience and the shard drop count follow the source properties in `FHBlocks`; none have separate runtime configuration.

Worldgen placement, distribution, measurements and client observations are documented in [ice caves](../worldgen/ice-caves.md). This document owns the current block, resource and drop behavior.
