package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFluid.class)
public class LavaFluidMixin {

    /**
     * Injects at the head of randomTick to check if lava source should be converted to stone based on temperature
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void onRandomTick(Level level, BlockPos pos, FluidState state, RandomSource random, CallbackInfo ci) {
        // Lava only freezes above -55. Below that, it is constantly heated.
        if (pos.getY() > WorldTemperature.LAVA_INTERFACE_LEVEL) {
            // Lava keeps itself for longer time
            if (random.nextInt(10) == 0) {
                // Check if the fluid state is a source block
                if (state.isSource()) {
                    // Get the temperature at this position
                    if (WorldTemperature.block(level, pos) < WorldTemperature.LAVA_FREEZES) {
                        // Convert to stone
                        level.setBlockAndUpdate(pos, Blocks.BASALT.defaultBlockState());
                        ci.cancel(); // Cancel the original method since we've handled this tick
                    }
                }
            }
        }
    }
}
