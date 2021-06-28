package com.teammoeg.frostedheart.mixin.accessors;

import com.stereowalker.survive.util.data.BlockTemperatureData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockTemperatureData.class)
public interface BlockTemperatureDataAccess {

    @Accessor(remap = false)
    void setTemperatureModifier(float newTemperatureModifier);

    @Accessor(remap = false)
    void setRange(int newRange);

    @Accessor(remap = false)
    void setUsesLitOrActiveProperty(boolean newUsesLitOrActiveProperty);

    @Accessor(remap = false)
    void setUsesLevelProperty(boolean newUsesLevelProperty);
}
