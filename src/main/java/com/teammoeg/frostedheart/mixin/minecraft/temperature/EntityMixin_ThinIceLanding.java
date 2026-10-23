package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.block.LayeredThinIceBlock;
import com.teammoeg.frostedheart.content.climate.block.ThinIceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Low ice must receive impact even when creative mode suppresses LivingFallEvent. */
@Mixin(Entity.class)
public abstract class EntityMixin_ThinIceLanding {
    @Redirect(method = "checkFallDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;fallOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;F)V"))
    private void fh$thinIceLanding(Block block, Level level, BlockState state, BlockPos legacyPos, Entity entity, float distance) {
        if (level instanceof ServerLevel && ThinIceBlock.canCrack(entity)) {
            var pos = entity.getOnPos();
            var surface = level.getBlockState(pos);
            // Correct the landing target, so stacked ice does not receive a second impact below it.
            if (surface.getBlock() instanceof LayeredThinIceBlock ice
                    && surface.getValue(LayeredThinIceBlock.LAYERS) == 1 && legacyPos.getY() < pos.getY()) {
                ice.fallOn(level, surface, pos, entity, distance);
                return;
            }
        }
        block.fallOn(level, state, legacyPos, entity, distance);
    }
}
