///*
// * Copyright (c) 2024 TeamMoeg
// *
// * This file is part of Frosted Heart.
// *
// * Frosted Heart is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, version 3.
// *
// * Frosted Heart is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
// *
// */
//
//package com.teammoeg.frostedheart.data;
//
//import com.teammoeg.frostedheart.FHMain;
//import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
//import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
//import com.teammoeg.frostedresearch.FRContents;
//
//import net.minecraft.core.HolderLookup;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.data.DataGenerator;
//import net.minecraft.data.tags.TagsProvider;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.tags.BlockTags;
//import net.minecraft.tags.TagKey;
//import net.minecraft.world.level.block.Block;
//import net.minecraftforge.common.Tags;
//import net.minecraftforge.common.data.ExistingFileHelper;
//import net.minecraftforge.registries.ForgeRegistries;
//import net.minecraftforge.registries.RegistryObject;
//
//import java.util.concurrent.CompletableFuture;
//
//public class FHBlockTagProvider extends TagsProvider<Block> {
//    public FHBlockTagProvider(DataGenerator dataGenerator, String modId, ExistingFileHelper existingFileHelper, CompletableFuture<HolderLookup.Provider> provider) {
//        super(dataGenerator.getPackOutput(), Registries.BLOCK,provider,modId, existingFileHelper);
//    }
//
//    @Override
//    protected void addTags(HolderLookup.Provider pProvider) {
//        // Do NOT add tags here. Use FHRegistrateTags instead, otherwise conflict with Registrate.
//    }
//
//    /*
//    Add block resource keys to tag appender
//     */
//    @SafeVarargs
//    private void adds(TagAppender<Block> ta, ResourceKey<? extends Block>... keys) {
//        ResourceKey[] rk=keys;
//        ta.add(rk);
//    }
//
//    /*
//    Get resource key for mod block id
//     */
//    private ResourceKey<Block> cp(String s) {
//        return ResourceKey.create(Registries.BLOCK,mrl(s));
//    }
//
//    /*
//    Get resource key for block
//     */
//    private ResourceKey<Block> rk(Block  b) {
//        return ForgeRegistries.BLOCKS.getResourceKey(b).orElseGet(()->b.builtInRegistryHolder().key());
//    }
//
//    private ResourceKey<Block> rk(RegistryObject<Block> it) {
//        return rk(it.get());
//    }
//
//    /*
//    Get tag appender from resource location
//     */
//    private TagAppender<Block> tag(ResourceLocation s) {
//        return this.tag(BlockTags.create(s));
//    }
//
//    private TagAppender<Block> tag(String s) {
//        return this.tag(BlockTags.create(new ResourceLocation(s)));
//    }
//
//    private TagKey<Block> modTag(String s) {
//        return BlockTags.create(mrl(s));
//    }
//
//    private TagKey<Block> rlTag(ResourceLocation s) {
//        return BlockTags.create(s);
//    }
//
//    /*
//    Get resource location from registry object
//     */
//    private ResourceLocation rl(RegistryObject<Block> it) {
//        return it.getId();
//    }
//
//    /*
//    Get resource location from string
//     */
//    private ResourceLocation rl(String r) {
//        return new ResourceLocation(r);
//    }
//
//    /*
//    Get resource location for mod namespace given value
//     */
//    private ResourceLocation mrl(String s) {
//        return new ResourceLocation(FHMain.MODID, s);
//    }
//
//    /*
//    Get resource location for forge namespace given value
//     */
//    private ResourceLocation frl(String s) {
//        return new ResourceLocation("forge", s);
//    }
//
//    /*
//    Get resource location for minecraft namespace given value
//     */
//    private ResourceLocation mcrl(String s) {
//        return new ResourceLocation(s);
//    }
//
//    @Override
//    public String getName() {
//        return FHMain.MODID + " block tags";
//    }
//
//
//}
