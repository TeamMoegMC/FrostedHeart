# Warm stone T09 resources and tooltip

- Time: `2026-08-28 22:17:46 +08:00`
- Author: `Codex; OpenAI GPT-5; T09 resources sub-agent`
- Status: `completed`
- Scope: `warm-stone Curios tag, localization, models, pixel textures, slot icon, and read-only ItemStack tooltip`

## Completed

- Added `FHTags.Items.CURIOS_WARM_STONE`, bound both registered reservoirs to it, and generated `data/curios/tags/items/warm_stone.json` with only `frostedheart:warm_stone` and `frostedheart:hot_water_bag`.
- Added `minecraft:item/generated` models, English/Chinese names and tooltip text, two transparent `16x16` item textures, and `textures/slot/empty_warm_stone_slot.png` for the existing Curios resource ID.
- Added `WarmStoneItem.appendHoverText` with surface temperature and `10%/25%` capacity in normal display and the internal temperature only in advanced display.

## Decisions

- Tooltip reads only `WearableThermalState.read`; it never calls `getOrCreateTag`, initializes a missing reservoir, or writes ItemStack NBT on the client.
- A missing thermal state is displayed as uninitialized. The client has no authority to choose its initial temperature.
- The built-in image-generation tool was unavailable. The required small transparent sprites were created deterministically in the repository's existing 16x16 pixel-art style and kept under the referenced workspace paths.

## Validation

- JDK 17 targeted `WarmStoneItemTest` and `WearableThermalStateTest`: `10/10` passed.
- Inspected all three PNGs: `16x16`, `Format32bppArgb`; confirmed item models, Curios icon resource ID, language keys and tag JSON paths.

## Remaining

- T11/T12 still own player/inventory exchange, server-side initialization/writeback cadence, and packet measurement for Gate B. No item/entity runtime behavior was added here.
