package com.teammoeg.frostedheart.infrastructure.command;

import java.util.HashSet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedheart_hot_spring_locate")
@PrefixGameTestTemplate(false)
public final class HotSpringLocateGameTests {
    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void realSourceAndFlowingFluidReturnAbsoluteCoordinates(GameTestHelper helper) {
        LevelChunkSection section = new LevelChunkSection(helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME));
        ChunkPos chunk = new ChunkPos(-7, 4);
        section.setBlockState(15, 0, 0, Blocks.WATER.defaultBlockState());
        helper.assertTrue(HotSpringLocateCommand.findInSection(section, chunk, -4) == null,
                "ordinary water must not count as a hot spring");
        section.setBlockState(15, 0, 0, FHFluids.HOT_SPRING.get().getSource().defaultFluidState().createLegacyBlock());
        helper.assertTrue(new BlockPos(-97, -64, 64).equals(HotSpringLocateCommand.findInSection(section, chunk, -4)),
                "source fluid at a negative chunk edge returns its real absolute coordinate");
        section.setBlockState(15, 0, 0, Blocks.AIR.defaultBlockState());
        section.setBlockState(0, 15, 15, FHFluids.HOT_SPRING.get().getFlowing(4, false).createLegacyBlock());
        helper.assertTrue(new BlockPos(-112, 319, 79).equals(HotSpringLocateCommand.findInSection(section, chunk, 19)),
                "flowing fluid and high hand-placed springs are searchable without biome or natural-height assumptions");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void loadedSearchSkipsMissingChunksWithinBudget(GameTestHelper helper) {
        CommandSourceStack source = distantSource(helper);
        HotSpringLocateCommand.Search search = new HotSpringLocateCommand.Search(source, 3, false);
        helper.assertTrue(search.chunks.stream().allMatch(pos -> helper.getLevel().getChunkSource().getChunkNow(pos.x, pos.z) == null),
                "fixture begins wholly unloaded");
        helper.assertTrue(!search.tick() && search.nextChunk == HotSpringLocateCommand.CHUNKS_PER_TICK,
                "a search yields after its chunk budget, even when every chunk is unloaded");
        for (int tick = 0; tick < 10 && !search.tick(); tick++) { }
        helper.assertTrue(search.skipped == 49 && search.scanned == 0 && search.nextChunk == 49,
                "all requested chunks are accounted for without claiming they were scanned");
        helper.assertTrue(search.chunks.stream().allMatch(pos -> helper.getLevel().getChunkSource().getChunkNow(pos.x, pos.z) == null),
                "ordinary search never loads/generates missing chunks");
        helper.assertTrue(new HashSet<>(search.chunks).size() == 49
                        && search.chunks.get(0).equals(search.center)
                        && search.chunks.stream().allMatch(pos -> Math.abs(pos.x - search.center.x) <= 3
                                && Math.abs(pos.z - search.center.z) <= 3),
                "bounded rings visit each chunk exactly once and start at the requested center");
        helper.succeed();
    }

    @GameTest(template = "phase0a_empty", batch = "ice_cave", timeoutTicks = 40)
    public static void generateYieldsAndLocateRequiresOperator(GameTestHelper helper) {
        CommandSourceStack source = distantSource(helper).withPosition(new Vec3(-1_234_560, 64, -1_234_560));
        HotSpringLocateCommand.Search search = new HotSpringLocateCommand.Search(source, 0, true);
        try {
            helper.assertTrue(!search.tick() && search.waiting == 1 && search.nextChunk == 0 && search.scanned == 0,
                    "explicit generation schedules one target and yields before scanning or scheduling another");
            helper.assertTrue(helper.getLevel().getChunkSource().getChunkNow(search.center.x, search.center.z) == null,
                    "issuing a generation request does not synchronously generate its target");
        } finally {
            search.releaseTicket();
        }
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(Commands.literal("fh").then(HotSpringLocateCommand.command()));
        try {
            dispatcher.execute("fh locate hot_spring", source.withPermission(0));
            throw new AssertionError("non-operators must not start a search");
        } catch (CommandSyntaxException expected) {
            // Matches the repository's permission level 2 debug-command convention.
        }
        helper.assertTrue(dispatcher.getRoot().getChild("locate") == null,
                "the standalone vanilla /locate root is never registered by this command");
        helper.succeed();
    }

    private static CommandSourceStack distantSource(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel())
                .withPosition(new Vec3(-1_234_560, 64, 1_234_560)).withSuppressedOutput();
    }
}
