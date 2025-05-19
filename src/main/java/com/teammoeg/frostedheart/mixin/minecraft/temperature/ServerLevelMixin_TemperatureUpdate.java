package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.climate.PhysicalState;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.block.LayeredThinIceBlock;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_TemperatureUpdate {

    @Shadow public abstract boolean isFlat();

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
        final long now = level.getGameTime();
        ChunkPos chunkpos = pChunk.getPos();
        boolean updateTempBlock = (now + (chunkpos.x)+(chunkpos.z)) % FHConfig.SERVER.tempBlockstateUpdateIntervalTicks.get() == 0;
        boolean isRaining = level.isRaining();
        int i = chunkpos.getMinBlockX();
        int j = chunkpos.getMinBlockZ();
        // Process fewer blocks for temperature checks to reduce performance impact
        // Adjust this divisor based on your performance needs
        int temperatureChecks = Math.max(1, pRandomTickSpeed / FHConfig.SERVER.tempRandomTickSpeedDivisor.get());

        // Custom water freezing logic
        level.getProfiler().popPush("water");
        if (pRandomTickSpeed > 0 && updateTempBlock) {
            //for (int l1 = 0; l1 < temperatureChecks; ++l1) {
                BlockPos blockpos1 = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(i, 0, j, 15));
                BlockPos blockpos2 = blockpos1.below();
                Holder<Biome> biomeHolder = CUtils.fastGetBiome(level, blockpos2);
                Biome biome = biomeHolder.value();
                // TODO: for ocean freezing, we need some special handling...
                if (!biomeHolder.is(FHTags.Biomes.WATER_DO_NOT_FREEZE.tag) && level.isAreaLoaded(blockpos2, 1)) // Forge: check area to avoid loading neighbors in unloaded chunks
                    // Check if the block should freeze based on our custom logic
                    frostedheart$freezeWater(level, blockpos2);
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
            //}
        }

        // Add temperature profiler section

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
                        
                        if(!frostedHeart$updateBlockBasedOnTemperature(level, blockpos3, blockstate2))
	                    	//@khjxiaogu: omit randomtick from the original block if temperature modification occurred
	                        if(!frostedHeart$updatePlantBasedOnTemperature(level,blockpos3,blockstate2)) {
		                        if (blockstate2.isRandomlyTicking()) {
		                            blockstate2.randomTick(level, blockpos3, level.random);
		                        }
		
		                        FluidState fluidstate = blockstate2.getFluidState();
		                        if (fluidstate.isRandomlyTicking()) {
		                            fluidstate.randomTick(level, blockpos3, level.random);
		                        }
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
     * Custom version of shouldFreeze that keeps all checks except the light level check.
     *
     * Freezes flowing water too.
     *
     * And it freeze water based on its level, so it turns into various thin ice.
     */
    @Unique
    private boolean frostedheart$freezeWater(ServerLevel level, BlockPos pos) {
        if (pos.getY() >= level.getMinBuildHeight() && pos.getY() < level.getMaxBuildHeight()
                && WorldTemperature.block(level, pos) < WorldTemperature.WATER_FREEZES) {
        	BlockState blockstate=level.getBlockState(pos);
            FluidState fluidstate = blockstate.getFluidState();

            // source
            if (fluidstate.getType() == Fluids.WATER && blockstate.getBlock() instanceof LiquidBlock) {
                // Check if block is at edge of water (from original code)
                boolean isAtEdge = !level.isWaterAt(pos.west()) ||
                        !level.isWaterAt(pos.east()) ||
                        !level.isWaterAt(pos.north()) ||
                        !level.isWaterAt(pos.south());

                if (isAtEdge) {
                    // TODO: this is not good because the below new formed water will
                    // be covered by the above thin ice, which makes the below one
                    // no longer the motion blocking height map
                    // check if the block below is a falling water
//                    BlockPos below = pos.below();
//                    BlockState belowState = level.getBlockState(below);
//                    FluidState belowFluidState = level.getFluidState(below);
//                    // if so, replace it with a source water
//                    if (belowFluidState.getType() == Fluids.FLOWING_WATER && belowState.getBlock() instanceof LiquidBlock) {
//                        if (fluidstate.hasProperty(FlowingFluid.FALLING) && belowFluidState.getValue(FlowingFluid.FALLING)) {
//                            level.setBlockAndUpdate(pos.below(), Blocks.WATER.defaultBlockState());
//                        }
//                    }

                    level.setBlockAndUpdate(pos, FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState());
                    return true;
                }
            }

            // flowing
            else if (fluidstate.getType() == Fluids.FLOWING_WATER && blockstate.getBlock() instanceof LiquidBlock) {
                // Check if block is at edge of water (from original code)
                boolean isAtEdge = !level.isWaterAt(pos.west()) ||
                        !level.isWaterAt(pos.east()) ||
                        !level.isWaterAt(pos.north()) ||
                        !level.isWaterAt(pos.south());

                if (isAtEdge) {
                    // TODO: should we do Icicles?
                    if (fluidstate.hasProperty(FlowingFluid.FALLING) && fluidstate.getValue(FlowingFluid.FALLING)) {
                        level.setBlockAndUpdate(pos, FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState());
                    }

                    if (fluidstate.hasProperty(FlowingFluid.LEVEL)) {
                        // range 1-8
                        int flowingLevel = fluidstate.getValue(FlowingFluid.LEVEL);
                        if (flowingLevel == 8) {
                            level.setBlockAndUpdate(pos, FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState());
                        } else {
                            level.setBlockAndUpdate(pos, FHBlocks.LAYERED_THIN_ICE.get().defaultBlockState()
                                    .setValue(LayeredThinIceBlock.LAYERS, flowingLevel));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    //Plants should go separately
    @Unique
    private boolean frostedHeart$updatePlantBasedOnTemperature(ServerLevel level, BlockPos pos, BlockState currentState) {
        var selfData = PlantTempData.getPlantData(currentState.getBlock());
        if (selfData == null) {
            return false;
        }
        // plant state transition
        float t = WorldTemperature.block(level, pos);
        var selfStatus = WorldTemperature.checkPlantStatus(level, pos, selfData, t);
        if (selfStatus.willDie()) {
            var dead = selfData.dead();
            var belowBlockState = level.getBlockState(pos.below());
            if (dead == Blocks.DEAD_BUSH && !belowBlockState.isAir() && !belowBlockState.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON)) {
                level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
            }
            level.setBlockAndUpdate(pos, dead.defaultBlockState());
            return true;
        }
        return false;
        
    }
    @Unique
    private boolean frostedHeart$updateBlockBasedOnTemperature(ServerLevel level, BlockPos pos, BlockState currentState) {

        Block block = currentState.getBlock();
        Holder<Biome> biome=CUtils.fastGetBiome(level, pos);

        StateTransitionData std = StateTransitionData.getData(block);
        //if data file states that it should not transit
        if (std == null||!std.willTransit()||level.getRandom().nextInt(std.heatCapacity()) != 0) {
            return false;
        }
        // TODO: For ocean melting we need special handling...
        else if (biome.is(FHTags.Biomes.ICE_DO_NOT_SMELT.tag) && currentState.is(BlockTags.ICE)) {
            return false;
        }
        // general state transition

        // To save performance, we only focus on blocks that player cares more about, otherwise we reduce translation rate
        boolean shouldDoAdjust = ChunkHeatData.hasActiveAdjust(level, pos)||level.random.nextInt(FHConfig.SERVER.ambientBlockStateUpdateDivisor.get())==0;
        float t = WorldTemperature.block(level, pos);
        // Determine the target state based on temperature thresholds
        // We check transitions in order of priority (solid->gas, gas->solid, etc.)
        PhysicalState targetState = std.state(); // Default to current state
        final PhysicalState sourceState = std.state(); 
        Block targetBlock = block; // Default to current block
        switch(sourceState) {
        case SOLID:
        	if (!shouldDoAdjust) {
                return false;
            }else if(t >= std.evaporateTemp() && std.gas() != null) {
            	targetState = PhysicalState.GAS;
                targetBlock = std.gas();
            }else if(t >= std.meltTemp() && std.liquid() != null) {
            	targetState = PhysicalState.LIQUID;
                targetBlock = std.liquid();
            }break;
        case LIQUID:
        	if(t <= std.freezeTemp() && std.solid() != null) {
        		targetState = PhysicalState.SOLID;
                targetBlock = std.solid();
        	}else if(!shouldDoAdjust) {
        		return false;
        	}else if(t >= std.evaporateTemp() && std.gas() != null){
        		targetState = PhysicalState.GAS;
                targetBlock = std.gas();
        	}break;
        case GAS:
        	if (t <= std.freezeTemp() && std.solid() != null) {
                targetState = PhysicalState.SOLID;
                targetBlock = std.solid();
            }else if (t <= std.condenseTemp() && std.liquid() != null) {
                targetState = PhysicalState.LIQUID;
                targetBlock = std.liquid();
            }break;
        }
        // Skip update if target state is the same as current state
        if (targetState==sourceState||targetBlock.defaultBlockState()==currentState) {
            return false;
        }
        // Create effects before changing the block
        frostedHeart$addTransitionEffects(level, pos, sourceState, targetState, block, targetBlock);

        // Update the block state
        level.setBlockAndUpdate(pos, targetBlock.defaultBlockState());
        return true;

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
    private void frostedHeart$addTransitionEffects(ServerLevel level, BlockPos pos, PhysicalState oldState, PhysicalState newState, Block oldBlock, Block newBlock) {
        // Create server-side particle effects that will be sent to clients
        // Use the transition type to determine appropriate particles
    	switch(oldState.translate(newState)) {
    	case MELTING:
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
            break;
    	case SUBLIMATION:
    	case EVAPORATION:
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
            break;
    	case FREEZING:
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
            break;
    	case CONDENSATION:
    	case DEPOSITION:
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
    		break;
    	default:
    		break;
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
