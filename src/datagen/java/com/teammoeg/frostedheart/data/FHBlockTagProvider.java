package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHBlocks;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.concurrent.CompletableFuture;

public class FHBlockTagProvider extends TagsProvider<Block> {
    public FHBlockTagProvider(DataGenerator dataGenerator, String modId, ExistingFileHelper existingFileHelper, CompletableFuture<HolderLookup.Provider> provider) {
        super(dataGenerator.getPackOutput(), Registries.BLOCK,provider,modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        // FH deco and walls
//        TagAppender<Block> deco = tag(FHTags.Blocks.TOWN_DECORATIONS);
//        deco.add(rk(Blocks.FLOWER_POT), rk(Blocks.LANTERN), rk(Blocks.SOUL_LANTERN), rk(Blocks.CAMPFIRE), rk(Blocks.SOUL_CAMPFIRE), rk(Blocks.ENCHANTING_TABLE), rk(Blocks.ANVIL), rk(Blocks.CHIPPED_ANVIL), rk(Blocks.DAMAGED_ANVIL), rk(Blocks.STONECUTTER), rk(Blocks.GRINDSTONE));
//        TagAppender<Block> wall = tag(FHTags.Blocks.TOWN_WALLS);
//        wall.addTag(BlockTags.DOORS).addTag(BlockTags.WALLS).addTag(Tags.Blocks.FENCES).addTag(Tags.Blocks.FENCE_GATES).addTag(Tags.Blocks.GLASS_PANES).add(rk(Blocks.IRON_BARS));

        // Vanilla ores
        TagAppender<Block> ores = tag(Tags.Blocks.ORES);
        ores.add(
                rk(FHBlocks.SILVER_ORE),
                rk(FHBlocks.TIN_ORE),
                rk(FHBlocks.PYRITE_ORE),
                rk(FHBlocks.NICKEL_ORE),
                rk(FHBlocks.LEAD_ORE),
                rk(FHBlocks.HALITE_ORE),
                rk(FHBlocks.SYLVITE_ORE),
                rk(FHBlocks.MAGNESITE_ORE),
                rk(FHBlocks.DEEPSLATE_SILVER_ORE),
                rk(FHBlocks.DEEPSLATE_TIN_ORE),
                rk(FHBlocks.DEEPSLATE_PYRITE_ORE),
                rk(FHBlocks.DEEPSLATE_NICKEL_ORE),
                rk(FHBlocks.DEEPSLATE_LEAD_ORE),
                rk(FHBlocks.DEEPSLATE_HALITE_ORE),
                rk(FHBlocks.DEEPSLATE_SYLVITE_ORE),
                rk(FHBlocks.DEEPSLATE_MAGNESITE_ORE),
                rk(FHBlocks.PEAT),
                rk(FHBlocks.KAOLIN),
                rk(FHBlocks.BAUXITE)
                );

        // Forge ores
        tag("forge:ores/silver")
                .add(rk(FHBlocks.SILVER_ORE))
                .add(rk(FHBlocks.DEEPSLATE_SILVER_ORE));
        tag("forge:ores/tin")
                .add(rk(FHBlocks.TIN_ORE))
                .add(rk(FHBlocks.DEEPSLATE_TIN_ORE));
        tag("forge:ores/iron")
                .add(rk(FHBlocks.PYRITE_ORE))
                .add(rk(FHBlocks.DEEPSLATE_PYRITE_ORE));
        tag("forge:ores/nickel")
                .add(rk(FHBlocks.NICKEL_ORE))
                .add(rk(FHBlocks.DEEPSLATE_NICKEL_ORE));
        tag("forge:ores/lead")
                .add(rk(FHBlocks.LEAD_ORE))
                .add(rk(FHBlocks.DEEPSLATE_LEAD_ORE));
        tag("forge:ores/salt")
                .add(rk(FHBlocks.HALITE_ORE))
                .add(rk(FHBlocks.DEEPSLATE_HALITE_ORE));
        tag("forge:ores/sylvite")
                .add(rk(FHBlocks.SYLVITE_ORE))
                .add(rk(FHBlocks.DEEPSLATE_SYLVITE_ORE));
        tag("forge:ores/peat")
                .add(rk(FHBlocks.PEAT))
                .add(rk(FHBlocks.PEAT_PERMAFROST));
        tag("forge:ores/kaolin")
                .add(rk(FHBlocks.KAOLIN))
                .add(rk(FHBlocks.KAOLIN_PERMAFROST));
        tag("forge:ores/aluminum")
                .add(rk(FHBlocks.BAUXITE))
                .add(rk(FHBlocks.BAUXITE_PERMAFROST));

        // Forge blocks
        tag("forge:storage_blocks/aluminum")
                .add(rk(FHBlocks.ALUMINUM_BLOCK));
        tag("forge:storage_blocks/steel")
                .add(rk(FHBlocks.STEEL_BLOCK));
        tag("forge:storage_blocks/electrum")
                .add(rk(FHBlocks.ELECTRUM_BLOCK));
        tag("forge:storage_blocks/constantan")
                .add(rk(FHBlocks.CONSTANTAN_BLOCK));
        tag("forge:storage_blocks/cast_iron")
                .add(rk(FHBlocks.CAST_IRON_BLOCK));
        tag("forge:storage_blocks/duralumin")
                .add(rk(FHBlocks.DURALUMIN_BLOCK));
        tag("forge:storage_blocks/silver")
                .add(rk(FHBlocks.SILVER_BLOCK));
        tag("forge:storage_blocks/nickel")
                .add(rk(FHBlocks.NICKEL_BLOCK));
        tag("forge:storage_blocks/lead")
                .add(rk(FHBlocks.LEAD_BLOCK));
        tag("forge:storage_blocks/titanium")
                .add(rk(FHBlocks.TITANIUM_BLOCK));
        tag("forge:storage_blocks/bronze")
                .add(rk(FHBlocks.BRONZE_BLOCK));
        tag("forge:storage_blocks/invar")
                .add(rk(FHBlocks.INVAR_BLOCK));
        tag("forge:storage_blocks/tungstensteel")
                .add(rk(FHBlocks.TUNGSTEN_STEEL_BLOCK));
        tag("forge:storage_blocks/tin")
                .add(rk(FHBlocks.TIN_BLOCK));
        tag("forge:storage_blocks/magnesium")
                .add(rk(FHBlocks.MAGNESIUM_BLOCK));
        tag("forge:storage_blocks/tungsten")
                .add(rk(FHBlocks.TUNGSTEN_BLOCK));

        // Snow condensed ores
//        tag(FHTags.Blocks.CONDENSED_ORES)
//                .add(rk(FHBlocks.CONDENSED_TIN_ORE))
//                .add(rk(FHBlocks.CONDENSED_NICKEL_ORE))
//                .add(rk(FHBlocks.CONDENSED_LEAD_ORE))
//                .add(rk(FHBlocks.CONDENSED_COPPER_ORE))
//                .add(rk(FHBlocks.CONDENSED_IRON_ORE))
//                .add(rk(FHBlocks.CONDENSED_SILVER_ORE))
//                .add(rk(FHBlocks.CONDENSED_GOLD_ORE))
//                .add(rk(FHBlocks.CONDENSED_ZINC_ORE))
//                .add(rk(FHBlocks.CONDENSED_PYRITE_ORE))
//                .add(rk(FHBlocks.CONDENSED_TIN_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_NICKEL_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_LEAD_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_COPPER_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_IRON_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_SILVER_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_GOLD_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_ZINC_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_PYRITE_ORE_BLOCK));

        // Permafrost
        tag(FHTags.Blocks.PERMAFROST)
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .add(rk(FHBlocks.ROOTED_DIRT_PERMAFROST))
                .add(rk(FHBlocks.COARSE_DIRT_PERMAFROST))
                .add(rk(FHBlocks.MUD_PERMAFROST))
                .add(rk(FHBlocks.GRAVEL_PERMAFROST))
                .add(rk(FHBlocks.SAND_PERMAFROST))
                .add(rk(FHBlocks.RED_SAND_PERMAFROST))
                .add(rk(FHBlocks.CLAY_PERMAFROST))
                .add(rk(FHBlocks.PEAT_PERMAFROST))
                .add(rk(FHBlocks.ROTTEN_WOOD_PERMAFROST))
                .add(rk(FHBlocks.BAUXITE_PERMAFROST))
                .add(rk(FHBlocks.KAOLIN_PERMAFROST));

        tag(FHTags.Blocks.SOIL)
                .add(rk(Blocks.DIRT))
                .add(rk(Blocks.GRASS_BLOCK))
                .add(rk(Blocks.COARSE_DIRT))
                .add(rk(Blocks.PODZOL))
                .add(rk(FHBlocks.BURIED_PODZOL))
                .add(rk(Blocks.MYCELIUM))
                .add(rk(FHBlocks.BURIED_MYCELIUM))
                .add(rk(Blocks.ROOTED_DIRT))
                .add(rk(Blocks.MUD))
                .add(rk(Blocks.GRAVEL))
                .add(rk(Blocks.SAND))
                .add(rk(Blocks.RED_SAND))
                .add(rk(Blocks.CLAY))
                .add(rk(FHBlocks.PEAT))
                .add(rk(FHBlocks.KAOLIN))
                .add(rk(FHBlocks.BAUXITE))
                .add(rk(FHBlocks.ROTTEN_WOOD));


        // Town blocks
        tag(FHTags.Blocks.TOWN_BLOCKS)
                .add(rk(FHBlocks.HOUSE))
                .add(rk(FHBlocks.WAREHOUSE))
                .add(rk(FHBlocks.MINE))
                .add(rk(FHBlocks.MINE_BASE))
                .add(rk(FHBlocks.HUNTING_BASE))
                .add(rk(FHBlocks.HUNTING_CAMP));

        // Metal machines
        tag(FHTags.Blocks.METAL_MACHINES)
                .add(rk(FHBlocks.RELIC_CHEST))
                .add(rk(FHBlocks.HEAT_INCUBATOR))
                .add(rk(FHBlocks.DEBUG_HEATER))
                .add(rk(FHBlocks.CHARGER))
                .add(rk(FHBlocks.OIL_BURNER))
                .add(rk(FHBlocks.GAS_VENT))
                .add(rk(FHBlocks.SMOKE_BLOCK_T1))
                .add(rk(FHBlocks.MECHANICAL_CALCULATOR))
                .add(rk(FHBlocks.SAUNA_VENT))
                .add(rk(FHBlocks.FOUNTAIN_BASE))
                .add(rk(FHBlocks.FOUNTAIN_NOZZLE));

        // Wood machines
        tag(FHTags.Blocks.WOODEN_MACHINES)
                .add(rk(FHBlocks.HEAT_PIPE))
                .add(rk(FHBlocks.DRAWING_DESK))
                .add(rk(FHBlocks.INCUBATOR));

        // carver
        tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES)
                .addTag(FHTags.Blocks.PERMAFROST)
                .addTag(FHTags.Blocks.CONDENSED_ORES)
                .addTag(Tags.Blocks.ORES)
                .add(rk(FHBlocks.BURIED_MYCELIUM))
                .add(rk(FHBlocks.BURIED_PODZOL));

        // snow
        tag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.CONDENSED_ORES);

