package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin_TemperatureUpdate {

    @Unique
    private static final int TEMPERATUE_RANDOM_TICK_SPEED_DIVISOR = 2;
    @Unique
    private static final int WATER_FREEZE_CHANCE_INVERSE = 10;

    /**
     * Adds our custom temperature section before iceandsnow.
     * Implement the tickBlocks section too.
     */
    @Inject(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    ordinal = 0, // This targets the profiler.popPush("iceandsnow") call
                    shift = At.Shift.BEFORE
            ),
            cancellable = true)
    private void addTemperatureSection(LevelChunk pChunk, int pRandomTickSpeed, CallbackInfo ci) {

        ServerLevel level = (ServerLevel)(Object)this;
        ChunkPos chunkpos = pChunk.getPos();
        boolean isRaining = level.isRaining();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        // Process fewer blocks for temperature checks to reduce performance impact
        // Adjust this divisor based on your performance needs
        int temperatureChecks = Math.max(1, pRandomTickSpeed / TEMPERATUE_RANDOM_TICK_SPEED_DIVISOR);

        // Custom water freezing logic
        level.getProfiler().popPush("water");
        for (int l1 = 0; l1 < temperatureChecks; ++l1) {
            if (level.random.nextInt(WATER_FREEZE_CHANCE_INVERSE) == 0) {
                BlockPos blockpos1 = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(i, 0, j, 15));
                BlockPos blockpos2 = blockpos1.below();
                Biome biome = level.getBiome(blockpos1).value();
                if (level.isAreaLoaded(blockpos2, 1)) // Forge: check area to avoid loading neighbors in unloaded chunks
                    // Check if the block should freeze based on our custom logic
                    if (frostedHeart$shouldFreezeCustom(level, blockpos2)) {
                        level.setBlockAndUpdate(blockpos2, FHBlocks.THIN_ICE.get().defaultBlockState());
                    }

                if (isRaining) {
                    int i1 = level.getGameRules().getInt(GameRules.RULE_SNOW_ACCUMULATION_HEIGHT);
                    if (i1 > 0 && frostedHeart$shouldSnowCustom(level, blockpos1)) {
                        BlockState blockstate = level.getBlockState(blockpos1);
                        if (blockstate.is(Blocks.SNOW)) {
                            int k = blockstate.getValue(SnowLayerBlock.LAYERS);
                            if (k < Math.min(i1, 8)) {
                                BlockState blockstate1 = blockstate.setValue(SnowLayerBlock.LAYERS, Integer.valueOf(k + 1));
                                Block.pushEntitiesUp(blockstate, blockstate1, level, blockpos1);
                                level.setBlockAndUpdate(blockpos1, blockstate1);
                            }
                        } else {
                            level.setBlockAndUpdate(blockpos1, Blocks.SNOW.defaultBlockState());
                        }
                    }

                    Biome.Precipitation biome$precipitation = biome.getPrecipitationAt(blockpos2);
                    if (biome$precipitation != Biome.Precipitation.NONE) {
                        BlockState blockstate3 = level.getBlockState(blockpos2);
                        blockstate3.getBlock().handlePrecipitation(blockstate3, level, blockpos2, biome$precipitation);
                    }
                }
            }
        }


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

        // Continue with tickBlocks section
        level.getProfiler().popPush("tickBlocks");

        // Now manually implement the tickBlocks section from the original method
        if (pRandomTickSpeed > 0) {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for(int l = 0; l < alevelchunksection.length; ++l) {
                LevelChunkSection levelchunksection = alevelchunksection[l];
                if (levelchunksection.isRandomlyTicking()) {
                    int j1 = pChunk.getSectionYFromSectionIndex(l);
                    int k1 = SectionPos.sectionToBlockCoord(j1);

                    for(int l1 = 0; l1 < pRandomTickSpeed; ++l1) {
                        BlockPos blockpos3 = level.getBlockRandomPos(i, k1, j, 15);
                        level.getProfiler().push("randomTick");
                        BlockState blockstate2 = levelchunksection.getBlockState(blockpos3.getX() - i, blockpos3.getY() - k1, blockpos3.getZ() - j);
                        if (blockstate2.isRandomlyTicking()) {
                            blockstate2.randomTick(level, blockpos3, level.random);
                        }

                        FluidState fluidstate = blockstate2.getFluidState();
                        if (fluidstate.isRandomlyTicking()) {
                            fluidstate.randomTick(level, blockpos3, level.random);
                        }

                        level.getProfiler().pop();
                    }
                }
            }
        }

        // Pop the final profiler section
        level.getProfiler().pop();

        // Cancel the original method
        ci.cancel();
    }

    /**
     * Custom version of shouldSnow that keeps all checks except the light level check
     */
    @Unique
    public boolean frostedHeart$shouldSnowCustom(LevelReader pLevel, BlockPos pPos) {
        if (pPos.getY() >= pLevel.getMinBuildHeight() && pPos.getY() < pLevel.getMaxBuildHeight()
                && WorldTemperature.air(pLevel, pPos) < WorldTemperature.SNOW_REACHES_GROUND) {
            BlockState blockstate = pLevel.getBlockState(pPos);
            if ((blockstate.isAir() || blockstate.is(Blocks.SNOW)) && Blocks.SNOW.defaultBlockState().canSurvive(pLevel, pPos)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Custom version of shouldFreeze that keeps all checks except the light level check
     */
    @Unique
    private boolean frostedHeart$shouldFreezeCustom(ServerLevel level, BlockPos pos) {
        // From original shouldFreeze method, but without warmEnoughToRain check
        // and without the light level check

        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()
                && WorldTemperature.block(level, pos) < WorldTemperature.WATER_FREEZES) {
            BlockState blockstate = level.getBlockState(pos);
            FluidState fluidstate = level.getFluidState(pos);

            if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                // Check if block is at edge of water (from original code)
                boolean isAtEdge = !level.isWaterAt(pos.west()) ||
                        !level.isWaterAt(pos.east()) ||
                        !level.isWaterAt(pos.north()) ||
                        !level.isWaterAt(pos.south());

                if (isAtEdge) {
                    return true;
                }
            }
        }

        return false;
    }

    @Unique
    private void frostedHeart$updateBlockBasedOnTemperature(ServerLevel level, BlockPos pos, BlockState currentState) {
        // Get temperature at this block position using your existing system
        float t = WorldTemperature.block(level, pos);
        Block block = currentState.getBlock();

        // plant related
        PlantTempData data = PlantTempData.getPlantData(block);
        if (data != null) {
            WorldTemperature.PlantStatus status = WorldTemperature.checkPlantStatus(level, pos, data);
            Block dead = data.dead();

            if (status.willDie()) {
                BlockState below = level.getBlockState(pos.below());
                if (dead == Blocks.DEAD_BUSH && !below.isAir() && !below.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON)) {
                    level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
                }
                level.setBlockAndUpdate(pos, dead.defaultBlockState());
            }
        }

        // general state transition
        // we do not handle water freezing logic in seas

        StateTransitionData std = StateTransitionData.getData(block);
        if (std == null) {
            return;
        }
        if (!std.willTransit())
            return;

        // Determine the target state based on temperature thresholds
        // We check transitions in order of priority (solid->gas, gas->solid, etc.)
        String targetState = std.state(); // Default to current state
        Block targetBlock = block; // Default to current block

        // Check for phase transitions in order of precedence
        // 1. Check solid -> gas (sublimation, highest priority if temp is high enough)
        if ("solid".equals(std.state()) && t >= std.evaporateTemp() && std.gas() != null) {
            targetState = "gas";
            targetBlock = std.gas();
        }
        // 2. Check gas -> solid (deposition, highest priority if temp is low enough)
        else if ("gas".equals(std.state()) && t <= std.freezeTemp() && std.solid() != null) {
            targetState = "solid";
            targetBlock = std.solid();
        }
        // 3. Check liquid -> gas (evaporation)
        else if ("liquid".equals(std.state()) && t >= std.evaporateTemp() && std.gas() != null) {
            targetState = "gas";
            targetBlock = std.gas();
        }
        // 4. Check gas -> liquid (condensation)
        else if ("gas".equals(std.state()) && t <= std.condenseTemp() && std.liquid() != null) {
            targetState = "liquid";
            targetBlock = std.liquid();
        }
        // 5. Check solid -> liquid (melting)
        else if ("solid".equals(std.state()) && t >= std.meltTemp() && std.liquid() != null) {
            targetState = "liquid";
            targetBlock = std.liquid();
        }
        // 6. Check liquid -> solid (freezing)
        else if ("liquid".equals(std.state()) && t <= std.freezeTemp() && std.solid() != null) {
            targetState = "solid";
            targetBlock = std.solid();
        }

        // Skip update if target state is the same as current state
        if (targetState.equals(std.state())) {
            return;
        }

        // Apply heat capacity check - only perform transition if random check passes
        if (level.getRandom().nextInt(std.heatCapacity()) == 0) {

            // Create effects before changing the block
            frostedHeart$addTransitionEffects(level, pos, std.state(), targetState, block, targetBlock);

            // Update the block state
            level.setBlockAndUpdate(pos, targetBlock.defaultBlockState());
        }

    }

    /**
     * Adds visual and audio effects when a block changes state
     * @param level The server level
     * @param pos Position of the block
     * @param oldState The original state string ("solid", "liquid", "gas")
     * @param newState The new state string
     * @param oldBlock The original block
     * @param newBlock The new block
     */
    @Unique
    private void frostedHeart$addTransitionEffects(ServerLevel level, BlockPos pos, String oldState, String newState, Block oldBlock, Block newBlock) {
        // Create server-side particle effects that will be sent to clients
        // Use the transition type to determine appropriate particles

        if (oldState.equals("solid") && newState.equals("liquid")) {
            // Melting effect - dripping water particles
            level.sendParticles(
                    ParticleTypes.DRIPPING_WATER,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    8, // particle count
                    0.3D, // spread X
                    0.3D, // spread Y
                    0.3D, // spread Z
                    0.0D  // speed
            );
            // Melting sound
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
        }
        else if (oldState.equals("liquid") && newState.equals("solid")) {
            // Freezing effect - snowflake particles
            level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    10, // particle count
                    0.3D, // spread X
                    0.3D, // spread Y
                    0.3D, // spread Z
                    0.0D  // speed
            );
            // Freezing sound
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.5F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.4F);
        }
        else if (newState.equals("gas")) {
            // Evaporation/sublimation effect - cloud particles
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    12, // particle count
                    0.25D, // spread X
                    0.25D, // spread Y
                    0.25D, // spread Z
                    0.05D  // speed - slightly rising
            );
            // Steam hissing sound
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
        }
        else if (oldState.equals("gas") && (newState.equals("liquid") || newState.equals("solid"))) {
            // Condensation/deposition effect - dripping particles
            level.sendParticles(
                    ParticleTypes.DRIPPING_WATER,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.8D,
                    pos.getZ() + 0.5D,
                    15, // particle count
                    0.4D, // spread X
                    0.1D, // spread Y
                    0.4D, // spread Z
                    0.0D  // speed
            );
            // Condensation sound - light rain-like sound
            level.playSound(null, pos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.AMBIENT, 0.5F, 1.0F);
        }

        // You could also add custom particles for specific block transitions
        // For example, if water is turning to ice:
        if (oldBlock == Blocks.WATER && newBlock == Blocks.ICE) {
            // Special ice formation particles
            level.sendParticles(
                    ParticleTypes.ITEM_SNOWBALL,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    5,
                    0.2D,
                    0.2D,
                    0.2D,
                    0.0D
            );
        }
    }

}
