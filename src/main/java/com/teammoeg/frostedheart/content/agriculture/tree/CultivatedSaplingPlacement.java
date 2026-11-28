package com.teammoeg.frostedheart.content.agriculture.tree;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Vanilla sapling items place the internal cultivation blocks; no extra item or manual item consumption. */
@Mod.EventBusSubscriber(modid = FHMain.MODID)
public final class CultivatedSaplingPlacement {
    private CultivatedSaplingPlacement() {}

    /** Late placement conversion respects already-cancelled events and vanilla snapshot rollback. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (var snapshot : multi.getReplacedBlockSnapshots()) convert(level, snapshot.getPos());
        } else convert(level, event.getPos());
    }

    private static void convert(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        CultivatedOakBlock target = cultivatedFor(state.getBlock());
        if (target == null) return;
        BlockState planted = target.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED))
            planted = planted.setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED));
        level.setBlock(pos, planted, Block.UPDATE_ALL);
    }

    /** Exact vanilla block identities only; natural world placement without this event is unchanged. */
    public static CultivatedOakBlock cultivatedFor(Block block) {
        if (block == Blocks.OAK_SAPLING) return FHBlocks.CULTIVATED_OAK.get();
        if (block == Blocks.SPRUCE_SAPLING) return FHBlocks.CULTIVATED_SPRUCE.get();
        if (block == Blocks.BIRCH_SAPLING) return FHBlocks.CULTIVATED_BIRCH.get();
        if (block == Blocks.JUNGLE_SAPLING) return FHBlocks.CULTIVATED_JUNGLE.get();
        if (block == Blocks.ACACIA_SAPLING) return FHBlocks.CULTIVATED_ACACIA.get();
        if (block == Blocks.DARK_OAK_SAPLING) return FHBlocks.CULTIVATED_DARK_OAK.get();
        if (block == Blocks.MANGROVE_PROPAGULE) return FHBlocks.CULTIVATED_MANGROVE.get();
        if (block == Blocks.CHERRY_SAPLING) return FHBlocks.CULTIVATED_CHERRY.get();
        return null;
    }

    /** Shared pick-block identity; dropped items are declared independently in loot data. */
    public static Block originalFor(CultivatedOakBlock block) {
        return switch (block.profileId().getPath()) {
            case "spruce" -> Blocks.SPRUCE_SAPLING;
            case "birch" -> Blocks.BIRCH_SAPLING;
            case "jungle" -> Blocks.JUNGLE_SAPLING;
            case "acacia" -> Blocks.ACACIA_SAPLING;
            case "dark_oak" -> Blocks.DARK_OAK_SAPLING;
            case "mangrove" -> Blocks.MANGROVE_PROPAGULE;
            case "cherry" -> Blocks.CHERRY_SAPLING;
            default -> Blocks.OAK_SAPLING;
        };
    }
}
