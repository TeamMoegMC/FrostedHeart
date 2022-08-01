package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin extends DirectionalBlock {
    protected PistonBlockMixin(Properties builder) {
        super(builder);
    }

    @ModifyConstant(method = "doMove", constant = @Constant(intValue = 68, ordinal = 0))
    public int getFlag(int in) {
        return in | 16;
    }
}
