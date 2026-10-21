package com.teammoeg.frostedheart.mixin.minecraft.water;

import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Glass bottles use the water tag instead of BucketPickup. */
@Mixin(BottleItem.class)
public abstract class GlassBottleItemMixin_HotSpring {
    @Redirect(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean fh$leaveSpringInWorld(FluidState state, TagKey<Fluid> tag) {
        return !state.getType().isSame(FHFluids.HOT_SPRING.get()) && state.is(tag);
    }
}
