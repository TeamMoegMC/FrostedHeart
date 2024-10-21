# Read ../src/main/resources/data/forestedheart/worldgen/terralith_overworld.json
# save as dict

import json
import os

# print current dir

print()

with open("/Users/wyc/Development/FrostedHeart-World/src/main/resources/data/frostedheart/worldgen/world_preset/terralith_overworld.json") as f:
    data = json.load(f)

biomes = data["dimensions"]["minecraft:overworld"]["generator"]["biome_source"]["biomes"]
print(len(biomes))

non_snowy_biome_ids = [
    "minecraft:stony_shore",
    "minecraft:river",
    "minecraft:ocean",
    "minecraft:deep_ocean",
    "minecraft:cold_ocean",
    "minecraft:deep_cold_ocean",
    "minecraft:old_growth_pine_taiga",
    "minecraft:old_growth_spruce_taiga",
    "terralith:alpine_highlands",
    "terralith:basalt_cliffs",
    "terralith:birch_taiga",
    "terralith:forested_highlands",
    "terralith:granite_cliffs",
    "terralith:gravel_desert",
    "terralith:white_cliffs",
    "terralith:valley_clearing",
    "terralith:siberian_taiga",
]

winter_biome_ids = [
    "minecraft:frozen_ocean",
    "minecraft:deep_frozen_ocean",
    "minecraft:jagged_peaks",
    "minecraft:frozen_peaks",
    "minecraft:grove",
    "minecraft:snowy_slopes",
    "minecraft:snowy_taiga",
    "minecraft:frozen_river",
    "minecraft:snowy_beach",
    "minecraft:snowy_plains",
    "minecraft:ice_spikes",
    "minecraft:dripstone_caves",
    "minecraft:lush_caves",
    "terralith:alpine_grove",
    "terralith:caldera",
    "terralith:cold_shrubland", # not enough snow
    "terralith:frozen_cliffs",
    "terralith:glacial_chasm",
    "terralith:ice_marsh",
    "terralith:rocky_mountains",
    "terralith:rocky_shrubland",
    "terralith:scarlet_mountains",
    "terralith:siberian_grove",
    "terralith:snowy_badlands",
    "terralith:snowy_maple_forest",
    "terralith:snowy_shield",
    "terralith:volcanic_crater",
    "terralith:volcanic_peaks",
    "terralith:wintry_forest",
    "terralith:wintry_lowlands",
    "terralith:yellowstone",
    "terralith:cave/andesite_caves",
    "terralith:cave/desert_caves",
    "terralith:cave/diorite_caves",
    "terralith:cave/fungal_caves",
    "terralith:cave/granite_caves",
    "terralith:cave/ice_caves",
    "terralith:cave/infested_caves",
    "terralith:cave/thermal_caves",
    "terralith:cave/underground_jungle",
    "terralith:cave/crystal_caves",
    "terralith:cave/deep_caves",
    "terralith:cave/frostfire_caves",
    "terralith:cave/mantle_caves",
    "terralith:cave/tuff_caves"
]

winter_biomes = []
repetitions = {}

for biome in biomes:
    if biome["biome"] in winter_biome_ids:
        winter_biomes.append(biome)
        if biome["biome"] not in repetitions:
            repetitions[biome["biome"]] = 1
        else:
            repetitions[biome["biome"]] += 1

print(len(winter_biomes))
# repetitions = dict(sorted(repetitions.items(), key=lambda item: item[1], reverse=True))
# print(repetitions)

# list the biome ids with temperature from high to low

biome_temperatures_high = {}
biome_temperatures_low = {}
for biome in winter_biomes:
    biome_temperatures_high[biome["biome"]] = biome["parameters"]["temperature"][1]
    biome_temperatures_low[biome["biome"]] = biome["parameters"]["temperature"][0]

biome_temperatures_high = dict(sorted(biome_temperatures_high.items(), key=lambda item: item[1], reverse=True))
biome_temperatures_low = dict(sorted(biome_temperatures_low.items(), key=lambda item: item[1], reverse=True))
print(biome_temperatures_high)
print(biome_temperatures_low)

# save winter biomes as json, with indent=2
data["dimensions"]["minecraft:overworld"]["generator"]["biome_source"]["biomes"] = winter_biomes
with open("winter_biomes.json", "w") as f:
    json.dump(data, f, indent=2)