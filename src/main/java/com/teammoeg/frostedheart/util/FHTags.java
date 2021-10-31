package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;

public class FHTags {
    public static final class Blocks {
        public static final ITag.INamedTag<Block> ALWAYS_BREAKABLE = create("always_breakable");

        private static ITag.INamedTag<Block> create(String id) {
            return BlockTags.makeWrapperTag(new ResourceLocation(FHMain.MODID, id).toString());
        }
    }
}