        // crops
        tag(BlockTags.CROPS)
                .add(rk(FHBlocks.RYE_BLOCK))
                .add(rk(FHBlocks.WHITE_TURNIP_BLOCK))
                .add(rk(FHBlocks.WOLFBERRY_BUSH_BLOCK));

        // beacon
        tag(BlockTags.BEACON_BASE_BLOCKS)
                .add(rk(FHBlocks.ALUMINUM_BLOCK))
                .add(rk(FHBlocks.STEEL_BLOCK))
                .add(rk(FHBlocks.ELECTRUM_BLOCK))
                .add(rk(FHBlocks.CONSTANTAN_BLOCK))
                .add(rk(FHBlocks.CAST_IRON_BLOCK))
                .add(rk(FHBlocks.DURALUMIN_BLOCK))
                .add(rk(FHBlocks.SILVER_BLOCK))
                .add(rk(FHBlocks.NICKEL_BLOCK))
                .add(rk(FHBlocks.LEAD_BLOCK))
                .add(rk(FHBlocks.TITANIUM_BLOCK))
                .add(rk(FHBlocks.BRONZE_BLOCK))
                .add(rk(FHBlocks.INVAR_BLOCK))
                .add(rk(FHBlocks.TUNGSTEN_STEEL_BLOCK))
                .add(rk(FHBlocks.TIN_BLOCK))
                .add(rk(FHBlocks.MAGNESIUM_BLOCK))
                .add(rk(FHBlocks.TUNGSTEN_BLOCK));

