/*
 * Copyright (c) 2024 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.infrastructure.gen;

import com.simibubi.create.AllItems;
import com.teammoeg.frostedheart.*;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.teammoeg.frostedheart.FHTags.forgeItemTag;

/**
 * See CreateRegistrateTags for reference.
 *
 * To use normal data generators with Registrate, you need to add stuff here.
 * Normally, just use the builder to add tags for new blocks or items added.
 * But if wanting to add tags for non-FH mods, vanilla, add them here.
 */
public class FHRegistrateTags {
    public static void addGenerators() {
        FHMain.REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, FHRegistrateTags::genBlockTags);
        FHMain.REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, FHRegistrateTags::genItemTags);
        FHMain.REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, FHRegistrateTags::genFluidTags);
        FHMain.REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, FHRegistrateTags::genEntityTags);
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
        FHTagGen.FHTagsProvider<Block> prov = new FHTagGen.FHTagsProvider<>(provIn, Block::builtInRegistryHolder);

        prov.tag(FHTags.Blocks.TOWN_DECORATIONS.tag).add(Blocks.FLOWER_POT, Blocks.LANTERN, Blocks.SOUL_LANTERN,
                Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL,
                Blocks.DAMAGED_ANVIL, Blocks.STONECUTTER, Blocks.GRINDSTONE, Blocks.BELL, Blocks.LOOM, Blocks.SMITHING_TABLE);

        prov.tag(FHTags.Blocks.TOWN_WALLS.tag)
                .add(Blocks.IRON_BARS)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.WALLS)
                .addTag(Tags.Blocks.FENCES)
                .addTag(Tags.Blocks.FENCE_GATES)
                .addTag(Tags.Blocks.GLASS_PANES);

        prov.tag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.CONDENSED_ORES.tag);

        prov.tag(FHTags.Blocks.SOIL.tag)
                .add(Blocks.DIRT)
                .add(Blocks.GRASS_BLOCK)
                .add(Blocks.COARSE_DIRT)
                .add(Blocks.PODZOL)
                .add(Blocks.MYCELIUM)
                .add(Blocks.ROOTED_DIRT)
                .add(Blocks.MUD)
                .add(Blocks.GRAVEL)
                .add(Blocks.SAND)
                .add(Blocks.RED_SAND)
                .add(Blocks.CLAY);

        prov.tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES)
                .addTag(FHTags.Blocks.PERMAFROST.tag)
                .addTag(FHTags.Blocks.CONDENSED_ORES.tag)
                .addTag(FHTags.Blocks.SOIL.tag);

        prov.tag(BlockTags.MINEABLE_WITH_AXE)
                .addTag(FHTags.Blocks.TOWN_BLOCKS.tag)
                .addTag(FHTags.Blocks.WOODEN_MACHINES.tag);

        prov.tag(BlockTags.MINEABLE_WITH_HOE);

        prov.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addTag(FHTags.Blocks.METAL_MACHINES.tag);

        prov.tag(BlockTags.NEEDS_STONE_TOOL)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        prov.tag(BlockTags.MINEABLE_WITH_SHOVEL)
                .addTag(FHTags.Blocks.CONDENSED_ORES.tag)
                .addTag(FHTags.Blocks.SLUDGE.tag)
                .addTag(FHTags.Blocks.SOIL.tag);

        prov.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        prov.tag(BlockTags.ANIMALS_SPAWNABLE_ON)
                .addTag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.SOIL.tag)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        prov.tag(FHTags.Blocks.SNOW_MOVEMENT.tag)
                .addTag(BlockTags.SNOW);

        prov.tag(FHTags.Blocks.ICE_MOVEMENT.tag)
                .addTag(BlockTags.ICE);

        prov.tag(BlockTags.RABBITS_SPAWNABLE_ON)
                .addTag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.SOIL.tag)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        prov.tag(BlockTags.FOXES_SPAWNABLE_ON)
                .addTag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.SOIL.tag)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        prov.tag(BlockTags.WOLVES_SPAWNABLE_ON)
                .addTag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.SOIL.tag)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        prov.tag(BlockTags.GOATS_SPAWNABLE_ON)
                .addTag(BlockTags.SNOW)
                .addTag(FHTags.Blocks.SOIL.tag)
                .addTag(FHTags.Blocks.PERMAFROST.tag);

        for (FHTags.Blocks tag : FHTags.Blocks.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }

    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        FHTagGen.FHTagsProvider<Item> prov = new FHTagGen.FHTagsProvider<>(provIn, Item::builtInRegistryHolder);

        prov.tag(FHTags.Items.RAW_FOOD.tag).add(Items.CHICKEN, Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.RABBIT, Items.COD, Items.SALMON);

        prov.tag(FHTags.Items.IGNITION_MATERIAL.tag)
                .add(Items.FLINT);

        prov.tag(FHTags.Items.IGNITION_METAL.tag)
                .add(Items.IRON_INGOT)
                .add(Items.IRON_NUGGET);

        prov.tag(FHTags.Items.REFUGEE_NEEDS.tag)
                .add(FHItems.military_rations.get())
                .add(FHItems.compressed_biscuits_pack.get())
                .add(FHItems.compressed_biscuits.get())
                .add(FHItems.packed_nuts.get())
                .add(FHItems.dried_vegetables.get())
                .add(FHItems.chocolate.get())
                .add(FHItems.black_bread.get())
                .add(FHItems.rye_bread.get())
                .add(FHItems.rye_porridge.get())
                .add(FHItems.rye_sawdust_porridge.get())
                .add(FHItems.vegetable_sawdust_soup.get())
                .add(FHItems.vegetable_soup.get())
                .add(Items.COOKED_PORKCHOP)
                .add(Items.COOKED_BEEF)
                .add(Items.COOKED_CHICKEN)
                .add(Items.COOKED_COD)
                .add(Items.COOKED_SALMON)
                .add(Items.COOKED_RABBIT)
                .add(Items.COOKED_MUTTON)
                .add(Items.BAKED_POTATO)
                .add(Items.BREAD)
                .add(Items.APPLE)
                .add(Items.BEETROOT_SOUP)
                .add(Items.MUSHROOM_STEW)
                .add(Items.RABBIT_STEW)
                .add(Items.PUMPKIN_PIE);

        prov.tag(FHTags.Items.DRY_FOOD.tag)
                .add(FHItems.compressed_biscuits_pack.get())
                .add(FHItems.compressed_biscuits.get())
                .add(FHItems.packed_nuts.get())
                .add(FHItems.dried_vegetables.get())
                .add(FHItems.chocolate.get());

        prov.tag(FHTags.Items.INSULATED_FOOD.tag)
                .add(FHItems.military_rations.get());

        prov.tag(forgeItemTag("crushed_raw_materials/copper"))
                .add(AllItems.CRUSHED_COPPER.asItem());
        prov.tag(forgeItemTag("crushed_raw_materials/iron"))
                .add(AllItems.CRUSHED_IRON.asItem());
        prov.tag(forgeItemTag("crushed_raw_materials/gold"))
                .add(AllItems.CRUSHED_GOLD.asItem());
        prov.tag(forgeItemTag("crushed_raw_materials/zinc"))
                .add(AllItems.CRUSHED_ZINC.asItem());

        // rewrite using prov method
        prov.tag(FHTags.Items.CHICKEN_FEED.tag)
                .addTag(Tags.Items.SEEDS);

        for (FHTags.Items tag : FHTags.Items.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }
    }

    private static void genFluidTags(RegistrateTagsProvider<Fluid> provIn) {
        FHTagGen.FHTagsProvider<Fluid> prov = new FHTagGen.FHTagsProvider<>(provIn, Fluid::builtInRegistryHolder);

        prov.tag(Tags.Fluids.GASEOUS)
                .add(FHFluids.STEAM.get());

        for (FHTags.Fluids tag : FHTags.Fluids.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }

    }

    private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provIn) {
        FHTagGen.FHTagsProvider<EntityType<?>> prov = new FHTagGen.FHTagsProvider<>(provIn, EntityType::builtInRegistryHolder);

        prov.tag(Tags.EntityTypes.BOSSES)
                .add(FHEntityTypes.CURIOSITY.get());

        for (FHTags.FHEntityTags tag : FHTags.FHEntityTags.values()) {
            if (tag.alwaysDatagen) {
                prov.getOrCreateRawBuilder(tag.tag);
            }
        }

    }

    /*
    Add block resource keys to tag appender
     */
    @SafeVarargs
    private void adds(TagsProvider.TagAppender<Block> ta, ResourceKey<? extends Block>... keys) {
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

}
