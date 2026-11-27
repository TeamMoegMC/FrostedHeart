package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.knowledge.furnace.ControlledFurnaceBlockEntity;
import java.nio.file.Files;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("frostedresearch_m5_restart")
@PrefixGameTestTemplate(false)
public final class ControlledFurnaceRestartGameTests {
    @GameTest(batch="m5_process_restart", template="empty", timeoutTicks=80)
    public static void restoresAndSettlesSpecimensAfterProcessRestart(GameTestHelper h) throws Exception {
        var server = h.getLevel().getServer();
        var world = server.overworld();
        var marker = server.getWorldPath(LevelResource.ROOT).resolve("m5-restart.nbt");
        var pos = new BlockPos(600, 80, 600);
        world.getChunkAt(pos);
        if (System.getProperty("frostedresearch.m5RestartPhase", "write").equals("write")) {
            world.setBlockAndUpdate(pos, FRContents.Blocks.CONTROLLED_FURNACE.get().defaultBlockState());
            var furnace = (ControlledFurnaceBlockEntity) world.getBlockEntity(pos);
            ControlledFurnaceGameTests.fill(furnace, 2, 2);
            furnace.start(1107, 21);
            ControlledFurnaceGameTests.ticks(furnace, 100);
            var data = new CompoundTag();
            data.putInt("seconds", furnace.timerSeconds());
            NbtIo.writeCompressed(data, marker.toFile());
        } else {
            h.assertTrue(Files.isRegularFile(marker), "requires previous write process");
            var data = NbtIo.readCompressed(marker.toFile());
            var furnace = (ControlledFurnaceBlockEntity) world.getBlockEntity(pos);
            h.assertTrue(furnace != null && furnace.running() && furnace.temperature() == 1107 && furnace.timerSeconds() > 0
                && furnace.timerSeconds() <= data.getInt("seconds"), "new JVM restores the active run and remaining timer");
            h.assertTrue(furnace.inventory().extractItem(0, 1, false).isEmpty(), "restored samples are still locked");
            ControlledFurnaceGameTests.ticks(furnace, furnace.timerSeconds() * 20);
            for (int slot = 0; slot < 2; slot++)
                h.assertTrue(furnace.inventory().getStackInSlot(slot).is(ControlledFurnaceGameTests.item("frostedheart:refractory_brick")),
                    "both saved specimens resolve in place");
            h.assertTrue(!furnace.running() && furnace.inventory().getStackInSlot(ControlledFurnaceBlockEntity.FUEL_SLOT).getCount() == 1,
                "restoring consumes no additional fuel");
        }
        h.succeed();
    }
}