        // mushroom grow
        tag(BlockTags.MUSHROOM_GROW_BLOCK)
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.ROTTEN_WOOD))
                .add(rk(FHBlocks.ROTTEN_WOOD_PERMAFROST));

        // Mining tool
        tag(BlockTags.MINEABLE_WITH_AXE)
                .addTag(FHTags.Blocks.TOWN_BLOCKS)
                .addTag(FHTags.Blocks.WOODEN_MACHINES)
                .add(rk(FHBlocks.ROTTEN_WOOD))
                .add(rk(FHBlocks.ROTTEN_WOOD_PERMAFROST))
                // deco
                .add(rk(FHBlocks.WOODEN_BOX))
                .add(rk(FHBlocks.SMALL_GARAGE))
                .add(rk(FHBlocks.PACKAGE_BLOCK));

        tag(BlockTags.MINEABLE_WITH_HOE);

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addTag(FHTags.Blocks.METAL_MACHINES)
                // ores
                .add(rk(FHBlocks.SILVER_ORE))
                .add(rk(FHBlocks.TIN_ORE))
                .add(rk(FHBlocks.PYRITE_ORE))
                .add(rk(FHBlocks.NICKEL_ORE))
                .add(rk(FHBlocks.LEAD_ORE))
                .add(rk(FHBlocks.HALITE_ORE))
                .add(rk(FHBlocks.SYLVITE_ORE))
                .add(rk(FHBlocks.MAGNESITE_ORE))
                // deepslate
                .add(rk(FHBlocks.DEEPSLATE_SILVER_ORE))
                .add(rk(FHBlocks.DEEPSLATE_TIN_ORE))
                .add(rk(FHBlocks.DEEPSLATE_PYRITE_ORE))
                .add(rk(FHBlocks.DEEPSLATE_NICKEL_ORE))
                .add(rk(FHBlocks.DEEPSLATE_LEAD_ORE))
                .add(rk(FHBlocks.DEEPSLATE_HALITE_ORE))
                .add(rk(FHBlocks.DEEPSLATE_SYLVITE_ORE))
                .add(rk(FHBlocks.DEEPSLATE_MAGNESITE_ORE))
                // storage blocks
                .add(rk(FHBlocks.ALUMINUM_BLOCK))
                .add(rk(FHBlocks.STEEL_BLOCK))
                .add(rk(FHBlocks.ELECTRUM_BLOCK))
                .add(rk(FHBlocks.CONSTANTAN_BLOCK))
                .add(rk(FHBlocks.CAST_IRON_BLOCK))
                .add(rk(FHBlocks.DURALUMIN_BLOCK))
                .add(rk(FHBlocks.SILVER_BLOCK))
                .add(rk(FHBlocks.NICKEL_BLOCK))
                .add(rk(FHBlocks.LEAD_BLOCK))
                .add(rk(FHBlocks.TITANIUM_BLOCK))
                .add(rk(FHBlocks.BRONZE_BLOCK))
                .add(rk(FHBlocks.INVAR_BLOCK))
                .add(rk(FHBlocks.TUNGSTEN_STEEL_BLOCK))
                .add(rk(FHBlocks.TIN_BLOCK))
                .add(rk(FHBlocks.MAGNESIUM_BLOCK))
                .add(rk(FHBlocks.TUNGSTEN_BLOCK))
                // misc blocks
                .add(rk(FHBlocks.QUICKLIME_BLOCK))
                .add(rk(FHBlocks.REFRACTORY_BRICKS))
                .add(rk(FHBlocks.HIGH_REFRACTORY_BRICKS))
                .add(rk(FHBlocks.MAGNESITE_BLOCK))
                .add(rk(FHBlocks.MAGNESIA_BLOCK))
                // building
                .add(rk(FHBlocks.GENERATOR_CORE_T1))
                .add(rk(FHBlocks.GENERATOR_BRICK))
                .add(rk(FHBlocks.GENERATOR_AMPLIFIER_T1))
                .add(rk(FHBlocks.DURALUMIN_SHEETMETAL))
                // deco
                .add(rk(FHBlocks.PEBBLE_BLOCK))
                .add(rk(FHBlocks.ODD_MARK))
                .add(rk(FHBlocks.MAKESHIFT_GENERATOR_BROKEN))
                .add(rk(FHBlocks.BROKEN_PLATE));

        tag(BlockTags.MINEABLE_WITH_SHOVEL)
                // condensed snow ores
                .add(rk(FHBlocks.CONDENSED_TIN_ORE))
                .add(rk(FHBlocks.CONDENSED_NICKEL_ORE))
                .add(rk(FHBlocks.CONDENSED_LEAD_ORE))
                .add(rk(FHBlocks.CONDENSED_COPPER_ORE))
