package com.teammoeg.frostedheart.content.agriculture.tree;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Placement and loot integration checks for the vanilla-item-only cultivation flow. */
@GameTestHolder("frostedheart_cultivated_oak")
@PrefixGameTestTemplate(false)
public class CultivatedSaplingPlacementGameTests {
    private static List<Block> saplings() {
        return List.of(Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING,
                Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING, Blocks.MANGROVE_PROPAGULE, Blocks.CHERRY_SAPLING);
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void vanillaItemsPlaceInternalBlocksAndDropVanillaAgain(GameTestHelper helper) {
        var level = helper.getLevel();var player = helper.makeMockSurvivalPlayer();int i = 0;
        for (Block original : saplings()) {
            BlockPos pos = helper.absolutePos(new BlockPos(3 + i % 4 * 3, 3, 4 + i / 4 * 6));i++;
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            ItemStack stack = new ItemStack(original, 2);player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false)));
            var cultivated = CultivatedSaplingPlacement.cultivatedFor(original);
            helper.assertTrue(result.consumesAction() && level.getBlockState(pos).is(cultivated),
                    "vanilla item placement becomes matching cultivated block");
            helper.assertTrue(stack.getCount() == 1, "vanilla placement alone consumes exactly one item");
            var tag = level.getBlockEntity(pos).saveWithFullMetadata();
            helper.assertTrue(tag.getShort("p") == 0 && tag.getShort("v") == 0, "new planting starts a fresh ledger");
            helper.assertTrue(cultivated.asItem() == Items.AIR && !ForgeRegistries.ITEMS.containsKey(
                    new ResourceLocation("frostedheart", "cultivated_" + cultivated.profileId().getPath() + "_sapling")),
                    "no standalone cultivated item or creative inventory entry");
            helper.assertTrue(cultivated.getCloneItemStack(level, pos, level.getBlockState(pos)).is(original.asItem()),
                    "creative pick block returns vanilla sapling");
            var drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos), player, ItemStack.EMPTY);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(original.asItem()) && drops.get(0).getCount() == 1
                    && !drops.get(0).hasTag(), "break yields one fresh vanilla sapling with no growth credit");
        }
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void cancelledAndNonPlacementWritesAreNotConverted(GameTestHelper helper) {
        var level = helper.getLevel();BlockPos pos = helper.absolutePos(new BlockPos(9, 3, 9));
        level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.OAK_SAPLING.defaultBlockState(), 3);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_SAPLING), "plain world writes are not a placement conversion");
        var event = new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, pos),
                Blocks.DIRT.defaultBlockState(), null);
        event.setCanceled(true);CultivatedSaplingPlacement.placed(event);
        helper.assertTrue(level.getBlockState(pos).is(Blocks.OAK_SAPLING), "cancelled event is left to Forge rollback");
        helper.succeed();
    }
    @GameTest(template = "phase0a_empty", timeoutTicks = 100)
    public static void multiPlacementAndWetMangrove(GameTestHelper helper) {
        var level = helper.getLevel();List<BlockSnapshot> snapshots = new ArrayList<>();int i = 0;
        for (Block original : List.of(Blocks.BIRCH_SAPLING, Blocks.CHERRY_SAPLING)) {
            BlockPos pos = helper.absolutePos(new BlockPos(4 + i++ * 4, 3, 4));
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(pos, original.defaultBlockState(), 3);
            snapshots.add(BlockSnapshot.create(level.dimension(), level, pos));
        }
        CultivatedSaplingPlacement.placed(new BlockEvent.EntityMultiPlaceEvent(snapshots, Blocks.DIRT.defaultBlockState(), null));
        for (BlockSnapshot snapshot : snapshots) helper.assertTrue(level.getBlockState(snapshot.getPos()).getBlock()
                instanceof CultivatedOakBlock, "convert every affected sapling in a multi-place event");
        BlockPos wet = helper.absolutePos(new BlockPos(9, 3, 10));
        level.setBlock(wet.below(), Blocks.CLAY.defaultBlockState(), 3);
        level.setBlock(wet, Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), 3);
        CultivatedSaplingPlacement.placed(new BlockEvent.EntityPlaceEvent(BlockSnapshot.create(level.dimension(), level, wet),
                Blocks.CLAY.defaultBlockState(), null));
        var state = level.getBlockState(wet);
        helper.assertTrue(state.getBlock() instanceof CultivatedOakBlock && state.getValue(BlockStateProperties.WATERLOGGED)
                && state.getFluidState().is(Fluids.WATER) && state.canSurvive(level, wet),
                "mangrove keeps source water and vanilla clay support");
        helper.succeed();
    }
}
