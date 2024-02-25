package com.teammoeg.frostedheart.data;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHTags;
import net.minecraft.block.Blocks;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.nio.file.Path;

public class FHBlockTagProvider extends BlockTagsProvider {
    public FHBlockTagProvider(DataGenerator gen, ExistingFileHelper existingFileHelper)
    {
        super(gen, FHMain.MODID, existingFileHelper);
    }

    @Override
    protected void registerTags() {
        FHMain.LOGGER.info("running FHBlockTagProvider.registerTags");//test
        this.getOrCreateBuilder(FHTags.Blocks.DECORATIONS).add(Blocks.FLOWER_POT, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.ENCHANTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL, Blocks.STONECUTTER, Blocks.GRINDSTONE);
    }

    @Override
    protected Path makePath(ResourceLocation id) {
        return this.generator.getOutputFolder()
                .resolve("data/" + id.getNamespace() + "/tags/blocks/" + id.getPath() + ".json");
    }

    @Override
    public String getName() {
        return FHMain.MODID + " block tags";
    }


}
