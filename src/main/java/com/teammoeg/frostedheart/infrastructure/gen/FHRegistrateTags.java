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

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * See CreateRegistrateTags
 */
public class FHRegistrateTags {
    public static void addGenerators() {
        FHMain.FH_REGISTRATE.addDataGenerator(ProviderType.BLOCK_TAGS, FHRegistrateTags::genBlockTags);
        FHMain.FH_REGISTRATE.addDataGenerator(ProviderType.ITEM_TAGS, FHRegistrateTags::genItemTags);
        FHMain.FH_REGISTRATE.addDataGenerator(ProviderType.FLUID_TAGS, FHRegistrateTags::genFluidTags);
        FHMain.FH_REGISTRATE.addDataGenerator(ProviderType.ENTITY_TAGS, FHRegistrateTags::genEntityTags);
    }

    private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
        FHTagGen.FHTagsProvider<Block> prov = new FHTagGen.FHTagsProvider<>(provIn, Block::builtInRegistryHolder);

        prov.tag(FHTags.Blocks.TOWN_DECORATIONS).add(Blocks.FLOWER_POT, Blocks.LANTERN, Blocks.SOUL_LANTERN,
                Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL,
                Blocks.DAMAGED_ANVIL, Blocks.STONECUTTER, Blocks.GRINDSTONE, Blocks.BELL, Blocks.LOOM, Blocks.SMITHING_TABLE);

        prov.tag(FHTags.Blocks.TOWN_WALLS)
                .add(Blocks.IRON_BARS)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.WALLS)
                .addTag(Tags.Blocks.FENCES)
                .addTag(Tags.Blocks.FENCE_GATES)
                .addTag(Tags.Blocks.GLASS_PANES);

    }

    private static void genItemTags(RegistrateTagsProvider<Item> provIn) {
        FHTagGen.FHTagsProvider<Item> prov = new FHTagGen.FHTagsProvider<>(provIn, Item::builtInRegistryHolder);
    }

    private static void genFluidTags(RegistrateTagsProvider<Fluid> provIn) {
        FHTagGen.FHTagsProvider<Fluid> prov = new FHTagGen.FHTagsProvider<>(provIn, Fluid::builtInRegistryHolder);
    }

    private static void genEntityTags(RegistrateTagsProvider<EntityType<?>> provIn) {
        FHTagGen.FHTagsProvider<EntityType<?>> prov = new FHTagGen.FHTagsProvider<>(provIn, EntityType::builtInRegistryHolder);
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
