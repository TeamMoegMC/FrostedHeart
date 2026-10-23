package com.teammoeg.frostedheart.content.climate.block;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import blusunrize.immersiveengineering.common.util.Utils;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.foundation.utility.BlockFace;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder("frostedheart_hot_spring")
@PrefixGameTestTemplate(false)
public class HotSpringGameTests {
    @GameTest(template = "phase0a_empty", batch = "hot_spring", timeoutTicks = 100)
    public static void adjacentSourcesDoNotReplicateAndFlowRecedes(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(2, 3, 2));
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(center.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(center.offset(x, 0, z),
                        z == 0 && Math.abs(x) <= 1 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
            }
        }
        var spring = FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock();
        level.setBlockAndUpdate(center.west(), spring);
        level.setBlockAndUpdate(center.east(), spring);
        helper.runAfterDelay(20, () -> {
            var flow = level.getFluidState(center);
            helper.assertTrue(flow.getType().isSame(FHFluids.HOT_SPRING.get()) && !flow.isSource(),
                    "two adjacent sources only create flowing spring between them");
            var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "spring-flow-test"));
            player.setPos(center.getX() + 0.5D, center.getY(), center.getZ() + 0.5D);
            player.baseTick();
            contactDetails(player, center);
            helper.assertTrue(player.getFluidTypeHeight(FHFluids.HOT_SPRING.get().getFluidType()) > 0.0D,
                    "flowing spring has the same thermal contact type as its source");
            level.setBlockAndUpdate(center.west(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(center.east(), Blocks.AIR.defaultBlockState());
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(level.getFluidState(center).isEmpty(),
                    "flow recedes after both original sources are removed");
            helper.succeed();
        });
    }

    @GameTest(template = "phase0a_empty", batch = "hot_spring", timeoutTicks = 50)
    public static void sourceFlowsButCannotBeCollected(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 3, 1));
        BlockPos below = source.below();
        level.setBlockAndUpdate(below, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(source.east(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(source, FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock());
        var state = level.getBlockState(source);
        helper.assertTrue(level.getFluidState(source).isSource(), "worldgen places a spring source");
        var springType = FHFluids.HOT_SPRING.get().getFluidType();
        helper.assertTrue(springType == FHFluids.HOT_SPRING.get().getFluidType()
                        && springType == FHFluids.HOT_SPRING.get().getSource().getFluidType()
                        && springType == level.getFluidState(source).getFluidType(),
                "source, flowing, and world state share one stable FluidType identity");
        helper.assertTrue(ForgeRegistries.FLUID_TYPES.get().getKey(springType) != null,
                "spring FluidType is registered for Forge entity contact");
        helper.assertTrue(FHFluids.HOT_SPRING.get().getSource().getBucket() == Items.AIR
                        && FHFluids.HOT_SPRING.get().getBucket() == Items.AIR,
                "both registered fluid variants expose AIR instead of an unregistered bucket supplier");
        helper.assertTrue(level.getFluidState(source).is(FluidTags.WATER), "spring retains water contact behavior");
        helper.assertTrue(level.getFluidState(source).is(FHFluids.HOT_SPRING_TAG), "spring has its own contact tag");
        helper.assertTrue(!level.getFluidState(source).canConvertToSource(level, source), "spring cannot make sources");
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "spring-source-test"));
        player.setPos(source.getX() + 0.5D, source.getY(), source.getZ() + 0.5D);
        player.updateFluidHeightAndDoFluidPushing();
        contactDetails(player, source);
        helper.assertTrue(player.getFluidTypeHeight(FHFluids.HOT_SPRING.get().getFluidType()) > 0.4D,
                "Forge tracks shallow spring contact on the player");
        helper.assertTrue(player.getFluidHeight(FluidTags.WATER) == 0.0D,
                "spring contact is separate from ordinary water temperature");
        helper.assertTrue(((HotSpringBlock) state.getBlock()).pickupBlock(level, source, state).isEmpty(),
                "bucket pickup yields no item");
        helper.assertTrue(Utils.drainFluidBlock(level, source, FluidAction.SIMULATE).isEmpty(),
                "IE pump simulation cannot extract the spring");
        helper.assertTrue(Utils.drainFluidBlock(level, source, FluidAction.EXECUTE).isEmpty(),
                "IE pump execution cannot extract the spring");

        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(source.west(), Direction.EAST));
        pipe.manageSource(level);
        var handler = pipe.provideHandler().orElseThrow(IllegalStateException::new);
        helper.assertTrue(handler.drain(1000, FluidAction.SIMULATE).isEmpty(),
                "Create open pipe simulation cannot extract the spring");
        helper.assertTrue(handler.drain(1000, FluidAction.EXECUTE).isEmpty(),
                "Create open pipe execution cannot extract the spring");
        helper.assertTrue(level.getFluidState(source).isSource(), "collection leaves source in place");

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(level.getFluidState(source.east()).getType().isSame(FHFluids.HOT_SPRING.get()),
                    "spring flows into open space");
            helper.assertTrue(!level.getFluidState(source.east()).isSource(),
                    "flowing spring does not become a source");
            helper.succeed();
        });
    }

    public static void contactDetails(Player player, BlockPos pos) {
        var worldFluid = player.level().getFluidState(pos);
        var springType = FHFluids.HOT_SPRING.get().getFluidType();
        FHMain.LOGGER.info("Spring contact pos={} player={} bbox={} unloaded={} fluid={} sourceType={} flowingType={} worldType={} stable={} worldHeight={} springHeight={} worldTypeHeight={} waterHeight={} inWater={}",
                pos, player.position(), player.getBoundingBox(), player.touchingUnloadedChunk(),
                ForgeRegistries.FLUIDS.getKey(worldFluid.getType()),
                ForgeRegistries.FLUID_TYPES.get().getKey(FHFluids.HOT_SPRING.get().getSource().getFluidType()),
                ForgeRegistries.FLUID_TYPES.get().getKey(springType),
                ForgeRegistries.FLUID_TYPES.get().getKey(worldFluid.getFluidType()),
                springType == FHFluids.HOT_SPRING.get().getFluidType(),
                worldFluid.getHeight(player.level(), pos), player.getFluidTypeHeight(springType),
                player.getFluidTypeHeight(worldFluid.getFluidType()), player.getFluidHeight(FluidTags.WATER),
                player.isInWater());
    }
}
