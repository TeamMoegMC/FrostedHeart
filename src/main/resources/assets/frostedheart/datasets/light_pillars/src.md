# Light pillar datasets

Tables for the light pillars of the calculated sky (`content/climate/render/sky`, see `docs/climate/calculated-sky.md`). `emitters.csv` is loaded by `LightPillarEmitters` and `passes_light.csv` by `LightPillarOccluders`.

## Files

- `emitters.csv`: colors for lights whose particle texture gives the wrong color. One row per block id, or per block tag written with a leading `#`. Block ids are matched first, then tags in table order. `red`, `green`, and `blue` are a linear color in any scale, normalized to a brightest channel of 1 when loaded. `intensity` multiplies the light's strength, which is otherwise `(emission / 15)^2`, and must be above `0`. Rows for blocks of mods that are not loaded are ignored.
- `passes_light.csv`: blocks that do not shadow the light of a pillar on its way up from the light, although they have collision. One column, `block`, with a block id or a block tag written with a leading `#` per row. Blocks without collision, such as plants, fluids, and torches, let light through without a row. Every other block stops light up to the top of its outline shape. The table lists glass, glass panes, and the clear ice of vanilla and FrostedHeart. Rows for blocks of mods that are not loaded are ignored.

## Source

The colors are the average of a vanilla texture, computed the same way as `LightPillarEmitters.averageTextureColor` (opaque pixels, weighted by the fourth power of their brightest sRGB channel, averaged in linear light):

| Rows | Texture |
|---|---|
| `generator_t1`, `generator_t2`, `furnace` | `block/furnace_front_on` |
| `radiator` | `block/torch` |
| `campfire` | `block/campfire_fire`, first frame |
| `soul_campfire` | `block/soul_campfire_fire`, first frame |
| `blast_furnace` | `block/blast_furnace_front_on`, first frame |
| `smoker` | `block/smoker_front_on`, first frame |
| `#minecraft:candles` | `particle/flame` |

FrostedHeart's generators and radiator emit light from every block while active, and their textures would give white. Campfires would give the color of their logs, the furnaces the color of their stone, and candles the color of their wax.
