package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.plant.EnumHempGrowth;
import blusunrize.immersiveengineering.common.blocks.plant.HempBlock;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.block.BlockState;
import net.minecraft.block.BushBlock;
import net.minecraft.block.IGrowable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.eventbus.api.Event;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

import static blusunrize.immersiveengineering.common.blocks.plant.HempBlock.GROWTH;
import static blusunrize.immersiveengineering.common.blocks.plant.HempBlock.getMaxGrowth;

@Mixin(HempBlock.class)
public class HempBlockMixin {

    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void inject$tick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        ChunkData data = ChunkData.get(world, pos);
        float temp = data.getTemperatureAtBlock(pos);
        if (temp < WorldClimate.HEMP_GROW_TEMPERATURE) {
            ci.cancel();
        }
    }
}