//                .add(rk(FHBlocks.CONDENSED_IRON_ORE))
                .add(rk(FHBlocks.CONDENSED_SILVER_ORE))
                .add(rk(FHBlocks.CONDENSED_GOLD_ORE))
                .add(rk(FHBlocks.CONDENSED_ZINC_ORE))
                .add(rk(FHBlocks.CONDENSED_PYRITE_ORE))
                .add(rk(FHBlocks.CONDENSED_TIN_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_NICKEL_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_LEAD_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_COPPER_ORE_BLOCK))
//                .add(rk(FHBlocks.CONDENSED_IRON_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_SILVER_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_GOLD_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_ZINC_ORE_BLOCK))
                .add(rk(FHBlocks.CONDENSED_PYRITE_ORE_BLOCK))
                // slude
                .add(rk(FHBlocks.IRON_SLUDGE))
                .add(rk(FHBlocks.GOLD_SLUDGE))
                .add(rk(FHBlocks.COPPER_SLUDGE))
                .add(rk(FHBlocks.TIN_SLUDGE))
                .add(rk(FHBlocks.LEAD_SLUDGE))
                .add(rk(FHBlocks.SILVER_SLUDGE))
                .add(rk(FHBlocks.NICKEL_SLUDGE))
                .add(rk(FHBlocks.ZINC_SLUDGE))
                .add(rk(FHBlocks.PYRITE_SLUDGE))
                .add(rk(FHBlocks.IRON_SLUDGE_BLOCK))
                .add(rk(FHBlocks.GOLD_SLUDGE_BLOCK))
                .add(rk(FHBlocks.COPPER_SLUDGE_BLOCK))
                .add(rk(FHBlocks.TIN_SLUDGE_BLOCK))
                .add(rk(FHBlocks.LEAD_SLUDGE_BLOCK))
                .add(rk(FHBlocks.SILVER_SLUDGE_BLOCK))
                .add(rk(FHBlocks.NICKEL_SLUDGE_BLOCK))
                .add(rk(FHBlocks.ZINC_SLUDGE_BLOCK))
                .add(rk(FHBlocks.PYRITE_SLUDGE_BLOCK))
                // minerals
                .add(rk(FHBlocks.PEAT))
                .add(rk(FHBlocks.KAOLIN))
                .add(rk(FHBlocks.BAUXITE))
                .add(rk(FHBlocks.BURIED_PODZOL))
                .add(rk(FHBlocks.BURIED_MYCELIUM))
                .add(rk(FHBlocks.COPPER_GRAVEL))
                // permafrost
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .add(rk(FHBlocks.ROOTED_DIRT_PERMAFROST))
                .add(rk(FHBlocks.COARSE_DIRT_PERMAFROST))
                .add(rk(FHBlocks.MUD_PERMAFROST))
                .add(rk(FHBlocks.GRAVEL_PERMAFROST))
                .add(rk(FHBlocks.SAND_PERMAFROST))
                .add(rk(FHBlocks.RED_SAND_PERMAFROST))
                .add(rk(FHBlocks.CLAY_PERMAFROST))
                .add(rk(FHBlocks.PEAT_PERMAFROST))
                .add(rk(FHBlocks.BAUXITE_PERMAFROST))
                .add(rk(FHBlocks.KAOLIN_PERMAFROST))
                // building
                .add(rk(FHBlocks.PACKED_SNOW))
                .add(rk(FHBlocks.PACKED_SNOW_SLAB))
                // deco
                .add(rk(FHBlocks.BLOOD_BLOCK))
                .add(rk(FHBlocks.BONE_BLOCK));

        tag(BlockTags.SWORD_EFFICIENT)
                .add(rk(FHBlocks.WHALE_BLOCK))
                .add(rk(FHBlocks.WHALE_BELLY_BLOCK));

        // Mining level
        tag(BlockTags.NEEDS_DIAMOND_TOOL)
                .add(rk(FHBlocks.TUNGSTEN_STEEL_BLOCK))
                .add(rk(FHBlocks.TUNGSTEN_BLOCK));

        tag(BlockTags.NEEDS_IRON_TOOL)
                .addTag(FHTags.Blocks.PERMAFROST) // permafrost needs iron/bronze level tool!
                .add(rk(FHBlocks.SILVER_ORE))
                .add(rk(FHBlocks.NICKEL_ORE))
                .add(rk(FHBlocks.STEEL_BLOCK))
                .add(rk(FHBlocks.ELECTRUM_BLOCK))
                .add(rk(FHBlocks.CAST_IRON_BLOCK))
                .add(rk(FHBlocks.DURALUMIN_BLOCK))
                .add(rk(FHBlocks.SILVER_BLOCK))
                .add(rk(FHBlocks.NICKEL_BLOCK))
                .add(rk(FHBlocks.TITANIUM_BLOCK))
                .add(rk(FHBlocks.INVAR_BLOCK));

        tag(BlockTags.NEEDS_STONE_TOOL)
                .add(rk(FHBlocks.TIN_ORE))
                .add(rk(FHBlocks.PYRITE_ORE))
                .add(rk(FHBlocks.LEAD_ORE))
                .add(rk(FHBlocks.HALITE_ORE))
                .add(rk(FHBlocks.SYLVITE_ORE))
                .add(rk(FHBlocks.MAGNESITE_ORE))
                .add(rk(FHBlocks.ALUMINUM_BLOCK))
                .add(rk(FHBlocks.CONSTANTAN_BLOCK))
                .add(rk(FHBlocks.LEAD_BLOCK))
                .add(rk(FHBlocks.BRONZE_BLOCK))
                .add(rk(FHBlocks.TIN_BLOCK))
                .add(rk(FHBlocks.MAGNESIUM_BLOCK));

        // normal animals
        tag(BlockTags.ANIMALS_SPAWNABLE_ON)
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .addTag(BlockTags.SNOW); // allow snow spawn

        // cold animals
        tag(BlockTags.RABBITS_SPAWNABLE_ON)
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .addTag(BlockTags.SNOW);

        tag(BlockTags.FOXES_SPAWNABLE_ON)
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .addTag(BlockTags.SNOW);

        tag(BlockTags.WOLVES_SPAWNABLE_ON)
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .addTag(BlockTags.SNOW);

        tag(BlockTags.GOATS_SPAWNABLE_ON)
                .add(rk(FHBlocks.DIRT_PERMAFROST))
                .add(rk(FHBlocks.MYCELIUM_PERMAFROST))
                .add(rk(FHBlocks.PODZOL_PERMAFROST))
                .add(rk(FHBlocks.GRAVEL_PERMAFROST))
                .addTag(BlockTags.SNOW);

        tag(BlockTags.SAND)
                .add(rk(FHBlocks.SAND_PERMAFROST))
                .add(rk(FHBlocks.RED_SAND_PERMAFROST));

        tag(FHTags.Blocks.SNOW_MOVEMENT)
                .add(rk(FHBlocks.PACKED_SNOW))
                .add(rk(FHBlocks.PACKED_SNOW_SLAB))
                .addTag(BlockTags.SNOW);

        tag(FHTags.Blocks.ICE_MOVEMENT)
                .addTag(BlockTags.ICE);

    }

    /*
    Add block resource keys to tag appender
     */
    @SafeVarargs
    private void adds(TagAppender<Block> ta, ResourceKey<? extends Block>... keys) {
        ResourceKey[] rk=keys;
        ta.add(rk);
    }

    /*
    Get resource key for mod block id
     */
    private ResourceKey<Block> cp(String s) {
        return ResourceKey.create(Registries.BLOCK,mrl(s));
    }

    /*
    Get resource key for block
     */
    private ResourceKey<Block> rk(Block  b) {
        return ForgeRegistries.BLOCKS.getResourceKey(b).orElseGet(()->b.builtInRegistryHolder().key());
    }

    private ResourceKey<Block> rk(RegistryObject<Block> it) {
        return rk(it.get());
    }

    /*
    Get tag appender from resource location
     */
    private TagAppender<Block> tag(ResourceLocation s) {
        return this.tag(BlockTags.create(s));
    }

    private TagAppender<Block> tag(String s) {
        return this.tag(BlockTags.create(new ResourceLocation(s)));
    }

    private TagKey<Block> modTag(String s) {
        return BlockTags.create(mrl(s));
    }

    private TagKey<Block> rlTag(ResourceLocation s) {
        return BlockTags.create(s);
    }

    /*
    Get resource location from registry object
     */
    private ResourceLocation rl(RegistryObject<Block> it) {
        return it.getId();
    }

    /*
    Get resource location from string
     */
    private ResourceLocation rl(String r) {
        return new ResourceLocation(r);
    }

    /*
    Get resource location for mod namespace given value
     */
    private ResourceLocation mrl(String s) {
        return new ResourceLocation(FHMain.MODID, s);
    }

    /*
    Get resource location for forge namespace given value
     */
    private ResourceLocation frl(String s) {
        return new ResourceLocation("forge", s);
    }

    /*
    Get resource location for minecraft namespace given value
     */
    private ResourceLocation mcrl(String s) {
        return new ResourceLocation(s);
    }

    @Override
    public String getName() {
        return FHMain.MODID + " block tags";
    }


}
