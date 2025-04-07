package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.PlantTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DeadBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin_TemperatureUpdate {

    @Unique
    private static final int TEMPERATUE_RANDOM_TICK_SPEED_DIVISOR = 2;

    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 1,  // This targets the profiler.popPush("tickBlocks") call
                    shift = At.Shift.BEFORE
            )
    )
    private void injectTemperatureLogic(LevelChunk pChunk, int pRandomTickSpeed, CallbackInfo ci) {
        ServerLevel level = (ServerLevel)(Object)this;
        ChunkPos chunkpos = pChunk.getPos();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();

        // Add temperature profiler section
        level.getProfiler().popPush("temperature");

        // Only process if random tick speed is positive
        if (pRandomTickSpeed > 0) {
            LevelChunkSection[] sections = pChunk.getSections();

            for (int l = 0; l < sections.length; ++l) {
                LevelChunkSection levelchunksection = sections[l];
                if (levelchunksection.isRandomlyTicking()) {
                    int j1 = pChunk.getSectionYFromSectionIndex(l);
                    int k1 = SectionPos.sectionToBlockCoord(j1);

                    // Process fewer blocks for temperature checks to reduce performance impact
                    // Adjust this divisor based on your performance needs
                    int temperatureChecks = Math.max(1, pRandomTickSpeed / TEMPERATUE_RANDOM_TICK_SPEED_DIVISOR);

                    for (int l1 = 0; l1 < temperatureChecks; ++l1) {
                        // Get a random block position in this chunk section
                        BlockPos blockpos = level.getBlockRandomPos(i, k1, j, 15);

                        // Get the block state at this position
                        BlockState blockstate = levelchunksection.getBlockState(
                                blockpos.getX() - i,
                                blockpos.getY() - k1,
                                blockpos.getZ() - j
                        );

                        // Check temperature and update block if necessary
                        frostedHeart$updateBlockBasedOnTemperature(level, blockpos, blockstate);
                    }
                }
            }
        }

        // The tickBlocks section will be handled by the original method after this injection
    }

    @Unique
    private void frostedHeart$updateBlockBasedOnTemperature(ServerLevel level, BlockPos pos, BlockState currentState) {
        // Get temperature at this block position using your existing system
        float temperature = WorldTemperature.block(level, pos);
        Block block = currentState.getBlock();

        PlantTempData data = PlantTempData.getPlantData(block);
        if (data == null) {
            return;
        }
        WorldTemperature.PlantStatus status = WorldTemperature.checkPlantStatus(level, pos, data);
        Block dead = data.dead();

        if (status.willDie()) {
            BlockState below = level.getBlockState(pos.below());
            if (dead == Blocks.DEAD_BUSH && !below.isAir() && !below.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON)) {
                level.setBlockAndUpdate(pos.below(), Blocks.COARSE_DIRT.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, dead.defaultBlockState());
        }

        // Handle any additional temperature-based logic
    }
}
