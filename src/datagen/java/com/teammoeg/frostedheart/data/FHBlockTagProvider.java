package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
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
        // this.getOrCreateBuilder(FHTags.Blocks.DECORATIONS).add(Blocks.FLOWER_POT, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.STONECUTTER, Blocks.GRINDSTONE);
        // this.getOrCreateBuilder(FHTags.Blocks.WALL_BLOCKS).addTag(BlockTags.DOORS).addTag(BlockTags.WALLS).addTag(Tags.Blocks.FENCES).addTag(Tags.Blocks.FENCE_GATES).addTag(Tags.Blocks.GLASS_PANES).add(Blocks.IRON_BARS);
    }

    @SafeVarargs
    private void adds(TagAppender<Block> ta, ResourceKey<? extends Block>... keys) {
        ResourceKey[] rk=keys;
        ta.add(rk);
    }
    private TagAppender<Block> tag(String s) {
        return this.tag(BlockTags.create(mrl(s)));
    }

    private ResourceKey<Block> cp(String s) {
        return ResourceKey.create(Registries.BLOCK,mrl(s));
    }
    private ResourceKey<Block> rk(Block  b) {
        return ForgeRegistries.BLOCKS.getResourceKey(b).orElseGet(()->b.builtInRegistryHolder().key());
    }
    private TagAppender<Block> tag(ResourceLocation s) {
        return this.tag(BlockTags.create(s));
    }
    private ResourceLocation rl(RegistryObject<Item> it) {
        return it.getId();
    }

    private ResourceLocation rl(String r) {
        return new ResourceLocation(r);
    }

    private TagKey<Block> otag(String s) {
        return BlockTags.create(mrl(s));
    }

    private TagKey<Item> atag(ResourceLocation s) {
        return ItemTags.create(s);
    }

    private ResourceLocation mrl(String s) {
        return new ResourceLocation(FHMain.MODID, s);
    }

    private ResourceLocation frl(String s) {
        return new ResourceLocation("forge", s);
    }

    private ResourceLocation mcrl(String s) {
        return new ResourceLocation(s);
    }

    @Override
    public String getName() {
        return FHMain.MODID + " block tags";
    }


}
